package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassParentsVisitor
import org.jetbrains.ide.diff.builder.signatures.getJavaPackageName
import org.jetbrains.ide.diff.builder.signatures.toSignature
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Builder of [SinceApiData] by APIs difference of two IDEs.
 */
class SinceApiBuilder(private val interestingPackages: List<String>, private val jdkPath: JdkPath) {

  companion object {
    private val LOG = LoggerFactory.getLogger(SinceApiBuilder::class.java)

    /**
     * IDs of plugins to be ignored from processing. Their APIs are not relevant to IDE.
     */
    private val IGNORED_PLUGIN_IDS = setOf("org.jetbrains.kotlin", "org.jetbrains.android")
  }

  fun build(oldIde: Ide, newIde: Ide): SinceApiData {
    val apiData = ApiData()
    return JdkResolverCreator.createJdkResolver(jdkPath.jdkPath.toFile()).use { jdkResolver ->
      appendIdeCoreData(oldIde, newIde, apiData, jdkResolver)
      appendBundledPluginsData(oldIde, newIde, apiData, jdkResolver)
      SinceApiData(newIde.version, mapOf(newIde.version to apiData))
    }
  }

  private fun appendIdeCoreData(oldIde: Ide, newIde: Ide, apiData: ApiData, jdkResolver: Resolver) {
    IdeResolverCreator.createIdeResolver(oldIde).use { oldIdeResolver ->
      IdeResolverCreator.createIdeResolver(newIde).use { newIdeResolver ->
        appendData(oldIdeResolver, newIdeResolver, apiData, jdkResolver)
      }
    }
  }

  private fun appendBundledPluginsData(oldIde: Ide, newIde: Ide, apiData: ApiData, jdkResolver: Resolver) {
    val oldPluginClassLocations = readBundledPluginsClassesLocations(oldIde)
    Closeable { oldPluginClassLocations.closeAll() }.use {
      val newPluginClassLocations = readBundledPluginsClassesLocations(newIde)
      Closeable { newPluginClassLocations.closeAll() }.use {
        val oldAllPluginsResolver = oldPluginClassLocations.map { it.getPluginClassesResolver() }.let { UnionResolver.create(it) }
        val newAllPluginsResolver = newPluginClassLocations.map { it.getPluginClassesResolver() }.let { UnionResolver.create(it) }
        appendData(oldAllPluginsResolver, newAllPluginsResolver, apiData, jdkResolver)
      }
    }
  }

  private fun appendData(oldResolver: Resolver, newResolver: Resolver, apiData: ApiData, jdkResolver: Resolver) {
    val completeNewResolver = CacheResolver(UnionResolver.create(listOf(newResolver, jdkResolver)))
    newResolver.processAllClasses { newClass ->
      if (newClass.isIgnored()) {
        return@processAllClasses true
      }

      val oldClass = oldResolver.safeFindClass(newClass.name)

      if (oldClass == null) {
        /**
         * Register the new class and all its stuff.
         *
         * Inner and nested classes will be processed separately
         * in this `processAllClasses`, so here is no need
         * to handle them.
         */
        apiData.addSignature(newClass.createClassLocation().toSignature())
        for (methodNode in newClass.getMethods().orEmpty().filterNot { it.isIgnored() || isMethodOverriding(it, newClass, completeNewResolver) }) {
          apiData.addSignature(createMethodLocation(newClass, methodNode).toSignature())
        }
        for (fieldNode in newClass.getFields().orEmpty().filterNot { it.isIgnored() }) {
          apiData.addSignature(createFieldLocation(newClass, fieldNode).toSignature())
        }
        return@processAllClasses true
      }

      compareClasses(oldClass, newClass, apiData, completeNewResolver)
      true
    }
  }

  private fun Resolver.safeFindClass(className: String): ClassNode? {
    return try {
      findClass(className)
    } catch (e: Exception) {
      LOG.warn("Class file $className couldn't be read from $this", e)
      return null
    }
  }

  private fun isMethodOverriding(methodNode: MethodNode, classNode: ClassNode, resolver: Resolver): Boolean {
    if (methodNode.isConstructor()
        || methodNode.isClassInitializer()
        || methodNode.isStatic()
        || methodNode.isPrivate()
        || methodNode.isDefaultAccess()
    ) {
      return false
    }

    val parentsVisitor = ClassParentsVisitor(true) { _, parentClassName ->
      resolver.safeFindClass(parentClassName)
    }

    val isOverriding = AtomicBoolean()
    parentsVisitor.visitClass(classNode, false, onEnter = { parentClass ->
      val hasSameMethod = parentClass.getMethods().orEmpty().any {
        it.name == methodNode.name
            && it.desc == methodNode.desc
            && !it.isStatic()
            && !it.isPrivate()
            && !it.isDefaultAccess()
      }
      if (hasSameMethod) {
        isOverriding.set(true)
      }
      !isOverriding.get()
    }, onExit = {})

    return isOverriding.get()
  }

  /**
   * Specifies which classes should be put into the plugin's class files resolver.
   * Currently, we select all the classes from:
   * 1) for `.jar`-red plugin, all classes contained in the `.jar`
   * 2) for directory-based plugins, all classes from the `/lib/` directory and
   * from the `/classes` directory, if any
   * 3) JPS-used classes, such as `Kotlin/lib/jps`.
   */
  private val pluginClassesLocationsKeys = IdePluginClassesFinder.MAIN_CLASSES_KEYS + listOf(CompileServerExtensionKey)

  /**
   * Merges all the classes by different locations (`/lib/`, `/classes/`, etc) into
   * one resolver.
   */
  private fun IdePluginClassesLocations.getPluginClassesResolver(): Resolver =
      pluginClassesLocationsKeys.mapNotNull { getResolver(it) }.let { UnionResolver.create(it) }

  /**
   * Finds class files of all plugins bundled into the [ide].
   *
   * The results must be [closed] [IdePluginClassesLocations.close]
   * when no more required to free up possibly occupied disk space:
   * plugins might have been extracted to a temporary directory.
   */
  private fun readBundledPluginsClassesLocations(ide: Ide): List<IdePluginClassesLocations> =
      ide.bundledPlugins.mapNotNull { safeFindPluginClasses(ide, it) }

  private fun safeFindPluginClasses(ide: Ide, idePlugin: IdePlugin): IdePluginClassesLocations? = try {
    if (idePlugin.pluginId !in IGNORED_PLUGIN_IDS) {
      LOG.debug("Reading class files of a plugin $idePlugin bundled into $ide")
      IdePluginClassesFinder.findPluginClasses(idePlugin, pluginClassesLocationsKeys)
    } else {
      null
    }
  } catch (e: Exception) {
    LOG.info("Unable to read class files of a plugin $idePlugin bundled to $ide: ${e.message}")
    null
  }

  private fun compareClasses(
      oldClass: ClassNode,
      newClass: ClassNode,
      apiDiff: ApiData,
      completeNewResolver: Resolver
  ) {
    for (newMethod in newClass.getMethods().orEmpty()) {
      if (newMethod.isIgnored() || isMethodOverriding(newMethod, newClass, completeNewResolver)) {
        continue
      }

      val oldMethod = oldClass.getMethods()?.find { it.name == newMethod.name && it.desc == newMethod.desc }
      if (oldMethod == null) {
        apiDiff.addSignature(createMethodLocation(newClass, newMethod).toSignature())
      }
    }

    for (newField in newClass.getFields().orEmpty()) {
      if (newField.isIgnored()) {
        continue
      }

      val oldField = oldClass.getFields()?.find { it.name == newField.name && it.desc == newField.desc }
      if (oldField == null) {
        apiDiff.addSignature(createFieldLocation(newClass, newField).toSignature())
      }
    }
  }

  private fun String.isSyntheticLikeName() = contains("$$") || substringAfterLast('$', "").toIntOrNull() != null

  private fun String.hasInterestingPackage(): Boolean {
    val packageName = getJavaPackageName(this)
    return interestingPackages.any { p ->
      p.isEmpty() || p == packageName || packageName.startsWith("$p.")
    }
  }

  /**
   * Returns `true` if this class is likely an implementation of something.
   * `org.some.ServiceImpl` -> true
   * `org.some.InterfaceImpl.InnerClass` -> true
   */
  private fun String.hasImplementationLikeName() = endsWith("Impl") || contains("Impl.")

  /**
   * Returns `true` if this package is likely a package containing implementation of some APIs.
   * `org.some.impl.services` -> true
   */
  private fun String.hasImplementationLikePackage(): Boolean {
    val packageName = getJavaPackageName(this)
    return ".impl." in packageName
  }

  private fun ClassNode.isIgnored() = isPrivate() || isDefaultAccess()
      || isSynthetic()
      || name.isSyntheticLikeName()
      || !name.hasInterestingPackage()
      || name.hasImplementationLikeName()
      || name.hasImplementationLikePackage()

  private fun MethodNode.isIgnored() = isPrivate() || isDefaultAccess() || isClassInitializer() || isBridgeMethod() || isSynthetic() || name.isSyntheticLikeName()

  private fun FieldNode.isIgnored() = isPrivate() || isDefaultAccess() || isSynthetic() || name.isSyntheticLikeName()

}
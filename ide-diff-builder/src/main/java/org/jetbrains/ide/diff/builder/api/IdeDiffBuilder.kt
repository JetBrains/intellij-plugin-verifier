package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.plugin.structure.classes.resolvers.CacheResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
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
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Builder of [ApiReport] by APIs difference of two IDEs.
 */
class IdeDiffBuilder(private val interestingPackages: List<String>, private val jdkPath: JdkPath) {

  companion object {
    private val LOG = LoggerFactory.getLogger(IdeDiffBuilder::class.java)

    /**
     * IDs of plugins to be ignored from processing. Their APIs are not relevant to IDE.
     */
    private val IGNORED_PLUGIN_IDS = setOf("org.jetbrains.kotlin", "org.jetbrains.android")
  }

  fun buildIdeDiff(oldIdePath: Path, newIdePath: Path): ApiReport {
    val oldIde = IdeManager.createManager().createIde(oldIdePath.toFile())
    val newIde = IdeManager.createManager().createIde(newIdePath.toFile())
    return buildIdeDiff(oldIde, newIde)
  }

  fun buildIdeDiff(oldIde: Ide, newIde: Ide): ApiReport {
    val introducedData = ApiData()
    val removedData = ApiData()
    return JdkResolverCreator.createJdkResolver(jdkPath.jdkPath.toFile()).use { jdkResolver ->
      appendIdeCoreData(oldIde, newIde, jdkResolver, introducedData, removedData)
      appendBundledPluginsData(oldIde, newIde, jdkResolver, introducedData, removedData)
      val apiEventToData = mapOf(
          IntroducedIn(newIde.version) to introducedData,
          RemovedIn(newIde.version) to removedData
      )
      ApiReport(newIde.version, apiEventToData)
    }
  }

  private fun appendIdeCoreData(oldIde: Ide, newIde: Ide, jdkResolver: Resolver, introducedData: ApiData, removedData: ApiData) {
    IdeResolverCreator.createIdeResolver(oldIde).use { oldIdeResolver ->
      IdeResolverCreator.createIdeResolver(newIde).use { newIdeResolver ->
        appendData(oldIdeResolver, newIdeResolver, jdkResolver, introducedData, removedData)
      }
    }
  }

  private fun appendBundledPluginsData(oldIde: Ide, newIde: Ide, jdkResolver: Resolver, introducedData: ApiData, removedData: ApiData) {
    val oldPluginClassLocations = readBundledPluginsClassesLocations(oldIde)
    Closeable { oldPluginClassLocations.closeAll() }.use {
      val newPluginClassLocations = readBundledPluginsClassesLocations(newIde)
      Closeable { newPluginClassLocations.closeAll() }.use {
        val oldAllPluginsResolver = oldPluginClassLocations.map { it.getPluginClassesResolver() }.let { UnionResolver.create(it) }
        val newAllPluginsResolver = newPluginClassLocations.map { it.getPluginClassesResolver() }.let { UnionResolver.create(it) }
        appendData(oldAllPluginsResolver, newAllPluginsResolver, jdkResolver, introducedData, removedData)
      }
    }
  }

  private fun appendData(oldResolver: Resolver, newResolver: Resolver, jdkResolver: Resolver, introducedData: ApiData, removedData: ApiData) {
    val completeOldResolver = CacheResolver(UnionResolver.create(listOf(oldResolver, jdkResolver)))
    val completeNewResolver = CacheResolver(UnionResolver.create(listOf(newResolver, jdkResolver)))

    val allClasses: Set<String> = oldResolver.allClasses + newResolver.allClasses
    for (className in allClasses) {
      if (isIgnoredClassName(className)) {
        continue
      }

      val oldClass = completeOldResolver.safeFindClass(className)
      val newClass = completeNewResolver.safeFindClass(className)
      if (oldClass == null && newClass == null) {
        continue
      }

      if (newClass != null && newClass.isAccessible() && !newClass.isIgnored()) {
        findAddedApi(oldClass, newClass, completeOldResolver, completeNewResolver, introducedData)
      }
      if (oldClass != null && oldClass.isAccessible() && !oldClass.isIgnored()) {
        findRemovedApi(oldClass, newClass, completeOldResolver, completeNewResolver, removedData)
      }
    }
  }

  /**
   * Appends all signatures available in [newClass] but not available in [oldClass].
   */
  private fun findAddedApi(
      oldClass: ClassNode?,
      newClass: ClassNode,
      oldResolver: Resolver,
      newResolver: Resolver,
      introducedData: ApiData
  ) {
    if (oldClass == null || !oldClass.isAccessible()) {
      //Don't register methods, fields and inner classes of newly added class, only its own signature.
      val outerClassName = getOuterClassName(newClass.name)
      if (outerClassName != null && !oldResolver.containsClass(outerClassName)) {
        //Outer class is already added => no need to register this inner one.
        return
      }
      introducedData.addSignature(newClass.createClassLocation().toSignature())
      return
    }

    for (newMethod in newClass.getMethods().orEmpty()) {
      if (!newMethod.isAccessible() || newMethod.isIgnored() || isMethodOverriding(newMethod, newClass, newResolver)) {
        continue
      }

      val oldMethod = oldClass.getMethods()?.find {
        it.name == newMethod.name && it.desc == newMethod.desc && it.isAccessible() && !it.isIgnored()
      }
      if (oldMethod == null) {
        introducedData.addSignature(createMethodLocation(newClass, newMethod).toSignature())
      }
    }

    for (newField in newClass.getFields().orEmpty()) {
      if (!newField.isAccessible() || newField.isIgnored()) {
        continue
      }

      val oldField = oldClass.getFields()?.find {
        it.name == newField.name && it.desc == newField.desc && it.isAccessible() && !it.isIgnored()
      }
      if (oldField == null) {
        introducedData.addSignature(createFieldLocation(newClass, newField).toSignature())
      }
    }
  }

  /**
   * Appends all signatures available in [oldClass] but not available in [newClass].
   */
  private fun findRemovedApi(
      oldClass: ClassNode,
      newClass: ClassNode?,
      oldResolver: Resolver,
      newResolver: Resolver,
      removedData: ApiData
  ) {
    if (newClass == null || !newClass.isAccessible()) {
      /**
       * Don't register methods, fields and inner classes of removed class, only its own signature.
       */
      val outerClassName = getOuterClassName(oldClass.name)
      if (outerClassName != null && !newResolver.containsClass(outerClassName)) {
        //Outer class is already registered => no need to register this inner one.
        return
      }
      removedData.addSignature(oldClass.createClassLocation().toSignature())
      return
    }

    for (oldMethod in oldClass.getMethods().orEmpty()) {
      if (!oldMethod.isAccessible() || oldMethod.isIgnored() || isMethodOverriding(oldMethod, oldClass, oldResolver)) {
        continue
      }

      val newMethod = newClass.getMethods()?.find {
        it.name == oldMethod.name && it.desc == oldMethod.desc && it.isAccessible() && !it.isIgnored()
      }
      if (newMethod == null) {
        removedData.addSignature(createMethodLocation(oldClass, oldMethod).toSignature())
      }
    }

    for (oldField in oldClass.getFields().orEmpty()) {
      if (!oldField.isAccessible() || oldField.isIgnored()) {
        continue
      }

      val newField = newClass.getFields()?.find {
        it.name == oldField.name && it.desc == oldField.desc && it.isAccessible() && !it.isIgnored()
      }
      if (newField == null) {
        removedData.addSignature(createFieldLocation(oldClass, oldField).toSignature())
      }
    }
  }


  private fun getOuterClassName(className: String): String? {
    val packageName = className.substringBeforeLast("/")
    val simpleName = className.substringAfterLast("/")
    if ('$' in simpleName) {
      val outerSimpleName = simpleName.substringBeforeLast('$')
      return if (packageName.isEmpty()) outerSimpleName else "$packageName/$outerSimpleName"
    }
    return null
  }

  private fun Resolver.safeFindClass(className: String): ClassNode? {
    return try {
      findClass(className)
    } catch (e: Exception) {
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
   * The results must be closed when no more required to free up possibly occupied disk space:
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

  private fun String.isSyntheticLikeName() = contains("$$") || substringAfterLast('$', "").toIntOrNull() != null

  private fun String.hasInterestingPackage(): Boolean {
    if (interestingPackages.isEmpty()) {
      return true
    }
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

  private fun isIgnoredClassName(className: String): Boolean =
      !className.hasInterestingPackage()
          || className.hasImplementationLikeName()
          || className.hasImplementationLikePackage()
          || className.isSyntheticLikeName()

  private fun ClassNode.isAccessible() = !isPrivate() && !isDefaultAccess()

  private fun ClassNode.isIgnored() = isIgnoredClassName(name) || isSynthetic()

  private fun MethodNode.isAccessible() = !isPrivate() && !isDefaultAccess()

  private fun MethodNode.isIgnored() = isClassInitializer() || isBridgeMethod() || isSynthetic() || name.isSyntheticLikeName()

  private fun FieldNode.isAccessible() = !isPrivate() && !isDefaultAccess()

  private fun FieldNode.isIgnored() = isSynthetic() || name.isSyntheticLikeName()

}
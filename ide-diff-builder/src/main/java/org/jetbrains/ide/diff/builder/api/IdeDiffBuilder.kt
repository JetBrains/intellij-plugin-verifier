package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
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
import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassParentsVisitor
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.getJavaPackageName
import org.jetbrains.ide.diff.builder.signatures.toSignature
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

    private val knownObfuscatedPackages = listOf(
        "a",
        "b",
        "com.intellij.a",
        "com.intellij.b",
        "com.intellij.ide.a",
        "com.intellij.ide.b",
        "com.jetbrains.a",
        "com.jetbrains.b",
        "com.jetbrains.ls"
    )

    fun hasObfuscatedLikePackage(className: String): Boolean {
      val javaName = toFullJavaClassName(className)
      return knownObfuscatedPackages.any { javaName.startsWith("$it.") }
    }
  }

  fun buildIdeDiff(oldIdePath: Path, newIdePath: Path): ApiReport {
    val oldIde = IdeManager.createManager().createIde(oldIdePath.toFile())
    val newIde = IdeManager.createManager().createIde(newIdePath.toFile())
    return buildIdeDiff(oldIde, newIde)
  }

  fun buildIdeDiff(oldIde: Ide, newIde: Ide): ApiReport {
    val introducedData = hashSetOf<ApiSignature>()
    val removedData = hashSetOf<ApiSignature>()
    return JdkResolverCreator.createJdkResolver(Resolver.ReadMode.SIGNATURES, jdkPath.jdkPath.toFile()).use { jdkResolver ->
      IdeResolverCreator.createIdeResolver(Resolver.ReadMode.SIGNATURES, oldIde).use { oldPlatformResolver ->
        IdeResolverCreator.createIdeResolver(Resolver.ReadMode.SIGNATURES, newIde).use { newPlatformResolver ->
          val oldPluginClassLocations = readBundledPluginsClassesLocations(oldIde)
          Closeable { oldPluginClassLocations.closeAll() }.use {
            val newPluginClassLocations = readBundledPluginsClassesLocations(newIde)
            Closeable { newPluginClassLocations.closeAll() }.use {
              val oldBundledPluginsResolvers = oldPluginClassLocations.map { it.getPluginClassesResolver() }
              val newBundledPluginsResolvers = newPluginClassLocations.map { it.getPluginClassesResolver() }

              val oldIdeResolver = UnionResolver.create(listOf(oldPlatformResolver) + oldBundledPluginsResolvers)
              val newIdeResolver = UnionResolver.create(listOf(newPlatformResolver) + newBundledPluginsResolvers)

              appendData(oldIdeResolver, newIdeResolver, jdkResolver, introducedData, removedData)
            }
          }
        }
      }
      val apiSignatureToEvents = hashMapOf<ApiSignature, MutableSet<ApiEvent>>()
      val introducedIn = IntroducedIn(newIde.version)
      val removedIn = RemovedIn(newIde.version)

      for (signature in introducedData) {
        apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += introducedIn
      }
      for (signature in removedData) {
        apiSignatureToEvents.getOrPut(signature) { hashSetOf() } += removedIn
      }

      ApiReport(newIde.version, apiSignatureToEvents)
    }
  }

  private fun appendData(
      oldResolver: Resolver,
      newResolver: Resolver,
      jdkResolver: Resolver,
      introducedData: MutableSet<ApiSignature>,
      removedData: MutableSet<ApiSignature>
  ) {
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
      oldClass: ClassFile?,
      newClass: ClassFile,
      oldResolver: Resolver,
      newResolver: Resolver,
      introducedData: MutableSet<ApiSignature>
  ) {
    if (oldClass == null || !oldClass.isAccessible()) {
      val outerClassName = getOuterClassName(newClass.name)
      if (outerClassName != null && !oldResolver.containsClass(outerClassName)) {
        //Outer class is already added => no need to register this inner one.
        return
      }
      introducedData += newClass.location.toSignature()
      return
    }

    for (newMethod in newClass.methods) {
      if (!newMethod.isAccessible() || newMethod.isIgnored() || isMethodOverriding(newMethod, newClass, newResolver)) {
        continue
      }

      val oldMethod = oldClass.methods.find {
        it.name == newMethod.name && it.descriptor == newMethod.descriptor && it.isAccessible() && !it.isIgnored()
      }
      if (oldMethod == null) {
        introducedData += newMethod.location.toSignature()
      }
    }

    for (newField in newClass.fields) {
      if (!newField.isAccessible() || newField.isIgnored()) {
        continue
      }

      val oldField = oldClass.fields.find {
        it.name == newField.name && it.descriptor == newField.descriptor && it.isAccessible() && !it.isIgnored()
      }
      if (oldField == null) {
        introducedData += newField.location.toSignature()
      }
    }
  }

  /**
   * Appends all signatures available in [oldClass] but not available in [newClass].
   */
  private fun findRemovedApi(
      oldClass: ClassFile,
      newClass: ClassFile?,
      oldResolver: Resolver,
      newResolver: Resolver,
      removedData: MutableSet<ApiSignature>
  ) {
    if (newClass == null || !newClass.isAccessible()) {
      val outerClassName = getOuterClassName(oldClass.name)
      if (outerClassName != null && !newResolver.containsClass(outerClassName)) {
        //Outer class is already registered => no need to register this inner one.
        return
      }
      removedData += oldClass.location.toSignature()
      return
    }

    for (oldMethod in oldClass.methods) {
      if (!oldMethod.isAccessible() || oldMethod.isIgnored() || isMethodOverriding(oldMethod, oldClass, oldResolver)) {
        continue
      }

      val newMethod = newClass.methods.find {
        it.name == oldMethod.name && it.descriptor == oldMethod.descriptor && it.isAccessible() && !it.isIgnored()
      }
      if (newMethod == null) {
        removedData += oldMethod.location.toSignature()
      }
    }

    for (oldField in oldClass.fields) {
      if (!oldField.isAccessible() || oldField.isIgnored()) {
        continue
      }

      val newField = newClass.fields.find {
        it.name == oldField.name && it.descriptor == oldField.descriptor && it.isAccessible() && !it.isIgnored()
      }
      if (newField == null) {
        removedData += oldField.location.toSignature()
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

  private fun Resolver.safeFindClass(className: String): ClassFile? {
    return try {
      findClass(className)?.let { ClassFileAsm(it, FakeClassFileOrigin) }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return null
    }
  }

  private fun isMethodOverriding(methodNode: Method, classNode: ClassFile, resolver: Resolver): Boolean {
    if (methodNode.isConstructor
        || methodNode.isClassInitializer
        || methodNode.isStatic
        || methodNode.isPrivate
        || methodNode.isPackagePrivate
    ) {
      return false
    }

    val parentsVisitor = ClassParentsVisitor(true) { _, parentClassName ->
      resolver.safeFindClass(parentClassName)
    }

    val isOverriding = AtomicBoolean()
    parentsVisitor.visitClass(classNode, false, onEnter = { parentClass ->
      val hasSameMethod = parentClass.methods.any {
        it.name == methodNode.name
            && it.descriptor == methodNode.descriptor
            && !it.isStatic
            && !it.isPrivate
            && !it.isPackagePrivate
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

  private fun readBundledPluginsClassesLocations(ide: Ide): List<IdePluginClassesLocations> =
      ide.bundledPlugins.mapNotNull { readPluginClassesExceptionally(it) }

  private fun readPluginClassesExceptionally(idePlugin: IdePlugin): IdePluginClassesLocations? {
    if (idePlugin.pluginId in IGNORED_PLUGIN_IDS) {
      return null
    }
    LOG.debug("Reading class files of a bundled plugin $idePlugin  (${idePlugin.originalFile})")
    return IdePluginClassesFinder.findPluginClasses(idePlugin, Resolver.ReadMode.SIGNATURES, pluginClassesLocationsKeys)
  }

  private fun IdePluginClassesLocations.getPluginClassesResolver(): Resolver =
      pluginClassesLocationsKeys.mapNotNull { getResolver(it) }.let { UnionResolver.create(it) }

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
          || hasObfuscatedLikePackage(className)

  private fun ClassFile.isAccessible() = !isPrivate && !isPackagePrivate

  private fun ClassFile.isIgnored() = isIgnoredClassName(name) || isSynthetic

  private fun Method.isAccessible() = !isPrivate && !isPackagePrivate

  private fun Method.isIgnored() = isClassInitializer || isBridgeMethod || isSynthetic || name.isSyntheticLikeName()

  private fun Field.isAccessible() = !isPrivate && !isPackagePrivate

  private fun Field.isIgnored() = isSynthetic || name.isSyntheticLikeName()

}
package com.jetbrains.pluginverifier.verifiers.resolution

import java.nio.file.Path

/**
 * Classes with the same name may be found in several paths.
 * For example, a class `com.google.common.cache.Cache` may be found
 * in a `IDE/lib/guava.jar` and in a `plugin/guava.jar` but the class loader
 * constructed by IDE defines a specific search order:
 *
 * 1) firstly a class is searched among classes of the plugin
 * 2) if not found, among the classes of the used JDK
 * 3) if not found, among the libraries of the checked IDE
 * 4) if not found, among the classes of the plugin's dependencies
 *
 * This class specifies the first path where a class is found.
 *
 * If a class is not found in the previous paths,
 * a "no such class" problem may be reported.
 */
sealed class IntelliJClassFileOrigin(override val classPath: Path) : ClassFileOrigin {
  class PluginClass(classPath: Path) : IntelliJClassFileOrigin(classPath)

  class ClassOfPluginDependency(classPath: Path) : IntelliJClassFileOrigin(classPath)

  class IdeClass(classPath: Path) : IntelliJClassFileOrigin(classPath)

  class JdkClass(classPath: Path) : IntelliJClassFileOrigin(classPath)
}
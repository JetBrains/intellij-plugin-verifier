package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeClassFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginClassFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.NoExplicitDependencyOnJavaPluginWarning
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsageProcessor
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingClassUsageProcessor
import com.jetbrains.pluginverifier.usages.discouraging.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiRegistrar
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsageProcessor
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsageRegistrar
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiUsageProcessor
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginApiUsageRegistrar
import com.jetbrains.pluginverifier.usages.javaPlugin.JavaPluginClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiRegistrar
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsageProcessor
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyRegistrar
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter

data class PluginVerificationContext(
    val idePlugin: IdePlugin,
    val verificationDescriptor: PluginVerificationDescriptor,
    override val classResolver: Resolver,
    override val externalClassesPackageFilter: PackageFilter
) : VerificationContext,
    ProblemRegistrar,
    DeprecatedApiRegistrar,
    ExperimentalApiRegistrar,
    OverrideOnlyRegistrar,
    InternalApiUsageRegistrar,
    NonExtendableApiRegistrar,
    JavaPluginApiUsageRegistrar {

  override val apiUsageProcessors: List<ApiUsageProcessor> =
      listOf(
          DeprecatedApiUsageProcessor(this),
          ExperimentalApiUsageProcessor(this),
          DiscouragingClassUsageProcessor(this),
          InternalApiUsageProcessor(this),
          OverrideOnlyMethodUsageProcessor(this),
          JavaPluginApiUsageProcessor(this)
      )

  fun postProcessResults() {
    groupMissingClassesToMissingPackages()
  }

  val compatibilityProblems = hashSetOf<CompatibilityProblem>()
  val compatibilityWarnings = hashSetOf<CompatibilityWarning>()
  val deprecatedUsages = hashSetOf<DeprecatedApiUsage>()
  val experimentalApiUsages = hashSetOf<ExperimentalApiUsage>()
  val internalApiUsages = hashSetOf<InternalApiUsage>()
  val nonExtendableApiUsages = hashSetOf<NonExtendableApiUsage>()
  val overrideOnlyMethodUsages = hashSetOf<OverrideOnlyMethodUsage>()

  override val problemRegistrar
    get() = this

  override fun registerProblem(problem: CompatibilityProblem) {
    compatibilityProblems += problem
  }

  override fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    val deprecatedElementHost = deprecatedApiUsage.apiElement.containingClass
    val usageHostClass = deprecatedApiUsage.usageLocation.containingClass
    if (deprecatedApiUsage is DiscouragingJdkClassUsage || shouldIndexDeprecatedClass(usageHostClass, deprecatedElementHost)) {
      deprecatedUsages += deprecatedApiUsage
    }
  }

  override fun registerExperimentalApiUsage(experimentalApiUsage: ExperimentalApiUsage) {
    val elementHostClass = experimentalApiUsage.apiElement.containingClass
    val usageHostClass = experimentalApiUsage.usageLocation.containingClass
    if (shouldIndexDeprecatedClass(usageHostClass, elementHostClass)) {
      experimentalApiUsages += experimentalApiUsage
    }
  }

  override fun registerInternalApiUsage(internalApiUsage: InternalApiUsage) {
    internalApiUsages += internalApiUsage
  }

  override fun registerNonExtendableApiUsage(nonExtendableApiUsage: NonExtendableApiUsage) {
    nonExtendableApiUsages += nonExtendableApiUsage
  }

  override fun registerOverrideOnlyMethodUsage(overrideOnlyMethodUsage: OverrideOnlyMethodUsage) {
    overrideOnlyMethodUsages += overrideOnlyMethodUsage
  }

  override fun registerJavaPluginClassUsage(javaPluginClassUsage: JavaPluginClassUsage) {
    if (idePlugin.dependencies.none { it.id == "com.intellij.modules.java" || it.id == "com.intellij.java" }) {
      registerCompatibilityWarning(NoExplicitDependencyOnJavaPluginWarning(javaPluginClassUsage))
    }
  }

  fun registerCompatibilityWarning(warning: CompatibilityWarning) {
    compatibilityWarnings += warning
  }

  private fun shouldIndexDeprecatedClass(usageHostClass: ClassLocation, apiHostClass: ClassLocation): Boolean {
    val usageHostOrigin = usageHostClass.classFileOrigin
    if (idePlugin == usageHostOrigin.findOriginOfType<PluginClassFileOrigin>()?.idePlugin) {
      val apiHostOrigin = apiHostClass.classFileOrigin
      if (apiHostOrigin.isOriginOfType<IdeClassFileOrigin>()) {
        return true
      }
      val pluginOrigin = apiHostOrigin.findOriginOfType<PluginClassFileOrigin>()
      if (pluginOrigin != null && pluginOrigin.idePlugin != idePlugin) {
        return true
      }
    }
    return false
  }

  /**
   * Returns the top-most package of the given [className] that is not available in this [Resolver].
   *
   * If all packages of the specified class exist, `null` is returned.
   * If the class has default (empty) package, and that default package
   * is not available, then "" is returned.
   */
  private fun Resolver.getTopMostMissingPackage(className: String): String? {
    if ('/' !in className) {
      return if (containsPackage("")) {
        null
      } else {
        ""
      }
    }
    val packageParts = className.substringBeforeLast('/', "").split('/')
    var superPackage = ""
    for (packagePart in packageParts) {
      if (superPackage.isNotEmpty()) {
        superPackage += '/'
      }
      superPackage += packagePart
      if (!containsPackage(superPackage)) {
        return superPackage
      }
    }
    return null
  }

  /**
   * Post-processes the verification result and groups many [ClassNotFoundProblem]s into [PackageNotFoundProblem]s,
   * to make the report easier to understand.
   */
  private fun groupMissingClassesToMissingPackages() {
    val classNotFoundProblems = compatibilityProblems.filterIsInstance<ClassNotFoundProblem>()

    /**
     * All [ClassNotFoundProblem]s will be split into 2 parts:
     * 1) Independent [ClassNotFoundProblem]s for classes
     * that originate from found packages.
     * These classes seem to be removed, causing API breakages.
     *
     * 2) Grouped [PackageNotFoundProblem]s for several [ClassNotFoundProblem]s
     * for packages that are not found.
     * These missing packages might have been removed,
     * or the Verifier is not properly configured to find them.
     */
    val noClassProblems = hashSetOf<ClassNotFoundProblem>()
    val packageToMissingProblems = hashMapOf<String, MutableSet<ClassNotFoundProblem>>()

    for (classNotFoundProblem in classNotFoundProblems) {
      val className = classNotFoundProblem.unresolved.className
      val missingPackage = classResolver.getTopMostMissingPackage(className)
      if (missingPackage != null) {
        packageToMissingProblems
            .getOrPut(missingPackage) { hashSetOf() }
            .add(classNotFoundProblem)
      } else {
        noClassProblems.add(classNotFoundProblem)
      }
    }

    val packageNotFoundProblems = packageToMissingProblems.map { (packageName, missingClasses) ->
      PackageNotFoundProblem(packageName, missingClasses)
    }

    //Retain all individual [ClassNotFoundProblem]s.
    for (problem in classNotFoundProblems) {
      if (problem !in noClassProblems) {
        compatibilityProblems -= problem
      }
    }

    for (packageNotFoundProblem in packageNotFoundProblems) {
      compatibilityProblems += packageNotFoundProblem
    }
  }
}
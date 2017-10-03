package com.jetbrains.pluginverifier.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DependencyResolver
import com.jetbrains.pluginverifier.filter.ProblemsFilter

/**
 * Accumulates parameters of the upcoming verification.
 */
data class VerifierParams(

    /**
     * The JDK against which the plugins will be verified.
     */
    val jdkDescriptor: JdkDescriptor,

    /**
     * Ignore missing classes having the listed packages
     */
    val externalClassesPrefixes: List<String>,

    /**
     * Problems filter to ignore unrelated or known problems
     */
    val problemFilters: List<ProblemsFilter>,

    /**
     * The Resolver for external classes. The verification can refer to them.
     */
    val externalClassPath: Resolver,

    /**
     * If set, this resolver will be used to resolve plugin dependencies.
     * Otherwise a default resolver which searches the plugin in the IDE
     * and in the Plugin Repository will be used.
     */
    val dependencyResolver: DependencyResolver
) {
  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }
}
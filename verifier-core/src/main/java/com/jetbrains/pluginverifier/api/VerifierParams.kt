package com.jetbrains.pluginverifier.api

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DependencyResolver

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
    val externalClassesPrefixes: List<String> = emptyList(),

    /**
     * Problems filter to ignore unrelated or known problems
     */
    val problemFilter: ProblemsFilter = ProblemsFilter.AlwaysTrue,

    /**
     * The Resolver for external classes. The verification can refer to them.
     */
    val externalClassPath: Resolver = Resolver.getEmptyResolver(),

    /**
     * If set, this resolver will be used to resolve plugin dependencies.
     * Otherwise a default resolver which searches the plugin in the IDE
     * and in the Plugin Repository will be used.
     */
    val dependencyResolver: DependencyResolver? = null
) {
  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }
}
package com.jetbrains.pluginverifier.api

import com.intellij.structure.resolvers.Resolver

/**
 * Accumulates parameters of the upcoming verification.
 */
data class VerifierParams(

    /**
     * The JDK against which the plugins will be verified.
     */
    val jdkDescriptor: JdkDescriptor,

    /**
     * The pairs of _(plugin, ide)_ which will be verified.
     */
    val pluginsToCheck: List<Pair<PluginDescriptor, IdeDescriptor>>,

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
    val dependencyResolver: DependencyResolver? = null,

    /**
     * The number of concurrent workers
     */
    val concurrentWorkers: Int = Runtime.getRuntime().availableProcessors()
) {
  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }
}
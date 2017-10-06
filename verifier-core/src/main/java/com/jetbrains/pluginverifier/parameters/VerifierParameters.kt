package com.jetbrains.pluginverifier.parameters

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter

/**
 * Accumulates parameters of the upcoming verification.
 */
data class VerifierParameters(

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
    val dependencyFinder: DependencyFinder
)
package com.jetbrains.pluginverifier.parameters

import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter

/**
 * Accumulates parameters of the upcoming verification.
 */
data class VerifierParameters(

    /**
     * Ignore missing classes having the listed packages
     */
    val externalClassesPrefixes: List<String>,

    /**
     * Problems filter to ignore unrelated or known problems
     */
    val problemFilters: List<ProblemsFilter>,

    /**
     * Whether the usages of deprecated IntelliJ Platform API must be reported or not
     */
    val findDeprecatedApiUsages: Boolean
)
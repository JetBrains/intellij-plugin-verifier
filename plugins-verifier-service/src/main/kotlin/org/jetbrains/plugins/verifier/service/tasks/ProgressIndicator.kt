package org.jetbrains.plugins.verifier.service.tasks

/**
 * Progress indicator is used to track task execution progress.
 */
data class ProgressIndicator(
    @Volatile var fraction: Double = 0.0,
    @Volatile var text: String = ""
)
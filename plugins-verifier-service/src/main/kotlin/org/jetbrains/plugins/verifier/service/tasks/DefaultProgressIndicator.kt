package org.jetbrains.plugins.verifier.service.tasks

/**
 * The default implementation of the [ProgressIndicator],
 * which can be safely used in a concurrent environment.
 */
data class DefaultProgressIndicator(
    @Volatile override var fraction: Double = 0.0,
    @Volatile override var text: String = ""
) : ProgressIndicator
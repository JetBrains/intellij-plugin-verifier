package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Base class for all events associated with API.
 */
sealed class ApiEvent {
  abstract val ideVersion: IdeVersion
}

/**
 * API was introduced in [ideVersion].
 */
data class IntroducedIn(override val ideVersion: IdeVersion) : ApiEvent()

/**
 * API was removed in [ideVersion].
 */
data class RemovedIn(override val ideVersion: IdeVersion) : ApiEvent()
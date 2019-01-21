package org.jetbrains.ide.diff.builder.api

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Base class for all events associated with API.
 */
sealed class ApiEvent

/**
 * API was introduced in [ideVersion].
 */
data class IntroducedIn(val ideVersion: IdeVersion) : ApiEvent()

/**
 * API was removed in [ideVersion].
 */
data class RemovedIn(val ideVersion: IdeVersion) : ApiEvent()
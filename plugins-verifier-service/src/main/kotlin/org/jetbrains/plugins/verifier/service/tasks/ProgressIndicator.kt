package org.jetbrains.plugins.verifier.service.tasks

/**
 * Progress indicator is used to track task execution progress.
 */
interface ProgressIndicator {

  var fraction: Double

  var text: String

}
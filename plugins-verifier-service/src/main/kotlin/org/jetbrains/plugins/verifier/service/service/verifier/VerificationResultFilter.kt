/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Utility class used to filter verifications that should be sent to JetBrains Marketplace.
 */
class VerificationResultFilter {

  companion object {
    private const val RECHECK_ATTEMPTS = 3

    private val RECHECK_ATTEMPT_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES)

    private val CLEANUP_TIMEOUT = Duration.ofHours(12)
  }

  private var lastCleanupTime = Instant.EPOCH

  private val _failedAttempts = hashMapOf<UpdateInfo, MutableList<VerificationAttempt>>()

  val failedAttempts: Map<UpdateInfo, List<VerificationAttempt>>
    @Synchronized
    get() {
      doCleanup()
      //Make a deep copy
      return _failedAttempts.mapValues { it.value.toList() }
    }

  @Synchronized
  private fun doCleanup() {
    val nowInstant = Instant.now()
    if (lastCleanupTime.plus(CLEANUP_TIMEOUT) < nowInstant) {
      lastCleanupTime = nowInstant
      val iterator = _failedAttempts.iterator()
      while (iterator.hasNext()) {
        val attempts = iterator.next().value
        if (attempts.isNotEmpty()) {
          val lastAttemptInstant = attempts.map { it.verificationEndTime }.maxOrNull()!!
          if (lastAttemptInstant.plus(CLEANUP_TIMEOUT) < nowInstant) {
            iterator.remove()
          }
        }
      }
    }
  }

  @Synchronized
  fun shouldStartVerification(scheduledVerification: ScheduledVerification, nowTime: Instant): Boolean {
    doCleanup()
    val failedAttempts = _failedAttempts[scheduledVerification.updateInfo].orEmpty()
    val lastFailedAttempt = failedAttempts.maxByOrNull { it: VerificationAttempt -> it.verificationEndTime } ?: return true
    return lastFailedAttempt.failureReason.shouldRecheck
      && nowTime > lastFailedAttempt.verificationEndTime.plus(RECHECK_ATTEMPT_TIMEOUT)
  }

  @Synchronized
  fun shouldSendVerificationResult(
    verificationResult: PluginVerificationResult,
    verificationEndTime: Instant,
    scheduledVerification: ScheduledVerification
  ): Boolean {
    doCleanup()
    val updateInfo = scheduledVerification.updateInfo
    val failureReason = getFailureReason(verificationResult)
    if (failureReason == null) {
      //Clear failed attempts for plugins that were successfully downloaded and verified.
      _failedAttempts.remove(updateInfo)
      return true
    }
    val attempts = _failedAttempts.getOrPut(updateInfo) { arrayListOf() }
    attempts += VerificationAttempt(verificationResult, scheduledVerification, verificationEndTime, failureReason)

    val times = attempts.map { it.verificationEndTime }
    val firstTime = times.minOrNull()!!
    val lastTime = times.maxOrNull()!!

    val triedEnough = attempts.size >= RECHECK_ATTEMPTS
      && Duration.between(firstTime, lastTime) > RECHECK_ATTEMPT_TIMEOUT.multipliedBy(RECHECK_ATTEMPTS.toLong())

    if (triedEnough) {
      //Clear failed attempts for already sent results.
      _failedAttempts.remove(updateInfo)
    }
    return triedEnough
  }

  private fun getFailureReason(verificationResult: PluginVerificationResult): FailureReason? = when (verificationResult) {
    is PluginVerificationResult.NotFound -> {
      FailureReason(verificationResult.notFoundReason, true)
    }
    is PluginVerificationResult.FailedToDownload -> {
      FailureReason(verificationResult.failedToDownloadReason, true)
    }
    is PluginVerificationResult.Verified -> {
      if (verificationResult.compatibilityProblems.any { it is FailedToReadClassFileProblem }) {
        val message = "MP-3191: Plugin Verifier service sporadically produces 'Failed to read class file: ChannelClosed'. " +
          "Temporary ignore verifications containing such problems to detect the root cause. " +
          "There were the following such problems detected:\n" +
          verificationResult.compatibilityProblems.filterIsInstance<FailedToReadClassFileProblem>()
            .joinToString("\n") { it.fullDescription }
        FailureReason(message, false)
      } else {
        null
      }
    }
    else -> null
  }

  data class FailureReason(
    val reason: String,
    val shouldRecheck: Boolean
  )

  data class VerificationAttempt(
    val verificationResult: PluginVerificationResult,
    val scheduledVerification: ScheduledVerification,
    val verificationEndTime: Instant,
    val failureReason: FailureReason
  )

}

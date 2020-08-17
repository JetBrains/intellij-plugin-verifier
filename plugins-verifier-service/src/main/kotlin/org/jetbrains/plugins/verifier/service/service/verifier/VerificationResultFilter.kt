/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Utility class used to filter verifications that should be sent to the Marketplace.
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
          val lastAttemptInstant = attempts.map { it.verificationEndTime }.max()!!
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
    val failedAttempts = _failedAttempts[scheduledVerification.updateInfo]
    if (failedAttempts == null || failedAttempts.isEmpty()) {
      return true
    }

    val lastFailedTime = failedAttempts.map { it.verificationEndTime }.max()!!
    return nowTime > lastFailedTime.plus(RECHECK_ATTEMPT_TIMEOUT)
  }

  @Synchronized
  fun shouldSendVerificationResult(
    verificationResult: PluginVerificationResult,
    verificationEndTime: Instant,
    scheduledVerification: ScheduledVerification
  ): Boolean {
    doCleanup()
    val updateInfo = scheduledVerification.updateInfo
    if (verificationResult !is PluginVerificationResult.NotFound && verificationResult !is PluginVerificationResult.FailedToDownload) {
      //Clear failed attempts for plugins that were successfully downloaded and verified.
      _failedAttempts.remove(updateInfo)
      return true
    }
    val failureReason = (verificationResult as? PluginVerificationResult.NotFound)?.notFoundReason
      ?: (verificationResult as PluginVerificationResult.FailedToDownload).failedToDownloadReason
    val attempts = _failedAttempts.getOrPut(updateInfo) { arrayListOf() }
    attempts += VerificationAttempt(verificationResult, scheduledVerification, verificationEndTime, failureReason)

    val times = attempts.map { it.verificationEndTime }
    val firstTime = times.min()!!
    val lastTime = times.max()!!

    val triedEnough = attempts.size >= RECHECK_ATTEMPTS
      && Duration.between(firstTime, lastTime) > RECHECK_ATTEMPT_TIMEOUT.multipliedBy(RECHECK_ATTEMPTS.toLong())

    if (triedEnough) {
      //Clear failed attempts for already sent results.
      _failedAttempts.remove(updateInfo)
    }
    return triedEnough
  }

  data class VerificationAttempt(
    val verificationResult: PluginVerificationResult,
    val scheduledVerification: ScheduledVerification,
    val verificationEndTime: Instant,
    val failureReason: String
  )

}
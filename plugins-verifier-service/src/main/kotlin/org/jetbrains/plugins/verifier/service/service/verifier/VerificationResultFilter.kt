package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.PluginVerificationResult
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

  private val _failedFetchAttempts = hashMapOf<UpdateInfo, MutableList<VerificationAttempt>>()

  val failedFetchAttempts: Map<UpdateInfo, List<VerificationAttempt>>
    @Synchronized
    get() {
      doCleanup()
      //Make a deep copy
      return _failedFetchAttempts.mapValues { it.value.toList() }
    }

  @Synchronized
  private fun doCleanup() {
    val nowInstant = Instant.now()
    if (lastCleanupTime.plus(CLEANUP_TIMEOUT) < nowInstant) {
      lastCleanupTime = nowInstant
      val iterator = _failedFetchAttempts.iterator()
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
    val failedAttempts = _failedFetchAttempts[scheduledVerification.updateInfo]
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
      _failedFetchAttempts.remove(updateInfo)
      return true
    }
    val attempts = _failedFetchAttempts.getOrPut(updateInfo) { arrayListOf() }
    attempts += VerificationAttempt(verificationResult, scheduledVerification, verificationEndTime)

    val times = attempts.map { it.verificationEndTime }
    val firstTime = times.min()!!
    val lastTime = times.max()!!

    val triedEnough = attempts.size >= RECHECK_ATTEMPTS
        && Duration.between(firstTime, lastTime) > RECHECK_ATTEMPT_TIMEOUT.multipliedBy(RECHECK_ATTEMPTS.toLong())

    if (triedEnough) {
      //Clear failed attempts for already sent results.
      _failedFetchAttempts.remove(updateInfo)
    }
    return triedEnough
  }

  data class VerificationAttempt(
      val verificationResult: PluginVerificationResult,
      val scheduledVerification: ScheduledVerification,
      val verificationEndTime: Instant
  )

}
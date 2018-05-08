package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Ignore
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Send
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Utility class used to control which verifications
 * should actually be sent to the Plugins Repository.
 *
 * Some verifications may be uncertain, such as those
 * that report too many problems (>100).
 * For such verifications it is better to investigate the reasons
 * manually than simply send them to confused end users.
 *
 * This class maintains a set of [ignoredVerifications].
 * That is possible to manually [unignore][unignoreVerificationResultFor]
 * verifications so that they will be sent later on.
 */
class VerificationResultFilter {

  companion object {
    private const val TOO_MANY_PROBLEMS_THRESHOLD = 100

    private val LOG = LoggerFactory.getLogger(VerificationResultFilter::class.java)
  }

  private val acceptedVerifications = hashSetOf<ScheduledVerification>()

  private val _ignoredVerifications = hashMapOf<ScheduledVerification, Result.Ignore>()

  val ignoredVerifications: Map<ScheduledVerification, Result.Ignore>
    get() = _ignoredVerifications

  /**
   * Accepts verification of a plugin against IDE specified by [scheduledVerification].
   *
   * When the verification of the plugin against this IDE occurs next time
   * the verification result will be sent anyway.
   */
  @Synchronized
  fun unignoreVerificationResultFor(scheduledVerification: ScheduledVerification) {
    LOG.info("Unignore verification result for $scheduledVerification")
    acceptedVerifications.add(scheduledVerification)
    _ignoredVerifications.remove(scheduledVerification)
  }

  /**
   * Determines whether the verification result should be sent.
   *
   * Currently, if the verification reports too many compatibility problems,
   * which is more than [TOO_MANY_PROBLEMS_THRESHOLD],
   * the result is not sent unless it has been manually accepted
   * via [unignoreVerificationResultFor].
   */
  @Synchronized
  fun shouldSendVerificationResult(verificationResult: VerificationResult,
                                   verificationEndTime: Instant): Result {
    with(verificationResult) {
      val compatibilityProblems = when (this) {
        is VerificationResult.OK -> emptySet()
        is VerificationResult.StructureWarnings -> emptySet()
        is VerificationResult.InvalidPlugin -> emptySet()
        is VerificationResult.NotFound -> emptySet()
        is VerificationResult.FailedToDownload -> emptySet()
        is VerificationResult.MissingDependencies -> compatibilityProblems
        is VerificationResult.CompatibilityProblems -> compatibilityProblems
      }

      val scheduledVerification = ScheduledVerification(plugin as UpdateInfo, (verificationTarget as VerificationTarget.Ide).ideVersion)

      if (compatibilityProblems.size > TOO_MANY_PROBLEMS_THRESHOLD) {
        if (scheduledVerification in acceptedVerifications) {
          LOG.info("Verification $scheduledVerification has been accepted, though there are many compatibility problems: ${compatibilityProblems.size}")
          return Result.Send
        }
        val reason = "There are too many compatibility problems between $plugin and $verificationTarget: ${compatibilityProblems.size}"
        LOG.info(reason)
        val verdict = this.toString()
        val ignore = Result.Ignore(verdict, verificationEndTime, reason)
        _ignoredVerifications[scheduledVerification] = ignore
        return ignore
      }

      return Result.Send
    }
  }

  /**
   * Possible decisions on whether to send a verification result: either [Send] or [Ignore].
   */
  sealed class Result {

    /**
     * The verification result should be sent.
     */
    object Send : Result()

    /**
     * The verification result has been ignored by some [reason][ignoreReason].
     */
    data class Ignore(val verificationVerdict: String,
                      val verificationEndTime: Instant,
                      val ignoreReason: String) : Result()

  }

}
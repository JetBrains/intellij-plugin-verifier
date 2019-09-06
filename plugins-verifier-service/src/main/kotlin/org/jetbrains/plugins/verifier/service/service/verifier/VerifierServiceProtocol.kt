package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerificationResult

/**
 * Protocol used to communicate with the Marketplace:
 * 1) Request scheduled verifications: [requestScheduledVerifications].
 * 2) Send the verification results: [sendVerificationResult].
 */
interface VerifierServiceProtocol {

  fun requestScheduledVerifications(): List<ScheduledVerification>

  fun sendVerificationResult(scheduledVerification: ScheduledVerification, verificationResult: PluginVerificationResult)

}
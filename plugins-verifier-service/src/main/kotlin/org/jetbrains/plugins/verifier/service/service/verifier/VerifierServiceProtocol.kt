package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult

/**
 * Protocol used to communicate with the Marketplace:
 * 1) Request scheduled verifications: [requestScheduledVerifications].
 * 2) Send the verification results: [sendVerificationResult].
 */
interface VerifierServiceProtocol {

  fun requestScheduledVerifications(): List<ScheduledVerification>

  fun sendVerificationResult(verificationResult: VerificationResult, updateInfo: UpdateInfo)

}
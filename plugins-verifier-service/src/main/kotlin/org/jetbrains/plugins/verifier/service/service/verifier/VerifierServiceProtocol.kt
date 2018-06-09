package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult

interface VerifierServiceProtocol {

  fun requestScheduledVerifications(): List<ScheduledVerification>

  fun sendVerificationResult(verificationResult: VerificationResult, updateInfo: UpdateInfo)

}
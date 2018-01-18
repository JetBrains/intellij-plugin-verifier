package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult

interface VerifierServiceProtocol {

  fun requestUpdatesToCheck(availableIde: IdeVersion): List<UpdateInfo>

  fun sendVerificationResult(verificationResult: VerificationResult)

}
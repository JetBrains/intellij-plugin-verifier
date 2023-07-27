/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.PluginVerificationResult

/**
 * Protocol used to communicate with JetBrains Marketplace:
 * 1) Request scheduled verifications: [requestScheduledVerifications].
 * 2) Send the verification results: [sendVerificationResult].
 */
interface VerifierServiceProtocol {

  fun requestScheduledVerifications(): List<ScheduledVerification>

  fun sendVerificationResult(scheduledVerification: ScheduledVerification, verificationResult: PluginVerificationResult)

}

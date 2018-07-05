package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.AvailableIde

/**
 * Protocol of communication with Marketplace
 * that allows to send available IDE builds
 * to schedule verifications.
 */
interface AvailableIdeProtocol {

  fun sendAvailableIdes(availableIdes: List<AvailableIde>)

}
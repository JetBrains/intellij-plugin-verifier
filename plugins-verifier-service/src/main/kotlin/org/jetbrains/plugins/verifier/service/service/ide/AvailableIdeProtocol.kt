package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.AvailableIde

/**
 * High-level protocol of communication between
 * the verifier service and the Marketplace.
 */
interface AvailableIdeProtocol {

  fun sendAvailableIdes(availableIdes: List<AvailableIde>)

}
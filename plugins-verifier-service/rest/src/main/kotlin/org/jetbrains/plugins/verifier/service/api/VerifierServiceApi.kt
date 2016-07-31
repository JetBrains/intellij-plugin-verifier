package org.jetbrains.plugins.verifier.service.api

import org.jetbrains.plugins.verifier.service.client.CLIENT_SIDE_VERSION
import org.jetbrains.plugins.verifier.service.client.VerifierService
import org.jetbrains.plugins.verifier.service.client.executeSuccessfully

abstract class VerifierServiceApi<out T>(val host: String) {
  internal val service = VerifierService(host)

  abstract fun executeImpl(): T

  fun execute(): T {
    val supported: List<String>
    try {
      supported = service.statusService.getSupportedClients().executeSuccessfully().body()
    } catch(e: Exception) {
      throw RuntimeException("Plugin verifier service $host is not available", e)
    }
    if (CLIENT_SIDE_VERSION !in supported) {
      throw IllegalStateException("Please update your rest-client distribution from version $CLIENT_SIDE_VERSION to ${supported.last()}")
    }
    return executeImpl()
  }

}
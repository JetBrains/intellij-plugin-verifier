package org.jetbrains.plugins.verifier.service.api

interface VerifierServiceApi<out T> {
  fun execute(): T
}
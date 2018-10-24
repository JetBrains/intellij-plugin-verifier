package org.jetbrains.ide.diff.builder.api

import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Container of all API elements of a library.
 */
data class ApiData(private val _apiSignatures: MutableSet<ApiSignature> = mutableSetOf()) {

  val apiSignatures: Set<ApiSignature>
    get() = _apiSignatures

  operator fun contains(apiSignature: ApiSignature) =
      apiSignature in _apiSignatures

  fun addSignature(signature: ApiSignature) {
    _apiSignatures.add(signature)
  }

}
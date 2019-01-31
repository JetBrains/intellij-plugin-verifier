package org.jetbrains.ide.diff.builder.api

import org.jetbrains.ide.diff.builder.signatures.ApiSignature

/**
 * Container of all API elements of a library.
 */
data class ApiData(val apiSignatures: Set<ApiSignature>) {

  operator fun contains(apiSignature: ApiSignature) =
      apiSignature in apiSignatures

}
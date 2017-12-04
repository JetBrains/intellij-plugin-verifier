package org.jetbrains.plugins.verifier.service.server.database

sealed class ValueType<T> {
  object STRING : ValueType<String>()

  object INT : ValueType<Int>()
}
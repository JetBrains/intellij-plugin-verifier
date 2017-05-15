package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

interface DependencyResolver {

  fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): Result

  sealed class Result : Closeable {
    class Found(val plugin: Plugin, val resolver: Resolver, val fileLock: FileLock) : Result() {
      override fun close() {
        try {
          fileLock.release()
        } finally {
          resolver.close()
        }
      }
    }

    class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }

    object Skip : Result() {
      override fun close() = Unit
    }
  }
}
/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.cleanup.SizeWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable
import java.nio.file.Path

/**
 * Cache of [IdeDescriptor] associated by [IdeVersion].
 *
 * This must be [close] on the application shutdown to deallocate all [IdeDescriptor]s.
 */
class IdeDescriptorsCache(cacheSize: Int, ideFilesBank: IdeFilesBank, defaultJdkPath: Path) : Closeable {

  private val descriptorsCache = createSizeLimitedResourceCache(
    cacheSize,
    IdeDescriptorResourceProvider(ideFilesBank, defaultJdkPath),
    { it.close() },
    "IdeDescriptorsCache"
  )

  /**
   * Atomically creates an [IdeDescriptor] for IDE [ideVersion] and registers a [ResourceCacheEntry] for it.
   * The cache's state is not modified until this method returns.
   */
  @Throws(InterruptedException::class)
  fun getIdeDescriptorCacheEntry(ideVersion: IdeVersion): Result {
    val resourceCacheEntryResult = descriptorsCache.getResourceCacheEntry(ideVersion)
    return with(resourceCacheEntryResult) {
      when (this) {
        is ResourceCacheEntryResult.Found -> Result.Found(resourceCacheEntry)
        is ResourceCacheEntryResult.Failed -> Result.Failed(message, error)
        is ResourceCacheEntryResult.NotFound -> Result.NotFound(message)
      }
    }
  }

  sealed class Result : Closeable {
    data class Found(private val resourceCacheEntry: ResourceCacheEntry<IdeDescriptor, SizeWeight>) : Result() {

      val ideDescriptor: IdeDescriptor
        get() = resourceCacheEntry.resource

      override fun close() = resourceCacheEntry.close()
    }

    data class Failed(val reason: String, val error: Throwable) : Result() {
      override fun close() = Unit
    }

    data class NotFound(val reason: String) : Result() {
      override fun close() = Unit
    }
  }

  private class IdeDescriptorResourceProvider(
    private val ideFilesBank: IdeFilesBank,
    private val defaultJdkPath: Path
  ) : ResourceProvider<IdeVersion, IdeDescriptor> {

    override fun provide(key: IdeVersion): ProvideResult<IdeDescriptor> {
      return when (val result = ideFilesBank.getIdeFile(key)) {
        is IdeFilesBank.Result.Found -> {
          val ideLock = result.ideFileLock
          val ideDescriptor = try {
            IdeDescriptor.create(ideLock.file, defaultJdkPath, key, ideLock)
          } catch (e: Exception) {
            ideLock.closeLogged()
            e.rethrowIfInterrupted()
            return ProvideResult.Failed("Unable to open IDE $key: ${e.message}", e)
          }
          ProvideResult.Provided(ideDescriptor)
        }
        is IdeFilesBank.Result.NotFound -> ProvideResult.NotFound("IDE $key is not found: ${result.reason}")
        is IdeFilesBank.Result.Failed -> ProvideResult.Failed("IDE $key can't be downloaded: ${result.reason}", result.exception)
      }
    }
  }

  override fun close() = descriptorsCache.close()
}
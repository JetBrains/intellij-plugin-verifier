package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.cache.createSizeLimitedResourceCache
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

/**
 * Caches the [IdeDescriptor]s by [IdeVersion]s.
 *
 * The cache must be [closed] [close] on the application shutdown
 * to deallocate all the [IdeDescriptor]s.
 */
class IdeDescriptorsCache(cacheSize: Int,
                          private val ideFilesBank: IdeFilesBank) : Closeable {

  private val resourceCache = createSizeLimitedResourceCache(
      cacheSize,
      IdeDescriptorResourceProvider(ideFilesBank),
      { it.close() },
      "IdeDescriptorsCache"
  )

  /**
   * Atomically [selects] [selector] an IDE from a set of [available] [IdeFilesBank.getAvailableIdeVersions] IDEs
   * and registers a [ResourceCacheEntry] for the [IdeDescriptor].
   * The cache's state is not modified until this method returns.
   */
  fun getIdeDescriptor(selector: (Set<IdeVersion>) -> IdeVersion): ResourceCacheEntry<IdeDescriptor> =
      getIdeDescriptors {
        listOf(selector(it))
      }.single()

  /**
   * Atomically [selects] [selector] several IDEs from a set of all [available] [IdeFilesBank.getAvailableIdeVersions] IDEs
   * and registers the [ResourceCacheEntry]s for the corresponding [IdeDescriptor]s.
   * The cache's state is not modified until this method returns.
   */
  fun getIdeDescriptors(selector: (Set<IdeVersion>) -> List<IdeVersion>): List<ResourceCacheEntry<IdeDescriptor>> {
    //Register fake locks protecting IDEs from deletion
    //while requesting the cache entries.
    val ides = arrayListOf<Pair<IdeVersion, FileLock>>()
    try {
      ideFilesBank.lockAndAccess {
        val ideVersions = selector(ideFilesBank.getAvailableIdeVersions())
        for (ideVersion in ideVersions) {
          val result = ideFilesBank.getIde(ideVersion)
          if (result is IdeFilesBank.Result.Found) {
            ides.add(ideVersion to result.ideFileLock)
          }
        }
      }
    } catch (e: Throwable) {
      ides.forEach { it.second.release() }
      throw e
    }

    val result = arrayListOf<ResourceCacheEntry<IdeDescriptor>>()
    try {
      for ((ideVersion, _) in ides) {
        result.add(getIdeDescriptorCacheEntry(ideVersion))
      }
    } catch (e: Throwable) {
      result.forEach { it.close() }
      throw e
    } finally {
      //Release all the fake locks as they
      //are handed over to the [ResourceCacheEntry]s.
      ides.forEach {
        it.second.release()
      }
    }
    return result
  }

  /**
   * Atomically creates an [IdeDescriptor] for IDE [ideVersion]
   * and registers a [ResourceCacheEntry] for it.
   * The cache's state is not modified until this method returns.
   */
  fun getIdeDescriptorCacheEntry(ideVersion: IdeVersion): ResourceCacheEntry<IdeDescriptor> {
    val resourceCacheEntryResult = resourceCache.getResourceCacheEntry(ideVersion)
    return with(resourceCacheEntryResult) {
      when (this) {
        is ResourceCacheEntryResult.Found -> resourceCacheEntry
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException(message, error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException(message)
      }
    }
  }

  /**
   * Implementation of the [ResourceProvider] that provides the IDE files from the [ideFilesBank].
   */
  private class IdeDescriptorResourceProvider(private val ideFilesBank: IdeFilesBank) : ResourceProvider<IdeVersion, IdeDescriptor> {

    override fun provide(key: IdeVersion): ProvideResult<IdeDescriptor> {
      val result = ideFilesBank.getIde(key)
      val ideLock = (result as? IdeFilesBank.Result.Found)?.ideFileLock
          ?: return ProvideResult.NotFound("IDE $key is not found in the $ideFilesBank")
      val ideDescriptor = try {
        IdeDescriptorCreator.createByPath(ideLock.file, null)
      } catch (e: Exception) {
        return ProvideResult.Failed("Unable to open IDE $key", e)
      }
      return ProvideResult.Provided(ideDescriptor)
    }
  }

  override fun close() = resourceCache.close()
}
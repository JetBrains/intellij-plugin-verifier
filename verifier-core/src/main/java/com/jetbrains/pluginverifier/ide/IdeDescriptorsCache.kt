package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cache.ResourceCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntry
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.io.Closeable

class IdeDescriptorsCache(cacheSize: Int,
                          private val ideFilesBank: IdeFilesBank) : Closeable {

  private val resourceCache = ResourceCache(
      cacheSize.toLong(),
      IdeDescriptorResourceProvider(ideFilesBank),
      { it.close() },
      "IdeDescriptorsCache"
  )

  fun getIdeDescriptor(selector: (Iterable<IdeVersion>) -> IdeVersion): ResourceCacheEntry<IdeDescriptor> =
      getIdeDescriptors { availableIdeVersions ->
        listOf(selector(availableIdeVersions))
      }.single()

  fun getIdeDescriptors(selector: (Iterable<IdeVersion>) -> List<IdeVersion>): List<ResourceCacheEntry<IdeDescriptor>> {
    //Register fake locks that prevents IDEs from deletion
    //while requesting the resource cache entries.
    val ides = arrayListOf<Pair<IdeVersion, FileLock>>()
    try {
      ideFilesBank.lockAndAccess {
        val ideVersions = selector(ideFilesBank.getAvailableIdeVersions())
        for (ideVersion in ideVersions) {
          val fileLock = ideFilesBank.getIdeFileLock(ideVersion)!!
          ides.add(ideVersion to fileLock)
        }
      }
    } catch (e: Throwable) {
      ides.forEach { it.second.release() }
      throw e
    }

    val result = arrayListOf<ResourceCacheEntry<IdeDescriptor>>()
    try {
      for ((ideVersion, _) in ides) {
        val ideDescriptor = getIdeDescriptor(ideVersion)
        result.add(ideDescriptor)
      }
    } catch (e: Throwable) {
      result.forEach { it.close() }
      throw e
    } finally {
      ides.forEach {
        it.second.release()
      }
    }
    return result
  }

  private fun getIdeDescriptor(ideVersion: IdeVersion): ResourceCacheEntry<IdeDescriptor> {
    val resourceCacheEntryResult = resourceCache.getResourceCacheEntry(ideVersion)
    return with(resourceCacheEntryResult) {
      when (this) {
        is ResourceCacheEntryResult.Found -> resourceCacheEntry
        is ResourceCacheEntryResult.Failed -> throw IllegalStateException(message, error)
        is ResourceCacheEntryResult.NotFound -> throw IllegalStateException(message)
      }
    }
  }

  private class IdeDescriptorResourceProvider(private val ideFilesBank: IdeFilesBank) : ResourceProvider<IdeVersion, IdeDescriptor> {

    override fun provide(key: IdeVersion): ProvideResult<IdeDescriptor> {
      val ideLock = ideFilesBank.getIdeFileLock(key)
          ?: return ProvideResult.NotFound("IDE $key is not found in the $ideFilesBank")
      val ideDescriptor = try {
        IdeCreator.createByFile(ideLock.file, null)
      } catch (e: Exception) {
        return ProvideResult.Failed("Unable to open IDE $key", e)
      }
      return ProvideResult.Provided(ideDescriptor)
    }
  }

  override fun close() = resourceCache.close()
}
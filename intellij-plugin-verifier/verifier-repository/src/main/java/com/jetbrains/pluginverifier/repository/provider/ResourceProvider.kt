package com.jetbrains.pluginverifier.repository.provider

/**
 * Provider of the resource by its key.
 */
interface ResourceProvider<in K, out R> {

  /**
   * Provides a resource by [key].
   *
   * @throws InterruptedException if the current thread has been
   * interrupted while providing the resource.
   */
  @Throws(InterruptedException::class)
  fun provide(key: K): ProvideResult<R>

}
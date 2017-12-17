package com.jetbrains.pluginverifier.repository.provider

/**
 * Provider of the resource by its key.
 */
@FunctionalInterface
interface ResourceProvider<in K, out R> {

  /**
   * Provides a resource by [key].
   */
  fun provide(key: K): ProvideResult<R>

}
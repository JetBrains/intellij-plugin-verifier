package com.jetbrains.pluginverifier.repository.provider

/**
 * [ResourceProvider] that doesn't provide any resources.
 */
class EmptyResourceProvider<in K, out R> : ResourceProvider<K, R> {
  override fun provide(key: K) = ProvideResult.NotFound<R>("The resource $key is not found")
}
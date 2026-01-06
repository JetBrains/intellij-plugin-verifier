/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.provider

/**
 * [ResourceProvider] that doesn't provide any resources.
 */
class EmptyResourceProvider<in K : Any, out R : Any> : ResourceProvider<K, R> {
  override fun provide(key: K) = ProvideResult.NotFound<R>("The resource $key is not found")
}
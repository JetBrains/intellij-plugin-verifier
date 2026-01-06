/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.provider

/**
 * Provider of the resource by its key.
 */
interface ResourceProvider<in K : Any, out R : Any> {

  /**
   * Provides a resource by [key].
   *
   * @throws InterruptedException if the current thread has been
   * interrupted while providing the resource.
   */
  @Throws(InterruptedException::class)
  fun provide(key: K): ProvideResult<R>

}
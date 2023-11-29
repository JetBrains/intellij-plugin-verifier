package com.jetbrains.pluginverifier.repository.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

fun <T> memoize(expirationInMinutes: Long = 5, delegateSupplier: () -> T): Supplier<T> = Caffeine.newBuilder()
  .expireAfterWrite(expirationInMinutes, TimeUnit.MINUTES)
  .build<Unit, T> {
    delegateSupplier()
  }
  .let { cache ->
    Supplier { cache.get(Unit) }
  }
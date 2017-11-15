package com.jetbrains.pluginverifier.repository.files

interface FileRepository<K> {

  fun get(key: K): FileRepositoryResult

  fun remove(key: K): Boolean

  fun has(key: K): Boolean

  fun sweep()
}
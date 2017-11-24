package com.jetbrains.pluginverifier.repository.files

import java.nio.file.Path

interface FileRepository<K> {

  fun get(key: K): FileRepositoryResult

  fun add(key: K, file: Path): Boolean

  fun remove(key: K): Boolean

  fun has(key: K): Boolean

  fun getAllExistingKeys(): Set<K>

  fun <R> lockAndAccess(block: () -> R): R

  fun sweep()
}
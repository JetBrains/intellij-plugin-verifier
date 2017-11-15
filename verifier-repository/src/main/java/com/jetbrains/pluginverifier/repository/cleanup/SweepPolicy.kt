package com.jetbrains.pluginverifier.repository.cleanup

interface SweepPolicy<K> {
  fun selectKeysForDeletion(sweepInfo: SweepInfo<K>): List<K>
}
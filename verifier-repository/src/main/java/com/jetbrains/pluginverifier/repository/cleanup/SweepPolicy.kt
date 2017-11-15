package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.files.AvailableFile

interface SweepPolicy<K> {
  fun selectFilesForDeletion(sweepInfo: SweepInfo<K>): List<AvailableFile<K>>
}
package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.time.Instant

data class DownloadEvent(val startInstant: Instant, val endInstant: Instant, val amount: SpaceAmount)
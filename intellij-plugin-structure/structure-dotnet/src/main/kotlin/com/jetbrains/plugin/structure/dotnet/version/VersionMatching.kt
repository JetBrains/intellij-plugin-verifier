package com.jetbrains.plugin.structure.dotnet.version

import com.jetbrains.plugin.structure.base.utils.Version

object VersionMatching {
  private const val LAST_OLD_WAVE_VERSION = 12
  private const val FIRST_NEW_WAVE_VERSION = 182
  fun getResharperRangeFromWaveRangeString(waveRangeString: String): ReSharperRange {
    val waveRange = getWaveRangeFromString(waveRangeString)
    return getReSharperRangeFromWaveRange(waveRange)
  }

  fun getReSharperRangeFromString(originalReSharperRange: String): ReSharperRange {
    return getRangeFromSting(
      originalReSharperRange,
      { versionString -> ReSharperVersion.fromString(versionString) },
      { min, isMinIncluded, max, isMaxIncluded -> ReSharperRange(min, isMinIncluded, max, isMaxIncluded) }
    ) as ReSharperRange
  }

  fun getReSharperVersionFromWave(wave: WaveVersion): ReSharperVersion {
    val (baseline, build) =
      if (wave.firstComponent <= LAST_OLD_WAVE_VERSION) {
        convertOldWaveToReSharperComponents(wave.firstComponent)
      } else {
        require(wave.firstComponent >= FIRST_NEW_WAVE_VERSION) { "The first wave version component should be in (1,..,9,11,12) or equal or greater than 182" }
        Pair(2000 + wave.firstComponent / 10, wave.firstComponent % 10)
      }
    return if (wave.secondComponent != 0) ReSharperVersion(listOf(baseline, build, wave.secondComponent)) else ReSharperVersion(listOf(baseline, build))
  }

  private fun getWaveRangeFromString(originalWaveRange: String): WaveRange {
    return getRangeFromSting(
      originalWaveRange,
      { versionString -> WaveVersion.fromString(versionString) },
      { min, isMinIncluded, max, isMaxIncluded -> WaveRange(min, isMinIncluded, max, isMaxIncluded) }
    ) as WaveRange
  }

  private fun getReSharperRangeFromWaveRange(range: WaveRange): ReSharperRange {
    val waveRange = replaceWrongWaveRangeIfNeeded(range)
    val min = if (waveRange.min != null && waveRange.min != WaveVersion(0, 0)) getReSharperVersionFromWave(waveRange.min) else null
    val max = if (waveRange.max != null) getReSharperVersionFromWave(waveRange.max) else null
    return ReSharperRange(min, waveRange.isMinIncluded, max, waveRange.isMaxIncluded)
  }

  // The wave versions 10 and 13-181 don't exist, but developers may restrict the compatibility by these versions, we should replace them with existing ones
  // https://www.jetbrains.com/help/resharper/sdk/PlatformVersioning.html#versioning-the-resharper-platform-waves
  private fun replaceWrongWaveRangeIfNeeded(range: WaveRange): WaveRange {
    val min = replaceWrongWaveVersionWithNextVersionIfNeeded(range.min)
    val max = replaceWrongWaveVersionWithNextVersionIfNeeded(range.max)

    if (min == null && max == null) {
      return range
    }

    // if min was changed we should include it, if max was changed we should exclude it, since we take a greater version
    val (newMin, isMinIncluded) = if (min != null) min to true else range.min to range.isMinIncluded
    val (newMax, isMaxIncluded) = if (max != null) max to false else range.max to range.isMaxIncluded
    return WaveRange(newMin, isMinIncluded, newMax, isMaxIncluded)
  }

  private fun replaceWrongWaveVersionWithNextVersionIfNeeded(version: WaveVersion?): WaveVersion? =
    when (version?.firstComponent) {
      10 -> WaveVersion(11, 0)
      in (LAST_OLD_WAVE_VERSION + 1) until FIRST_NEW_WAVE_VERSION -> WaveVersion(FIRST_NEW_WAVE_VERSION, 0)
      else -> null
    }

  private fun <V : Version<V>> getRangeFromSting(
    originalRange: String,
    getVersionFromString: (versionString: String) -> V,
    createRange: (min: V?, isMinIncluded: Boolean, max: V?, isMaxIncluded: Boolean) -> VersionRange<V>
  ): VersionRange<V> {
    var range = originalRange.filterNot { it.isWhitespace() }
    require(range.isNotBlank()) { "Original wave range string is blank" }
    val minVersion: V?
    val maxVersion: V?
    val isMinIncluded: Boolean
    val isMaxIncluded: Boolean
    if (range[0] == '(' || range[0] == '[') {
      isMinIncluded = when (range[0]) {
        '[' -> true
        else -> false
      }
      // The last character must be ] ot ) if the first is [ or (
      isMaxIncluded = when (range[range.length - 1]) {
        ')' -> false
        ']' -> true
        else -> throw IllegalArgumentException(") or ] was expected")
      }
      // removing parentheses
      range = range.substring(1, range.length - 1)
      // split lower and upper bounds by comma
      val parts = range.split(',')
      require(parts.size <= 2) { "There shouldn't be more than 2 values in range" }
      if (parts.size == 1 && (!isMinIncluded || !isMaxIncluded)) {
        throw IllegalArgumentException("The formats (1.0.0], [1.0.0) and (1.0.0) are invalid")
      }
      if (parts.size == 2 && parts[0].isBlank() && parts[1].isBlank()) {
        throw IllegalArgumentException("Neither of upper nor lower bounds were specified")
      }
      minVersion = if (parts[0].isBlank()) {
        null
      } else {
        getVersionFromString(parts[0])
      }
      maxVersion = if (parts.size == 2) {
        if (parts[1].isBlank()) {
          null
        } else {
          getVersionFromString(parts[1])
        }
      } else {
        minVersion
      }
    } else {
      isMinIncluded = true
      minVersion = getVersionFromString(range)
      maxVersion = null
      isMaxIncluded = false
    }
    if (minVersion != null && maxVersion != null) {
      if (minVersion > maxVersion) {
        throw IllegalArgumentException("maxVersion should be greater than minVersion")
      }
      if (minVersion == maxVersion && !isMinIncluded && !isMaxIncluded) {
        throw IllegalArgumentException("Wrong format. (1.0.0, 1.0.0], [1.0.0, 1.0.0) and (1.0.0, 1.0.0) are invalid")
      }
    }

    return createRange(minVersion, isMinIncluded, maxVersion, isMaxIncluded)
  }

  private fun convertOldWaveToReSharperComponents(wave: Int): Pair<Int, Int> {
    return when (wave) {
      1 -> Pair(9, 0)
      2 -> Pair(9, 1)
      3 -> Pair(9, 2)
      4 -> Pair(10, 0)
      5 -> Pair(2016, 1)
      6 -> Pair(2016, 2)
      7 -> Pair(2016, 3)
      8 -> Pair(2017, 1)
      9 -> Pair(2017, 2)
      11 -> Pair(2017, 3)
      12 -> Pair(2018, 1)
      else -> throw IllegalArgumentException("The first wave version component should be in (1,..,9,11,12)")
    }
  }
}
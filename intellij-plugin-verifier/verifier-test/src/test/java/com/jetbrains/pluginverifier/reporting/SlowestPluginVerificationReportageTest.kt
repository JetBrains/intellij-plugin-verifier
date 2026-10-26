package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFIED_CLASSES_COUNT
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.tasks.profiling.SlowestPluginVerificationReportage
import com.jetbrains.pluginverifier.tests.mocks.TelemetryVerificationReportage
import com.jetbrains.pluginverifier.tests.mocks.createMockPluginInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class SlowestPluginVerificationReportageTest {

  private val fast = createMockPluginInfo("com.example.fast", "1.0")
  private val slow = createMockPluginInfo("com.example.slow", "2.0")

  @Test
  fun `keeps the plugin with the longest verification time`() {
    val reportage = SlowestPluginVerificationReportage(TelemetryVerificationReportage())
    reportage.reportTelemetry(fast, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(3)))
    reportage.reportTelemetry(slow, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(115)))
    reportage.reportTelemetry(fast, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(30)))

    val slowest = reportage.slowestVerification!!
    assertEquals(slow, slowest.plugin)
    assertEquals(Duration.ofSeconds(115), slowest.duration)
  }

  @Test
  fun `ignores telemetry without verification time and still delegates it`() {
    val delegate = TelemetryVerificationReportage()
    val reportage = SlowestPluginVerificationReportage(delegate)
    reportage.reportTelemetry(fast, PluginTelemetry(PLUGIN_VERIFIED_CLASSES_COUNT to 42))

    assertNull(reportage.slowestVerification)
    assertEquals(42, delegate[fast]!![PLUGIN_VERIFIED_CLASSES_COUNT])
  }

  @Test
  fun `keeps the longest verification time of each tracked plugin`() {
    val untracked = createMockPluginInfo("com.example.untracked", "1.0")
    val newerSlow = createMockPluginInfo("com.example.slow", "3.0")
    val reportage = SlowestPluginVerificationReportage(TelemetryVerificationReportage(), setOf(fast.pluginId, slow.pluginId))
    reportage.reportTelemetry(fast, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(3)))
    reportage.reportTelemetry(slow, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(56)))
    reportage.reportTelemetry(newerSlow, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(20)))
    reportage.reportTelemetry(untracked, PluginTelemetry(PLUGIN_VERIFICATION_TIME to Duration.ofSeconds(115)))

    val tracked = reportage.trackedVerifications
    assertEquals(setOf(fast.pluginId, slow.pluginId), tracked.keys)
    assertEquals(Duration.ofSeconds(3), tracked.getValue(fast.pluginId).duration)
    assertEquals(slow, tracked.getValue(slow.pluginId).plugin)
    assertEquals(Duration.ofSeconds(56), tracked.getValue(slow.pluginId).duration)
    assertEquals(untracked, reportage.slowestVerification!!.plugin)
  }
}

package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.tasks.profiling.measurePluginVerification
import com.jetbrains.pluginverifier.tests.mocks.MockPluginVerificationReportage
import com.jetbrains.pluginverifier.tests.mocks.MockTaskResult
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DurationMeasuringPluginVerificationTest {

  @Test
  fun `run duration on function`() {
    val sleepDuration = 5L
    measurePluginVerification {
      MockPluginVerificationReportage().run {
        TimeUnit.MILLISECONDS.sleep(sleepDuration)
        MockTaskResult()
      }
    }.run {
      assertTrue(this.duration.nano > sleepDuration)
    }
  }
}
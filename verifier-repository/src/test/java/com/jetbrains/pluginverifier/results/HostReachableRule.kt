package com.jetbrains.pluginverifier.results

import com.jetbrains.pluginverifier.results.HostReachableRule.HostReachable
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.URL

/**
 * Allows to run a test only if the [specified] [HostReachable.value] host is available
 */
class HostReachableRule : TestRule {

  @Retention(AnnotationRetention.RUNTIME)
  @Target(
      AnnotationTarget.FUNCTION,
      AnnotationTarget.PROPERTY_GETTER,
      AnnotationTarget.PROPERTY_SETTER,
      AnnotationTarget.CLASS,
      AnnotationTarget.FILE
  )
  annotation class HostReachable(val value: String)

  override fun apply(statement: Statement, description: Description): Statement {
    val hostReachable = description.getAnnotation(HostReachable::class.java)

    val host = hostReachable.value
    if (hostReachable != null && !checkHost(host)) {
      return SkipStatement(host)
    }
    return statement
  }

  private class SkipStatement(private val host: String) : Statement() {

    @Throws(Throwable::class)
    override fun evaluate() {
      Assume.assumeTrue(messageHost(host), false)
    }
  }

  private fun checkHost(host: String): Boolean = try {
    val connection = URL(host).openConnection()
    connection.connect()
    connection.getInputStream().close()
    true
  } catch (e: Exception) {
    System.err.println(messageHost(host))
    e.printStackTrace()
    false
  }

  companion object {
    private fun messageHost(host: String) = "Skipped, because the following host is not available at the moment: $host"
  }

}
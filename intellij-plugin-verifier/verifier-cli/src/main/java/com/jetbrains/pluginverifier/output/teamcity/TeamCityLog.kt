/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.teamcity

import java.io.Closeable
import java.io.PrintStream
import java.io.PrintWriter

class TeamCityLog(private val out: PrintWriter) {

  constructor(printStream: PrintStream) : this(PrintWriter(printStream, true))

  fun messageError(text: String) {
    out.printf("##teamcity[message text='%s' status='ERROR']\n", escape(text))
  }

  fun messageError(text: String, errorDetails: String) {
    out.printf("##teamcity[message text='%s' errorDetails='%s' status='ERROR']\n", escape(text), escape(errorDetails))
  }

  fun message(text: String) {
    out.printf("##teamcity[message text='%s']\n", escape(text))
  }

  fun messageWarn(text: String) {
    out.printf("##teamcity[message text='%s' status='WARNING']\n", escape(text))
  }

  fun buildProblem(description: String) {
    out.printf("##teamcity[buildProblem description='%s']\n", escape(description))
  }

  fun buildProblem(description: String, identity: String) {
    out.printf("##teamcity[buildProblem description='%s' identity='%s']\n", escape(description), escape(identity))
  }

  fun buildStatus(text: String) {
    out.printf("##teamcity[buildStatus text='%s']\n", escape(text))
  }

  fun buildStatusSuccess(text: String) {
    out.printf("##teamcity[buildStatus status='SUCCESS' text='%s']\n", escape(text))
  }

  fun buildStatusFailure(text: String) {
    out.printf("##teamcity[buildStatus status='FAILURE' text='%s']\n", escape(text))
  }


  fun testIgnored(testName: String, message: String) {
    out.printf("##teamcity[testIgnored name='%s' message='%s']\n", escape(testName), escape(message))
  }

  fun testStdOut(testName: String, outText: String) {
    out.printf("##teamcity[testStdOut name='%s' out='%s']\n", escape(testName), escape(outText))
  }

  fun testStdErr(testName: String, errText: String) {
    out.printf("##teamcity[testStdErr name='%s' out='%s']\n", escape(testName), escape(errText))
  }

  fun testFailed(name: String, message: String, details: String) {
    out.printf("##teamcity[testFailed name='%s' message='%s' details='%s']\n", escape(name), escape(message), escape(details))
  }

  fun blockOpen(name: String): Block {
    out.printf("##teamcity[blockOpened name='%s']\n", escape(name))
    return Block(name)
  }

  fun testSuiteStarted(suiteName: String): TestSuite {
    out.printf("##teamcity[testSuiteStarted name='%s']\n", escape(suiteName))
    return TestSuite(suiteName)
  }

  fun testStarted(testName: String): Test {
    out.printf("##teamcity[testStarted name='%s']\n", escape(testName))
    return Test(testName)
  }

  fun buildStatisticValue(key: String, value: Number) {
    out.printf("##teamcity[buildStatisticValue key='${escape(key)}' value='$value']\n")
  }

  inner class Test(private val testName: String) : Closeable {

    override fun close() {
      out.printf("##teamcity[testFinished name='%s']\n", escape(testName))
    }
  }

  inner class TestSuite(private val suiteName: String) : Closeable {

    override fun close() {
      out.printf("##teamcity[testSuiteFinished name='%s']\n", escape(suiteName))
    }
  }

  inner class Block(private val name: String) : Closeable {

    override fun close() {
      out.printf("##teamcity[blockClosed name='%s']\n", escape(name))
    }
  }

  private fun escape(s: String): String {
    return s.replace("[\\|'\\[\\]]".toRegex(), "\\|$0").replace("\n".toRegex(), "|n").replace("\r".toRegex(), "|r")
  }
}

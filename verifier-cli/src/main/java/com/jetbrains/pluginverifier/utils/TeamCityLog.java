package com.jetbrains.pluginverifier.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

/**
 * @author Sergey Evdokimov
 */
public class TeamCityLog {

  public static final TeamCityLog NULL_LOG = new TeamCityLog(new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM));

  private final PrintStream out;

  public TeamCityLog(PrintStream out) {
    this.out = out;
  }

  private static String escape(@NotNull String s) {
    return s.replaceAll("[\\|'\\[\\]]", "\\|$0").
      replaceAll("\n", "|n").
      replaceAll("\r", "|r");
  }

  public static TeamCityLog getInstance(@NotNull CommandLine commandLine) {
    return commandLine.hasOption("tc") ? new TeamCityLog(System.out) : TeamCityLog.NULL_LOG;
  }

  public void messageError(@NotNull String text) {
    out.printf("##teamcity[message text='%s' status='ERROR']\n", escape(text));
  }

  public void messageError(@NotNull String text, @NotNull String errorDetails) {
    out.printf("##teamcity[message text='%s' errorDetails='%s' status='ERROR']\n", escape(text), escape(errorDetails));
  }

  public void message(@NotNull String text) {
    out.printf("##teamcity[message text='%s']\n", escape(text));
  }

  public void messageWarn(@NotNull String text) {
    out.printf("##teamcity[message text='%s' status='WARNING']\n", escape(text));
  }

  public void buildProblem(@NotNull String description) {
    out.printf("##teamcity[buildProblem description='%s']\n", escape(description));
  }

  public void buildProblem(@NotNull String description, @NotNull String identity) {
    out.printf("##teamcity[buildProblem description='%s' identity='%s']\n", escape(description), escape(identity));
  }

  public void buildStatus(@NotNull String text) {
    out.printf("##teamcity[buildStatus text='%s']\n", escape(text));
  }

  public void buildStatusSuccess(@NotNull String text) {
    out.printf("##teamcity[buildStatus status='SUCCESS' text='%s']\n", escape(text));
  }

  public void testIgnored(@NotNull String testName, @NotNull String message) {
    out.printf("##teamcity[testIgnored name='%s' message='%s']\n", escape(testName), escape(message));
  }

  public void testStdOut(@NotNull String className, @NotNull String outText) {
    out.printf("##teamcity[testStdOut name='%s' out='%s']\n", escape(className), escape(outText));
  }

  public void testStdErr(@NotNull String className, @NotNull String errText) {
    out.printf("##teamcity[testStdErr name='%s' out='%s']\n", escape(className), escape(errText));
  }

  public void testFailed(@NotNull String name, @NotNull String message, @NotNull String details) {
    out.printf("##teamcity[testFailed name='%s' message='%s' details='%s']\n", escape(name), escape(message), escape(details));
  }

  public Block blockOpen(@NotNull String name) {
    out.printf("##teamcity[blockOpened name='%s']\n", escape(name));
    return new Block(name);
  }

  public TestSuite testSuiteStarted(@NotNull String suiteName) {
    out.printf("##teamcity[testSuiteStarted name='%s']\n", escape(suiteName));
    return new TestSuite(suiteName);
  }

  public Test testStarted(@NotNull String testName) {
    out.printf("##teamcity[testStarted name='%s']\n", escape(testName));
    return new Test(testName);
  }

  public class Test {
    private String testName;

    public Test(String testName) {
      this.testName = testName;
    }

    public void close() {
      out.printf("##teamcity[testFinished name='%s']\n", escape(testName));
    }
  }

  public class TestSuite {
    private String suiteName;

    public TestSuite(String suiteName) {
      this.suiteName = suiteName;
    }

    public void close() {
      out.printf("##teamcity[testSuiteFinished name='%s']\n", escape(suiteName));
    }
  }

  public class Block {
    private String name;

    public Block(String name) {
      this.name = name;
    }

    public void close() {
      out.printf("##teamcity[blockClosed name='%s']\n", escape(name));
    }
  }
}

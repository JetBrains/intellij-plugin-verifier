package com.jetbrains.pluginverifier.utils;

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

  public void buildStatus(@NotNull String text) {
    out.printf("##teamcity[buildStatus text='%s']\n", escape(text));
  }

  public Block blockOpen(@NotNull String name) {
    out.printf("##teamcity[blockOpened name='%s']\n", escape(name));
    return new Block(name);
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

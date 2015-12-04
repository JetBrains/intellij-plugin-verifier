package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

import java.util.Arrays;
import java.util.List;

public class PluginVerifierMain {

  public static void main(String[] args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args);
    }
    catch (ParseException e) {
      throw FailUtil.fail(e);
    }

    if (commandLine.hasOption('h')) {
      Util.printHelp();
      return;
    }

    List<String> freeArgs = Arrays.asList(commandLine.getArgs());

    VerifierCommand command;
    if (freeArgs.isEmpty()) {
      command = CommandHolder.getDefaultCommand();
    }
    else {
      command = CommandHolder.getCommand(freeArgs.get(0));
      if (command == null) {
        command = CommandHolder.getDefaultCommand();
      }
      else {
        freeArgs = freeArgs.subList(1, freeArgs.size());
      }
    }

    int exitCode = command.execute(commandLine, freeArgs);

    if (exitCode != 0 && !Boolean.getBoolean("exitCode0")) {
      System.exit(exitCode);
    }
  }

}

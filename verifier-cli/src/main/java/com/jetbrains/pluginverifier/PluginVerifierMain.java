package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.util.Util;
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
      throw Util.fail(e.getLocalizedMessage());
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

    command.execute(commandLine, freeArgs);
  }

}

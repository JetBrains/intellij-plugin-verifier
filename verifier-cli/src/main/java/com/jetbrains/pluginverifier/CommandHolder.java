package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.commands.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CommandHolder {

  private static final VerifierCommand defaultCommand = new CheckPluginCommand();

  private static final Map<String, VerifierCommand> COMMAND_MAP = new HashMap<String, VerifierCommand>();

  static {
    for (VerifierCommand c : new VerifierCommand[]{new CheckIdeCommand(), new CompareResultsCommand(), new NewProblemsCommand(), new ProblemsPrinter()}) {
      COMMAND_MAP.put(c.getName(), c);
    }

    COMMAND_MAP.put(defaultCommand.getName(), defaultCommand);
  }

  @Nullable
  public static VerifierCommand getCommand(@NotNull String commandName) {
    return COMMAND_MAP.get(commandName);
  }

  public static Map<String, VerifierCommand> getCommandMap() {
    return COMMAND_MAP;
  }

  public static VerifierCommand getDefaultCommand() {
    return defaultCommand;
  }
}

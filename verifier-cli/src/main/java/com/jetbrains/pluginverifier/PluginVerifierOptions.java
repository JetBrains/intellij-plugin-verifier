package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PluginVerifierOptions {
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;
  private final String[] externalClassPrefixes;
  private final Set<String> myOptionalDependenciesIdsToIgnoreIfMissing;

  private PluginVerifierOptions(String[] prefixesToSkipForDuplicateClassesCheck, String[] externalClassPrefixes, String[] optionalDependenciesIdsToIgnoreIfMissing) {
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
    this.externalClassPrefixes = externalClassPrefixes;
    myOptionalDependenciesIdsToIgnoreIfMissing = new HashSet<String>(Arrays.asList(optionalDependenciesIdsToIgnoreIfMissing));
  }

  @Nullable
  private static String getOption(CommandLine commandLine, String shortKey) {
    Option option = Util.CMD_OPTIONS.getOption(shortKey);

    String cmdValue = commandLine.getOptionValue(shortKey);
    if (cmdValue != null) return cmdValue;

    return RepositoryConfiguration.getInstance().getProperty(option.getLongOpt());
  }

  @NotNull
  private static List<String> getOptionValues(CommandLine commandLine, String shortKey) {
    List<String> res = new ArrayList<String>();

    String[] cmdValues = commandLine.getOptionValues(shortKey);
    if (cmdValues != null) {
      Collections.addAll(res, cmdValues);
    }

    Option option = Util.CMD_OPTIONS.getOption(shortKey);
    String cfgProperty = RepositoryConfiguration.getInstance().getProperty(option.getLongOpt());

    if (cfgProperty != null) {
      res.add(cfgProperty);
    }

    return res;
  }

  @NotNull
  private static String[] getOptionValuesSplit(CommandLine commandLine, String splitter, String shortKey) {
    List<String> res = new ArrayList<String>();
    for (String optionStr : getOptionValues(commandLine, shortKey)) {
      if (optionStr.isEmpty()) continue;

      Collections.addAll(res, optionStr.split(splitter));
    }

    return res.toArray(new String[res.size()]);
  }

  @NotNull
  public static PluginVerifierOptions parseOpts(@NotNull CommandLine commandLine) {
    String[] prefixesToSkipForDuplicateClassesCheck = getOptionValuesSplit(commandLine, ":", "s");
    for (int i = 0; i < prefixesToSkipForDuplicateClassesCheck.length; i++) {
      prefixesToSkipForDuplicateClassesCheck[i] = prefixesToSkipForDuplicateClassesCheck[i].replace('.', '/');
    }

    String[] externalClasses = getOptionValuesSplit(commandLine, ":", "e");
    for (int i = 0; i < externalClasses.length; i++) {
      externalClasses[i] = externalClasses[i].replace('.', '/');
    }
    String[] optionalDependenciesIdsToIgnoreIfMissing = getOptionValuesSplit(commandLine, ",", "imod");

    return new PluginVerifierOptions(prefixesToSkipForDuplicateClassesCheck, externalClasses, optionalDependenciesIdsToIgnoreIfMissing);
  }

  public boolean isIgnoreMissingOptionalDependency(@NotNull String pluginId) {
    return myOptionalDependenciesIdsToIgnoreIfMissing.contains(pluginId);
  }

  @NotNull
  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }

  @NotNull
  public String[] getExternalClassPrefixes() {
    return externalClassPrefixes;
  }

  public boolean isExternalClass(@NotNull String className) {
    for (String prefix : externalClassPrefixes) {
      if (prefix != null && prefix.length() > 0 && className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}

package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.utils.Configuration;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginVerifierOptions {
  private String[] myPrefixesToSkipForDuplicateClassesCheck = new String[]{"com/intellij/uiDesigner/core"};

  private String[] externalClassPrefixes = new String[0];

  @Nullable
  private static String getOption(CommandLine commandLine, String shortKey) {
    Option option = Util.CMD_OPTIONS.getOption(shortKey);

    String cmdValue = commandLine.getOptionValue(shortKey);
    if (cmdValue != null) return cmdValue;

    return Configuration.getInstance().getProperty(option.getLongOpt());
  }

  @NotNull
  public static PluginVerifierOptions parseOpts(CommandLine commandLine) {
    PluginVerifierOptions res = new PluginVerifierOptions();

    final String prefixes = getOption(commandLine, "s");
    if (prefixes != null && prefixes.length() > 0) {
      res.setPrefixesToSkipForDuplicateClassesCheck(prefixes.replace('.', '/').split(":"));
    }

    final String e = getOption(commandLine, "e");
    if (e != null && e.length() > 0) {
      res.setExternalClassPrefixes(e.replace('.', '/').split(":"));
    }

    return res;
  }

  @NotNull
  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }

  public void setPrefixesToSkipForDuplicateClassesCheck(@NotNull String[] prefixesToSkipForDuplicateClassesCheck) {
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
  }

  @NotNull
  public String[] getExternalClassPrefixes() {
    return externalClassPrefixes;
  }

  public void setExternalClassPrefixes(@NotNull String[] externalClassPrefixes) {
    this.externalClassPrefixes = externalClassPrefixes;
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

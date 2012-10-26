package com.jetbrains.pluginverifier;

import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

public class PluginVerifierOptions {
  private String[] myPrefixesToSkipForDuplicateClassesCheck = new String[]{"com/intellij/uiDesigner/core"};

  @NotNull
  public static PluginVerifierOptions parseOpts(CommandLine commandLine) {
    PluginVerifierOptions res = new PluginVerifierOptions();

    final String prefixes = commandLine.getOptionValue('s');
    if (prefixes != null && prefixes.length() > 0) {
      res.setPrefixesToSkipForDuplicateClassesCheck(prefixes.replace('.', '/').split(":"));
    }

    return res;
  }


  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }

  public void setPrefixesToSkipForDuplicateClassesCheck(String[] prefixesToSkipForDuplicateClassesCheck) {
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
  }
}

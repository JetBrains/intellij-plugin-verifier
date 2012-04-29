package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.verifiers.DuplicateClassesVerifier;
import com.jetbrains.pluginverifier.verifiers.ReferencesVerifier;

public class PluginVerifierMain {
  public static void main(String[] args) throws Exception {
    long start = System.currentTimeMillis();

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(args);
    if (options == null) {
      PluginVerifierOptions.printHelp();
      System.exit(1);
    }

    final IdeaPlugin[] contexts = options.getContexts();

    System.out.println("Reading directories took " + (System.currentTimeMillis() - start) + "ms");
    start = System.currentTimeMillis();

    final boolean[] failed = {false};
    for (IdeaPlugin plugin : contexts) {
      System.out.println("Verifying " + plugin.getId() + " against " + plugin.getIdea().getMoniker());

      ErrorRegister errorRegister = new ErrorRegister() {
        @Override
        public void registerError(final String className, final String error) {
          printError(className.replace('/', '.') + ": " + error);
          failed[0] = true;
        }
      };

      new ReferencesVerifier(plugin, errorRegister).verify();
      new DuplicateClassesVerifier(plugin, errorRegister, options.getPrefixesToSkipForDuplicateClassesCheck()).verify();
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    System.out.println(failed[0] ? "FAILED" : "OK");
    System.exit(failed[0] ? 1 : 0);
  }

  private static void printError(String errorMessage) {
    if (System.getenv("TEAMCITY_VERSION") != null)
      System.err.println("##teamcity[message text='" + errorMessage + "' status='ERROR']");
    else
      System.err.println("  ERROR  " + errorMessage);
  }
}

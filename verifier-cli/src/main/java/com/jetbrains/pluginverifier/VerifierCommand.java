package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.utils.FailUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author Sergey Evdokimov
 */
public abstract class VerifierCommand {

  private final String name;

  public VerifierCommand(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * @return exit code
   */
  public abstract int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception;

  @NotNull
  protected Jdk createJdk(@NotNull CommandLine commandLine) throws IOException {
    File runtimeDirectory;

    if (commandLine.hasOption('r')) {
      runtimeDirectory = new File(commandLine.getOptionValue('r'));
      if (!runtimeDirectory.isDirectory()) {
        throw FailUtil.fail("Specified runtime directory is not a directory: " + commandLine.getOptionValue('r'));
      }
    }
    else {
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome == null) {
        throw FailUtil.fail("JAVA_HOME is not specified");
      }

      runtimeDirectory = new File(javaHome);
      if (!runtimeDirectory.isDirectory()) {
        throw FailUtil.fail("Invalid JAVA_HOME: " + javaHome);
      }
    }

    return Jdk.createJdk(runtimeDirectory);
  }

  @Nullable
  protected Resolver getExternalClassPath(CommandLine commandLine) throws IOException {
    String[] values = commandLine.getOptionValues("cp");
    if (values == null) {
      return null;
    }

    List<Resolver> pools = new ArrayList<Resolver>(values.length);

    for (String value : values) {
      pools.add(Resolver.createJarResolver(new JarFile(value)));
    }

    return Resolver.createUnionResolver("External classpath resolver: " + Arrays.toString(values), pools);
  }

  @Nullable
  protected IdeVersion takeVersionFromCmd(@NotNull CommandLine commandLine) throws IOException {
    String build = commandLine.getOptionValue("iv");
    if (build != null && !build.isEmpty()) {
      try {
        return IdeVersion.createIdeVersion(build);
      } catch (IllegalArgumentException e) {
        throw FailUtil.fail("Incorrect update IDE-version has been specified " + build, e);
      }
    }
    return null;
  }

}

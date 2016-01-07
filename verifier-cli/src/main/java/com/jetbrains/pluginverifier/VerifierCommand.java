package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.domain.IdeRuntimeManager;
import com.intellij.structure.impl.pool.ContainerClassPool;
import com.intellij.structure.impl.pool.JarClassPool;
import com.intellij.structure.pool.ClassPool;
import com.jetbrains.pluginverifier.utils.FailUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
  protected IdeRuntime createJdk(@NotNull CommandLine commandLine) throws IOException {
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

    return IdeRuntimeManager.getInstance().createRuntime(runtimeDirectory);
  }

  @Nullable
  protected ClassPool getExternalClassPath(CommandLine commandLine) throws IOException {
    String[] values = commandLine.getOptionValues("cp");
    if (values == null) {
      return null;
    }

    List<ClassPool> pools = new ArrayList<ClassPool>(values.length);

    for (String value : values) {
      pools.add(new JarClassPool(new JarFile(value)));
    }

    return ContainerClassPool.getUnion("external_class_path", pools);
  }

  protected void updateIdeVersionFromCmd(@NotNull Ide ide, @NotNull CommandLine commandLine) throws IOException {
    String build = commandLine.getOptionValue("iv");
    if (build != null && !build.isEmpty()) {
      ide.updateVersion(build);
    }
  }

}

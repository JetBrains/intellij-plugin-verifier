package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final ProblemSet myProblemSet = new ProblemSet();
  private final Plugin myPlugin;
  private final PluginVerifierOptions myVerifierOptions;
  private final Ide myIde;
  private final Jdk myJdk;
  private final Resolver myExternalClassPath;

  public VerificationContextImpl(@NotNull Plugin plugin, @NotNull Ide ide, @NotNull Jdk jdk, @Nullable Resolver externalClassPath, @NotNull PluginVerifierOptions verifierOptions) {
    myPlugin = plugin;
    myVerifierOptions = verifierOptions;
    myIde = ide;
    myJdk = jdk;
    myExternalClassPath = externalClassPath;
  }

  public PluginVerifierOptions getVerifierOptions() {
    return myVerifierOptions;
  }

  @Override
  public void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    if (!myVerifierOptions.isIgnoredProblem(myPlugin, problem)) {
      myProblemSet.addProblem(problem, location);
    }
  }

  @Override
  public Plugin getPlugin() {
    return myPlugin;
  }

  @NotNull
  public ProblemSet getProblemSet() {
    return myProblemSet;
  }

  @NotNull
  public Ide getIde() {
    return myIde;
  }

  public Jdk getJdk() {
    return myJdk;
  }

  @Override
  public Resolver getExternalClassPath() {
    return myExternalClassPath;
  }
}

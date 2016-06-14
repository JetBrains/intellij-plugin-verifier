package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VOptions;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Evdokimov
 */
public class VerificationContextImpl implements VerificationContext {

  private final ProblemSet myProblemSet = new ProblemSet();
  private final Plugin myPlugin;
  private final VOptions myVerifierOptions;
  private final Ide myIde;
  private final Resolver myJdkResolver;
  private final Resolver myExternalClassPath;
  private final Resolver myIdeResolver;
  private String myOverview;

  public VerificationContextImpl(@NotNull Plugin plugin, @NotNull Ide ide, @NotNull Resolver ideResolver, @NotNull Resolver jdkResolver, @Nullable Resolver externalClassPath, @NotNull VOptions verifierOptions) {
    myPlugin = plugin;
    myIdeResolver = ideResolver;
    myVerifierOptions = verifierOptions;
    myIde = ide;
    myJdkResolver = jdkResolver;
    myExternalClassPath = externalClassPath;
  }

  @NotNull
  public VOptions getVerifierOptions() {
    return myVerifierOptions;
  }

  @Override
  public void registerProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    if (!myVerifierOptions.isIgnoredProblem(myPlugin, problem)) {
      myProblemSet.addProblem(problem, location);
    }
  }

  @Override
  @NotNull
  public String getOverview() {
    return StringUtil.notNullize(myOverview);
  }

  public void setOverview(String overview) {
    myOverview = overview;
  }

  @NotNull
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

  @NotNull
  @Override
  public Resolver getIdeResolver() {
    return myIdeResolver;
  }

  @NotNull
  @Override
  public Resolver getJdkResolver() {
    return myJdkResolver;
  }

  @Override
  public Resolver getExternalClassPath() {
    return myExternalClassPath;
  }
}

package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Jdk;
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
  private final PluginVerifierOptions myVerifierOptions;
  private final Ide myIde;
  private final Jdk myJdk;
  private final Resolver myExternalClassPath;

  public VerificationContextImpl(@NotNull PluginVerifierOptions verifierOptions,
                                 @NotNull Ide ide,
                                 @NotNull Jdk jdk,
                                 @Nullable Resolver externalClassPath) {
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
    myProblemSet.addProblem(problem, location);
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

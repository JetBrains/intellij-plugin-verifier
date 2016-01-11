package com.jetbrains.pluginverifier;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.pool.ClassPool;
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
  private final IdeRuntime myIdeRuntime;
  private final ClassPool myExternalClassPath;

  public VerificationContextImpl(@NotNull PluginVerifierOptions verifierOptions,
                                 @NotNull Ide ide,
                                 @NotNull IdeRuntime ideRuntime,
                                 @Nullable ClassPool externalClassPath) {
    myVerifierOptions = verifierOptions;
    myIde = ide;
    myIdeRuntime = ideRuntime;
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

  @Override
  public IdeRuntime getIdeRuntime() {
    return myIdeRuntime;
  }

  @Override
  public ClassPool getExternalClassPath() {
    return myExternalClassPath;
  }
}

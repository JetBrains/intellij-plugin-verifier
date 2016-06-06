package com.jetbrains.pluginverifier.reports;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public class DetailsImpl implements Details {

  private final IdeVersion myIdeVersion;
  private final UpdateInfo myUpdateInfo;
  private final Multimap<Problem, ProblemLocation> myProblems;
  private final String myOverview;

  public DetailsImpl(@NotNull IdeVersion ideVersion,
                     @NotNull UpdateInfo updateInfo,
                     @NotNull Multimap<Problem, ProblemLocation> problems,
                     @NotNull String overview) {
    myIdeVersion = ideVersion;
    myUpdateInfo = updateInfo;
    myProblems = problems;
    myOverview = overview;
  }

  @NotNull
  @Override
  public IdeVersion checkedIde() {
    return myIdeVersion;
  }

  @NotNull
  @Override
  public UpdateInfo checkedPlugin() {
    return UpdateInfo.copy(myUpdateInfo);
  }

  @NotNull
  @Override
  public Multimap<Problem, ProblemLocation> problems() {
    return Multimaps.unmodifiableMultimap(myProblems);
  }

  @NotNull
  @Override
  public String overview() {
    return myOverview;
  }

}

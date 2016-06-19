package com.jetbrains.pluginverifier.reports;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class DetailsBuilder {

  private final Multimap<Problem, ProblemLocation> myProblems = HashMultimap.create();
  private IdeVersion myIdeVersion;
  private UpdateInfo myUpdate;
  private String myOverview;

  public DetailsBuilder(@NotNull IdeVersion ideVersion, @NotNull UpdateInfo update, @Nullable String overview) {
    myIdeVersion = ideVersion;
    myUpdate = update;
    myOverview = overview == null ? "" : overview;
  }

  public DetailsBuilder addProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    myProblems.put(problem, location);
    return this;
  }

  public DetailsBuilder addProblems(@NotNull Problem problem, @NotNull Set<ProblemLocation> locations) {
    myProblems.putAll(problem, locations);
    return this;
  }


  public DetailsImpl build() {
    if (myIdeVersion == null) {
      throw new IllegalArgumentException("Ide version is not specified");
    }
    if (myUpdate == null) {
      throw new IllegalArgumentException("Update number is not specified");
    }
    if (myOverview == null) {
      myOverview = "";
    }
    return new DetailsImpl(myIdeVersion, myUpdate, myProblems, myOverview);
  }
}

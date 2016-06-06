package com.jetbrains.pluginverifier.reports;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class DetailsBuilder {

  private final Multimap<Problem, ProblemLocation> myProblems = HashMultimap.create();
  private IdeVersion myIdeVersion;
  private UpdateInfo myUpdate;
  private String myOverview;

  public DetailsBuilder setIdeVersion(@NotNull IdeVersion ideVersion) {
    if (myIdeVersion != null) {
      throw new IllegalArgumentException("Ide version is already set");
    }
    myIdeVersion = ideVersion;
    return this;
  }

  public DetailsBuilder setPlugin(@NotNull UpdateInfo update) {
    if (myUpdate != null) {
      throw new IllegalArgumentException("Update number is already set");
    }
    myUpdate = update;
    return this;
  }

  public DetailsBuilder setOverview(@NotNull String overview) {
    if (myOverview != null) {
      throw new IllegalArgumentException("Overview is already set");
    }
    myOverview = overview;
    return this;
  }

  public DetailsBuilder addProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    myProblems.put(problem, location);
    return this;
  }

  public DetailsBuilder addProblem(@NotNull Problem problem, @NotNull Set<ProblemLocation> locations) {
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

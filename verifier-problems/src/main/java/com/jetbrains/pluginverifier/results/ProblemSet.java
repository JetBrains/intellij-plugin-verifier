package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Describes problems of overlying plugin and
 * their precise locations inside a plugin
 *
 * @author Sergey Evdokimov
 */
public class ProblemSet {

  private Map<Problem, Set<ProblemLocation>> map = new HashMap<Problem, Set<ProblemLocation>>();

  public ProblemSet() {
  }

  public ProblemSet(Map<Problem, Set<ProblemLocation>> map) {
    this.map = map;
  }

  @NotNull
  public Map<Problem, Set<ProblemLocation>> asMap() {
    return map == null ? Collections.<Problem, Set<ProblemLocation>>emptyMap() : map;
  }

  public void appendProblems(@NotNull ProblemSet otherSet) {
    Map<Problem, Set<ProblemLocation>> map = otherSet.asMap();
    for (Map.Entry<Problem, Set<ProblemLocation>> entry : map.entrySet()) {
      for (ProblemLocation location : entry.getValue()) {
        addProblem(entry.getKey(), location);
      }
    }
  }

  public void addProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
    if (map == null) {
      map = new LinkedHashMap<Problem, Set<ProblemLocation>>();
    }

    Set<ProblemLocation> locations = map.get(problem);
    if (locations == null) {
      locations = new LinkedHashSet<ProblemLocation>();
      map.put(problem, locations);
    }

    locations.add(location);
  }

  public void printProblems(@NotNull PrintStream out, @Nullable String indent) {
    if (indent == null) {
      indent = "";
    }

    for (Map.Entry<Problem, Set<ProblemLocation>> entry : asMap().entrySet()) {
      out.print(indent);
      out.println(MessageUtils.cutCommonPackages(entry.getKey().getDescription()));

      out.printf("%s    at %d locations\n", indent, entry.getValue().size());

      for (ProblemLocation location : entry.getValue()) {
        out.print(indent);
        out.print("    ");
        out.println(MessageUtils.cutCommonPackages(location.toString()));
      }

      out.println();
    }
  }

  public void setMap(@NotNull Map<Problem, Set<ProblemLocation>> map) {
    this.map = map;
  }


  @NotNull
  public Set<Problem> getAllProblems() {
    return asMap().keySet();
  }

  @NotNull
  public Set<ProblemLocation> getLocations(@NotNull Problem problem) {
    return asMap().get(problem);
  }

  public boolean isEmpty() {
    return asMap().isEmpty();
  }

  public int count() {
    return asMap().size();
  }
}

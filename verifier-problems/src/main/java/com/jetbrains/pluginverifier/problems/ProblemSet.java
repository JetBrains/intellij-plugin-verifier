package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class ProblemSet {

  private Map<Problem, Set<ProblemLocation>> map = new LinkedHashMap<Problem, Set<ProblemLocation>>();

  public Map<Problem, Set<ProblemLocation>> asMap() {
    return map;
  }

  public void addProblem(@NotNull Problem problem, @NotNull ProblemLocation location) {
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

    for (Map.Entry<Problem, Set<ProblemLocation>> entry : map.entrySet()) {
      out.print(indent);
      out.println(entry.getKey().getDescription());

      out.printf("%s    at %d locations\n", indent, entry.getValue().size());

      for (ProblemLocation location : entry.getValue()) {
        out.print(indent);
        out.print("    ");
        out.println(location.toString());
      }
    }
  }

  public Set<Problem> getAllProblems() {
    return map.keySet();
  }

  public Set<ProblemLocation> getLocations(Problem problem) {
    return map.get(problem);
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public int count() {
    return map.size();
  }
}

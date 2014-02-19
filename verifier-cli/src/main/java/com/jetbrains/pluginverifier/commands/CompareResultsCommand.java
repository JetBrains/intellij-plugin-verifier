package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class CompareResultsCommand extends VerifierCommand {
  public CompareResultsCommand() {
    super("compare-results");
  }

  @Override
  public void execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.size() != 2) {
      throw Util.fail("You have to specify two result files. For example: \"java -jar verifier.jar compare-results res_133.200.xml res_133.201.xml\"");
    }

    File previousBuildProblems = new File(freeArgs.get(0));
    File currentBuildProblems = new File(freeArgs.get(1));

    Set<Problem> oldProblems = getOldProblems(previousBuildProblems);
    Map<UpdateInfo, Collection<Problem>> currentProblemMap = ProblemUtils.loadProblems(currentBuildProblems).asMap();

    Multimap<Problem, UpdateInfo> res = HashMultimap.create();

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : currentProblemMap.entrySet()) {
      UpdateInfo update = entry.getKey();
      Collection<Problem> updateProblems = entry.getValue();

      for (Problem problem : updateProblems) {
        if (!oldProblems.contains(problem)) {
          res.put(problem, update);
        }
      }
    }

    if (res.isEmpty()) {
      System.out.println("No problems!");
    }
    else {
      System.out.printf("Found %d new problems:\n", res.asMap().size());

      for (Map.Entry<Problem, Collection<UpdateInfo>> entry : res.asMap().entrySet()) {
        System.out.println(MessageUtils.cutCommonPackages(entry.getKey().getDescription()));

        Collection<UpdateInfo> updates = entry.getValue();

        System.out.printf("    at %d locations: %s\n", updates.size(), Joiner.on(", ").join(updates));

        System.out.println();
      }
    }


    System.out.println("Done");
  }

  private Set<Problem> getOldProblems(File previousResults) throws IOException {
    Map<UpdateInfo, Collection<Problem>> previousProblemMap = ProblemUtils.loadProblems(previousResults).asMap();

    Set<Problem> oldProblems = Sets.newHashSet();
    for (Collection<Problem> problems : previousProblemMap.values()) {
      oldProblems.addAll(problems);
    }

    return oldProblems;
  }
}

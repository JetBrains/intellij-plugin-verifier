package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.FailUtil;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.TeamCityLog;
import com.jetbrains.pluginverifier.utils.TeamCityUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CompareResultsCommand extends VerifierCommand {
  public CompareResultsCommand() {
    super("compare-results");
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.size() != 2) {
      throw FailUtil.fail("You have to specify two result files. For example: \"java -jar verifier.jar compare-results res_133.200.xml res_133.201.xml\"");
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
      System.out.println("No problems appeared between two builds!");
    }
    else {
      Map<Problem, Collection<UpdateInfo>> collectionMap = res.asMap();
      System.out.printf("Found %d new problems:\n", collectionMap.size());

      List<String> clazzNames = new ArrayList<String>();
      for (Problem problem : collectionMap.keySet()) {
        clazzNames.add(problem.getClass().getCanonicalName());
      }
      Map<String, Integer> typeCounter = countObjects(clazzNames);

      Set<String> updateNames = new HashSet<String>();
      for (Collection<UpdateInfo> updateInfos : collectionMap.values()) {
        for (UpdateInfo updateInfo : updateInfos) {
          updateNames.add(updateInfo.toString());
        }
      }

      Map<String, Integer> updateCounter = countObjects(new ArrayList<String>(updateNames));

      System.out.println("Number of problems by type: ");
      for (Map.Entry<String, Integer> entry : typeCounter.entrySet()) {
        System.out.println("    " + entry.getKey() + " " + entry.getValue() + " " + (String.format("%.2f%%", (double) entry.getValue() * 100 / collectionMap.size())));
      }

      System.out.println("Number of problems by plugin: ");
      for (Map.Entry<String, Integer> entry : updateCounter.entrySet()) {
        System.out.println("    " + entry.getKey() + " " + entry.getValue() + " " + (String.format("%.2f%%", (double) entry.getValue() * 100 / collectionMap.size())));
      }

      for (Map.Entry<Problem, Collection<UpdateInfo>> entry : collectionMap.entrySet()) {
        System.out.println(MessageUtils.cutCommonPackages(entry.getKey().getDescription()));

        Collection<UpdateInfo> updates = entry.getValue();

        System.out.printf("    at %d locations: %s\n", updates.size(), Joiner.on(", ").join(updates));

        System.out.println();
      }
    }

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);
    TeamCityUtil.printReport(tc, res, TeamCityUtil.ReportGrouping.NONE);

    System.out.println("Done");

    return 0;
  }

  @NotNull
  private Map<String, Integer> countObjects(List<String> clazzNames) {
    Map<String, Integer> typeCounter = new HashMap<String, Integer>();
    for (String name : clazzNames) {
      if (!typeCounter.containsKey(name)) {
        typeCounter.put(name, 1);
      } else {
        Integer integer = typeCounter.get(name);
        typeCounter.put(name, integer + 1);
      }
    }
    return typeCounter;
  }

  @NotNull
  private Set<Problem> getOldProblems(@NotNull File previousResults) throws IOException {
    Map<UpdateInfo, Collection<Problem>> previousProblemMap = ProblemUtils.loadProblems(previousResults).asMap();

    Set<Problem> oldProblems = Sets.newHashSet();
    for (Collection<Problem> problems : previousProblemMap.values()) {
      oldProblems.addAll(problems);
    }

    return oldProblems;
  }
}

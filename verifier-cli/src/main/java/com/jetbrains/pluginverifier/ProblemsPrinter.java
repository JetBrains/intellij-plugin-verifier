package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.commands.VerifierCommand;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.results.ResultsElement;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static com.jetbrains.pluginverifier.utils.StringUtil.pluralize;

/**
 * @author Sergey Patrikeev
 */
public class ProblemsPrinter extends VerifierCommand {

  public ProblemsPrinter() {
    super("print-problems");
  }


  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    TeamCityLog log = TeamCityLog.Companion.getInstance(commandLine);

    String file = commandLine.getOptionValue("printFile");

    if (file == null) {
      throw FailUtil.fail("You have to specify report-file to be printed");
    }

    File reportToCheck = new File(file);
    if (!reportToCheck.isFile()) {
      throw FailUtil.fail("Report not found: " + reportToCheck);
    }

    TeamCityUtil.ReportGrouping grouping = TeamCityUtil.ReportGrouping.Companion.parseGrouping(commandLine);

    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();


    switch (grouping) {
      case NONE:
        break;
      case PLUGIN:
        TeamCityUtil.INSTANCE.groupByPlugin(log, TeamCityUtil.INSTANCE.fillWithEmptyLocations(map));
        break;
      case PROBLEM_TYPE:
        TeamCityUtil.INSTANCE.groupByType(log, map);
        break;
      case PLUGIN_WITH_LOCATION:
        Map<UpdateInfo, ProblemSet> setMap = new HashMap<>();
        for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
          for (Problem problem : entry.getValue()) {
            setMap.putIfAbsent(entry.getKey(), new ProblemSet());
            setMap.get(entry.getKey()).addProblem(problem, ProblemLocation.fromClass("TestClass"));
          }
        }
        TeamCityUtil.INSTANCE.groupByPlugin(log, setMap);
        break;
    }

    final int totalProblems = new HashSet<>(currentCheckResult.getProblems()).size(); //number of unique problems

    String description = "IDE " + currentCheckResult.getIde() + " has " + totalProblems + " " + pluralize("problem", totalProblems);
    log.buildProblem(description);
    log.buildStatus(description);

    return 0;
  }


}

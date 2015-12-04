package com.jetbrains.pluginverifier.commands;

import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.ResultsElement;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    TeamCityLog log = TeamCityLog.getInstance(commandLine);

    String file = commandLine.getOptionValue("printFile");

    if (file == null) {
      throw FailUtil.fail("You have to specify report-file to be printed");
    }

    File reportToCheck = new File(file);
    if (!reportToCheck.isFile()) {
      throw FailUtil.fail("Report not found: " + reportToCheck);
    }

    TeamCityUtil.ReportGrouping grouping = TeamCityUtil.ReportGrouping.parseGrouping(commandLine);

    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();


    switch (grouping) {
      case NONE:
        break;
      case PLUGIN:
        TeamCityUtil.groupByPlugin(log, map);
        break;
      case PROBLEM_TYPE:
        TeamCityUtil.groupByType(log, map);
        break;
    }

    final int totalProblems = new HashSet<Problem>(currentCheckResult.getProblems()).size(); //number of unique problems

    String description = "IDE " + currentCheckResult.getIde() + " has " + totalProblems + " " + pluralize("problem", totalProblems);
    log.buildProblem(description);
    log.buildStatus(description);

    return 0;
  }


}

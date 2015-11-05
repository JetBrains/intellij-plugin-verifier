package com.jetbrains.pluginverifier.commands;

import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.FailUtil;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.ResultsElement;
import com.jetbrains.pluginverifier.utils.TeamCityLog;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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


    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();


    int totalProblems = 0;

    TeamCityLog.Test test = log.testStarted("someTest");

    try {

      for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
        UpdateInfo updateInfo = entry.getKey();
        Collection<Problem> problems = entry.getValue();
        totalProblems += problems.size();

        TeamCityLog.TestSuite testSuite = log.testSuiteStarted("Plugin " + updateInfo.toString());
        try {
          for (Problem problem : problems) {
            log.testStdErr("verifierClassName.testName", problem.getDescription());
          }
          log.testFailed("verifierClassName.testName", "plugin " + updateInfo + " has problems", problems.size() + " problems!");

        } finally {
          testSuite.close();
        }

      }
    } finally {
      test.close();
    }

    log.buildProblem("someBuildProblem");

    log.buildStatus("Total " + totalProblems + " found");

    return 0;
  }
}

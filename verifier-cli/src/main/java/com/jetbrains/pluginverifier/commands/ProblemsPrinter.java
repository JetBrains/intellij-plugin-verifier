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


    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();


    final int totalProblems = currentCheckResult.getProblems().size();

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
      UpdateInfo updateInfo = entry.getKey();
      Collection<Problem> problems = entry.getValue();

      String testName = updateInfo.toString();
      TeamCityLog.Test test = log.testStarted(testName);
      try {
        for (Problem problem : problems) {
          log.testStdErr(testName, problem.getDescription());
        }
        log.testFailed(testName, updateInfo + " has " + problems.size() + pluralize("problem", problems.size()), "");
      } finally {
        test.close();
      }
    }

    /*
    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
      UpdateInfo updateInfo = entry.getKey();
      Collection<Problem> problems = entry.getValue();

      String plugin = updateInfo.toString();
      TeamCityLog.TestSuite suite = log.testSuiteStarted(plugin);
      try {
        for (Problem problem : problems) {
          String description = problem.getDescription();

          String canonicalName = problem.getClass().getCanonicalName();

          String testName = plugin + "." + description;

          TeamCityLog.Test test =  log.testStarted(testName);
          try {
            log.testFailed(testName, "", description);
          } finally {
            test.close();
          }
        }
      } finally {
        suite.close();
      }


    }
    */
/*
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
*/

    String description = "IDE " + currentCheckResult.getIde() + " has " + totalProblems + " " + pluralize("problem", totalProblems);
    log.buildProblem(description);
    log.buildStatus(description);

    return 0;
  }
}

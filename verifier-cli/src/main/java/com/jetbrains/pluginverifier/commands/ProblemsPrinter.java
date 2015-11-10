package com.jetbrains.pluginverifier.commands;

import com.google.common.collect.ArrayListMultimap;
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


    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();

    ArrayListMultimap<String, UpdateInfo> idToUpdates = fillIdToUpdates(map);

    final int totalProblems = new HashSet<Problem>(currentCheckResult.getProblems()).size(); //number of unique problems

    for (String pluginId : idToUpdates.keySet()) {
      List<UpdateInfo> updateInfos = idToUpdates.get(pluginId);
      ProblemUtils.sortUpdates(updateInfos);

      TeamCityLog.TestSuite pluginSuite = log.testSuiteStarted(pluginId);

      for (UpdateInfo updateInfo : updateInfos) {

        List<Problem> problems = ProblemUtils.sortProblems(map.get(updateInfo));

        if (!problems.isEmpty()) {
          String version = updateInfo.getVersion() != null ? updateInfo.getVersion() : "#" + updateInfo.getUpdateId();

          StringBuilder builder = new StringBuilder();

          for (Problem problem : problems) {
            builder.append("#").append(problem.getDescription()).append("\n");
          }

          String testName = version.replace('.', ',');
          TeamCityLog.Test test = log.testStarted(testName);
          log.testStdErr(testName, builder.toString());
          log.testFailed(testName, updateInfo + " has " + problems.size() + " " + pluralize("problem", problems.size()), "");
          test.close();

        }
      }
      pluginSuite.close();
    }


    String description = "IDE " + currentCheckResult.getIde() + " has " + totalProblems + " " + pluralize("problem", totalProblems);
    log.buildProblem(description);
    log.buildStatus(description);

    return 0;
  }

  @NotNull
  private ArrayListMultimap<String, UpdateInfo> fillIdToUpdates(Map<UpdateInfo, Collection<Problem>> map) {
    ArrayListMultimap<String, UpdateInfo> idToUpdates = ArrayListMultimap.create();
    for (UpdateInfo updateInfo : map.keySet()) {
      String pluginId = updateInfo.getPluginId() != null ? updateInfo.getPluginId() : "#" + updateInfo.getUpdateId();
      idToUpdates.put(pluginId, updateInfo);
    }
    return idToUpdates;
  }
}

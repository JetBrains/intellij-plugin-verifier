package verifier.tests;

import com.jetbrains.pluginverifier.commands.CheckPluginCommand;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.ToStringCachedComparator;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntegrationTest {
  @Test
  public void issue2() throws Exception {
    runCheck(
        "AWSCloudFormation-0.3.9.zip", TestData.IDEA_IC_14_0_4_ZIP,
        "accessing to unknown class com.intellij.json.JsonContextType\n  void com.intellij.aws.cloudformation.CloudFormationCodeContextType#<init>()");
  }

  @Test
  public void issue2_fixed_plugin() throws Exception {
    runCheck("AWSCloudFormation-0.3.16.zip", TestData.IDEA_IC_14_0_4_ZIP, "");
  }

  private void runCheck(String pluginId, String ideId, String expectedProblems) throws Exception {
    final File plugin = TestData.fetchResource(pluginId, false);
    final File ide = TestData.fetchResource(ideId, true);

    List<String> args = Arrays.asList(plugin.getPath(), ide.getPath());

    final CommandLine commandLine = new GnuParser().parse(Util.INSTANCE.getCMD_OPTIONS(), args.toArray(new String[args.size()]));
    final CheckPluginCommand checkPluginCommand = new CheckPluginCommand();
    checkPluginCommand.execute(commandLine, Arrays.asList(commandLine.getArgs()));

    Assert.assertEquals(expectedProblems, problemSetToString(checkPluginCommand.getLastProblemSet()));
  }

  private String problemSetToString(ProblemSet set) {
    StringWriter result = new StringWriter();

    final ArrayList<Problem> problems = new ArrayList<>(set.asMap().keySet());
    Collections.sort(problems, (o1, o2) -> o1.getDescription().compareTo(o2.getDescription()));

    for (Problem problem : problems) {
      result.append(problem.getDescription()).append("\n");

      final List<ProblemLocation> locations = new ArrayList<>(set.getLocations(problem));
      Collections.sort(locations, new ToStringCachedComparator<>());
      for (ProblemLocation location : locations) {
        result.append("  ").append(location.toString());
      }
    }

    return result.toString();
  }
}

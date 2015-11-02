package verifier.tests;

import com.jetbrains.pluginverifier.commands.CheckPluginCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.utils.ToStringCachedComparator;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.*;

public class IntegrationTest {
  @Test
  public void issue2() throws Exception {
    runCheck(
        "AWSCloudFormation-0.3.9.zip", "ideaIC-14.0.4.tar.gz",
        "accessing to unknown class: com.intellij.json.JsonContextType\n  void com.intellij.aws.cloudformation.CloudFormationCodeContextType#<init>()");
  }

  @Test
  public void issue2_fixed_plugin() throws Exception {
    runCheck("AWSCloudFormation-0.3.16.zip", "ideaIC-14.0.4.tar.gz", "");
  }

  private void runCheck(String pluginId, String ideId, String expectedProblems) throws Exception {
    final File plugin = TestData.fetchResource(pluginId, false);
    final File ide = TestData.fetchResource(ideId, true);

    List<String> args = Arrays.asList(plugin.getPath(), ide.getPath());

    final CommandLine commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args.toArray(new String[args.size()]));
    final CheckPluginCommand checkPluginCommand = new CheckPluginCommand();
    checkPluginCommand.execute(commandLine, Arrays.asList(commandLine.getArgs()));

    Assert.assertEquals(expectedProblems, problemSetToString(checkPluginCommand.getLastProblemSet()));
  }

  private String problemSetToString(ProblemSet set) {
    StringWriter result = new StringWriter();

    final ArrayList<Problem> problems = new ArrayList<Problem>(set.asMap().keySet());
    Collections.sort(problems, new Comparator<Problem>() {
      @Override
      public int compare(Problem o1, Problem o2) {
        return o1.getDescription().compareTo(o2.getDescription());
      }
    });

    for (Problem problem : problems) {
      result.append(problem.getDescription()).append("\n");

      final List<ProblemLocation> locations = new ArrayList<ProblemLocation>(set.getLocations(problem));
      Collections.sort(locations, new ToStringCachedComparator<ProblemLocation>());
      for (ProblemLocation location : locations) {
        result.append("  ").append(location.toString());
      }
    }

    return result.toString();
  }
}

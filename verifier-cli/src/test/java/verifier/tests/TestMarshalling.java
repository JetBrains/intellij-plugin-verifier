package verifier.tests;

import com.jetbrains.pluginverifier.commands.NewProblemsCommand;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.results.GlobalResultsRepository;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.results.ResultsElement;
import com.jetbrains.pluginverifier.results.plugin.PluginCheckResult;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sergey.Patrikeev
 */
public class TestMarshalling {

  public static void main(String[] args) throws IOException {
    Map<UpdateInfo, Collection<Problem>> map = new HashMap<UpdateInfo, Collection<Problem>>();
    List<Problem> problems = new ArrayList<Problem>();
    problems.add(new ClassNotFoundProblem("a"));
    problems.add(new DuplicateClassProblem("a", "b"));
    problems.add(new FailedToReadClassProblem("a", ""));
    problems.add(new IllegalMethodAccessProblem("a", IllegalMethodAccessProblem.MethodAccess.PACKAGE_PRIVATE));
    problems.add(new IncompatibleClassChangeProblem());
    problems.add(new MethodNotFoundProblem("a"));
    problems.add(new MethodNotImplementedProblem("a"));
    problems.add(new OverridingFinalMethodProblem("a"));
    problems.add(new MissingDependencyProblem("a", "aa", "bb"));
    map.put(new UpdateInfo(12345), problems);
    ProblemUtils.saveProblems(new File("brokenReport.xml"), "IU-144.0000", map);

  }

  private void loadAndOpenAllPluginResults() throws IOException {
    GlobalResultsRepository repository = new GlobalResultsRepository();
    for (int i = 143; i < 145; i++) {
      List<String> builds = NewProblemsCommand.findPreviousBuilds("IU-" + i + ".9999", repository);
      for (String build : builds) {
        File file = repository.getReportFile(build);
        System.out.println(build + " at " + file);
        ResultsElement element = ProblemUtils.loadProblems(file);
        Map<UpdateInfo, Collection<Problem>> map = element.asMap();
      }
    }
  }

  @Test
  public void testMarshallUnmarshall() throws Exception {

    Map<String, ProblemSet> expectedIdeToProblems = new HashMap<String, ProblemSet>();

    HashMap<Problem, Set<ProblemLocation>> problemSetHashMap = new HashMap<Problem, Set<ProblemLocation>>();
    problemSetHashMap.put(new ClassNotFoundProblem("com.jetbrains.AnAction"), new HashSet<ProblemLocation>(Arrays.asList(ProblemLocation.fromClass("location #1"), ProblemLocation.fromClass("location #2"))));
    problemSetHashMap.put(new MethodNotFoundProblem("class#invokeMethod()"), new HashSet<ProblemLocation>(Arrays.asList(ProblemLocation.fromClass("location #3"), ProblemLocation.fromClass("location #4"))));
    problemSetHashMap.put(new MissingDependencyProblem("pluginId", "missingId", "description"), new HashSet<ProblemLocation>(Arrays.asList(ProblemLocation.fromPlugin("pluginId#1"), ProblemLocation.fromClass("location #4"))));

    expectedIdeToProblems.put("IDEA-IU-143", new ProblemSet(problemSetHashMap));

    problemSetHashMap.put(new IllegalMethodAccessProblem("illegal", IllegalMethodAccessProblem.MethodAccess.PACKAGE_PRIVATE), new HashSet<ProblemLocation>(Collections.singletonList(ProblemLocation.fromClass("location #5"))));

    expectedIdeToProblems.put("IDEA-IU-144", new ProblemSet(problemSetHashMap));

    UpdateInfo updateInfo = new UpdateInfo("testId", "testName", "testVersion");

    PluginCheckResult checkResult = new PluginCheckResult(updateInfo, expectedIdeToProblems);

    File file = new File("for_tests", "test.xml");
    //noinspection ResultOfMethodCallIgnored
    file.getParentFile().mkdirs();

    //marshall
    ProblemUtils.savePluginCheckResult(file, checkResult);

    //unmarshall
    PluginCheckResult pluginCheckResult = ProblemUtils.loadPluginCheckResults(file);


    Map<String, ProblemSet> actualIdeToProblems = pluginCheckResult.getIdeToProblems();


  }
}

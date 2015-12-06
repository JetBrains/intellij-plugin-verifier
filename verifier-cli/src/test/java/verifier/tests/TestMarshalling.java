package verifier.tests;

import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IllegalMethodAccessProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.results.plugin.PluginCheckResult;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Created by Sergey.Patrikeev
 */
public class TestMarshalling {

  @Test
  public void testMarshallUnmarshall() throws Exception {

    Map<String, ProblemSet> expectedIdeToProblems = new HashMap<String, ProblemSet>();

    HashMap<Problem, Set<ProblemLocation>> problemSetHashMap = new HashMap<Problem, Set<ProblemLocation>>();
    problemSetHashMap.put(new ClassNotFoundProblem("com.jetbrains.AnAction"), new HashSet<ProblemLocation>(Arrays.asList(ProblemLocation.fromClass("location #1"), ProblemLocation.fromClass("location #2"))));
    problemSetHashMap.put(new MethodNotFoundProblem("class#invokeMethod()"), new HashSet<ProblemLocation>(Arrays.asList(ProblemLocation.fromClass("location #3"), ProblemLocation.fromClass("location #4"))));

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

    //TODO: check equivalence

  }
}

package verifier.tests;

import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.JDK;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class VerifierTest {

  private static final String IDEA_14_0_4 = "ideaIC-14.0.4.tar.gz";
  private Idea myIdea;
  private IdeaPlugin myPlugin;
  private ProblemSet myProblemSet;
  private Map<Problem, Set<ProblemLocation>> myProblems;

  @Before
  public void setUp() throws Exception {
    File ideaFile = TestData.fetchResource(IDEA_14_0_4, true);
    String jdkPath = System.getenv("JAVA_HOME");
    if (jdkPath == null) {
      jdkPath = "/usr/lib/jvm/java-6-oracle";
    }

    File jdkFile = new File(jdkPath);
    File pluginFile = new File("../classes/artifacts/mock_plugin/mock_plugin.jar");

    JDK jdk = new JDK(jdkFile);

    myIdea = new Idea(ideaFile, jdk);
    myPlugin = IdeaPlugin.createIdeaPlugin(pluginFile);

    List<String> args = Arrays.asList("");
    final CommandLine commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args.toArray(new String[args.size()]));

    VerificationContextImpl ctx = new VerificationContextImpl(PluginVerifierOptions.parseOpts(commandLine), myIdea);
    Verifiers.processAllVerifiers(myPlugin, ctx);

    myProblemSet = ctx.getProblems();
    myProblems = myProblemSet.asMap();
  }

  @Test
  public void testSuperClassVerifier() throws Exception {

    Problem[] shouldBeFoundProblems = new Problem[]{
        new ClassNotFoundProblem("non/existing/NonExistingInterface")
    };

    for (Problem problem : shouldBeFoundProblems) {
      Assert.assertTrue(myProblems.containsKey(problem));
    }

    for (Map.Entry<Problem, Set<ProblemLocation>> entry : myProblems.entrySet()) {
      System.out.println(entry.getKey());
    }

  }
}

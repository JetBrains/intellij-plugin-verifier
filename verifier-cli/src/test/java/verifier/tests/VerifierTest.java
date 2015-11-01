package verifier.tests;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.JDK;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class VerifierTest {

  private static final String IDEA_14_0_4 = "ideaIC-14.0.4.tar.gz";
  private static final ImmutableMultimap<Problem, ProblemLocation> ACTUAL_PROBLEMS =
      ImmutableMultimap.<Problem, ProblemLocation>builder()
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromField("mock/plugin/FieldTypeNotFound", "myNonExistingClass"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingInterface"), new ProblemLocation("mock/plugin/NotFoundInterface"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenReturn()Lnon/existing/NonExistingClass;"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenArg(Lnon/existing/NonExistingClass;)V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenLocalVar()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenThrows()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenCatch()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenDotClass()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenMultiArray()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenInvocation()V"))
          .put(new MethodNotFoundProblem("com/intellij/openapi/actionSystem/AnAction#nonExistingMethod()V"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenNonFoundMethod()V"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/actionSystem/AnAction#actionPerformed(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V"), new ProblemLocation("mock/plugin/OverrideFinalMethodProblem"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#getState()Ljava/lang/Object;"), new ProblemLocation("mock/plugin/NotImplementedProblem"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#loadState(Ljava/lang/Object;)V"), new ProblemLocation("mock/plugin/NotImplementedProblem"))
          .put(new OverridingFinalMethodProblem("com/intellij/openapi/actionSystem/AnAction#isEnabledInModalContext()Z"), ProblemLocation.fromMethod("mock/plugin/OverrideFinalMethodProblem", "isEnabledInModalContext()Z"))
          .put(new IllegalMethodAccessProblem("com/intellij/openapi/diagnostic/LogUtil#<init>()V"), ProblemLocation.fromMethod("mock/plugin/AccessChangedProblem", "foo()V"))
          .build();


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
    File pluginFile = new File("../mock-plugin/target/mock-plugin-1.0-SNAPSHOT.jar");

    JDK jdk = new JDK(jdkFile);

    myIdea = new Idea(ideaFile, jdk);
    myPlugin = IdeaPlugin.createIdeaPlugin(pluginFile);

    List<String> args = Collections.singletonList("");
    final CommandLine commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args.toArray(new String[args.size()]));

    VerificationContextImpl ctx = new VerificationContextImpl(PluginVerifierOptions.parseOpts(commandLine), myIdea);
    Verifiers.processAllVerifiers(myPlugin, ctx);

    myProblemSet = ctx.getProblems();
    myProblems = myProblemSet.asMap();
  }

  @Test
  public void testFoundProblems() throws Exception {

    Multimap<Problem, ProblemLocation> redundantProblems = HashMultimap.create();
    for (Map.Entry<Problem, Set<ProblemLocation>> entry : myProblems.entrySet()) {
      for (ProblemLocation location : entry.getValue()) {
        redundantProblems.put(entry.getKey(), location);
      }
    }

    for (Map.Entry<Problem, ProblemLocation> entry : ACTUAL_PROBLEMS.entries()) {
      Problem problem = entry.getKey();
      ProblemLocation location = entry.getValue();
      Assert.assertTrue("problem " + problem + " should be found, but it isn't", myProblems.containsKey(problem));
      Assert.assertTrue("problem " + problem + " should be found in the following location " + location, myProblems.get(problem).contains(location));
      redundantProblems.remove(problem, location);
    }


    if (!redundantProblems.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Problem, ProblemLocation> entry : redundantProblems.entries()) {
        builder.append(entry.getKey()).append(" at ").append(entry.getValue()).append("\n");
      }
      Assert.fail("Found redundant problems which shouldn't be found:\n" + builder.toString());
    }

  }
}

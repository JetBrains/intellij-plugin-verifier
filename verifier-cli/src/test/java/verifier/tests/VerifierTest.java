package verifier.tests;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.*;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTE: this test should be run AFTER mock-plugin and mock-idea artifacts
 * are generated.
 * The best way to run this test is a Maven PACKAGE goal (because all
 * the module artifact-dependencies will be resolved automatically)
 *
 * @author Sergey Patrikeev
 */
public class VerifierTest {

  private static final String IDEA_14_0_4 = "ideaIC-14.0.4.tar.gz";
  private static final ImmutableMultimap<Problem, ProblemLocation> ACTUAL_PROBLEMS =
      ImmutableMultimap.<Problem, ProblemLocation>builder()
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromField("mock/plugin/FieldTypeNotFound", "myNonExistingClass"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingInterface"), ProblemLocation.fromClass("mock/plugin/NotFoundInterface"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenReturn()Lnon/existing/NonExistingClass;"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenArg(Lnon/existing/NonExistingClass;)V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenLocalVar()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenThrows()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenCatch()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenDotClass()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenMultiArray()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenMultiArray()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenInvocation()V"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromClass("mock/plugin/ParentDoesntExist"))
          .put(new ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/ParentDoesntExist", "<init>()V"))
          .put(new ClassNotFoundProblem("non/existing/DeletedClass"), ProblemLocation.fromMethod("mock/plugin/inheritance/PluginClass", "<init>()V"))
          .put(new ClassNotFoundProblem("non/existing/DeletedClass"), ProblemLocation.fromClass("mock/plugin/inheritance/PluginClass"))

          .put(new MethodNotFoundProblem("com/intellij/openapi/actionSystem/AnAction#nonExistingMethod()V"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenNonFoundMethod()V"))

          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#getState()Ljava/lang/Object;"), ProblemLocation.fromClass("mock/plugin/NotImplementedProblem"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#loadState(Ljava/lang/Object;)V"), ProblemLocation.fromClass("mock/plugin/NotImplementedProblem"))
          .put(new MethodNotImplementedProblem("com/intellij/psi/search/UseScopeEnlarger#getAdditionalUseScope(Lcom/intellij/psi/PsiElement;)Lcom/intellij/psi/search/SearchScope;"), ProblemLocation.fromClass("mock/plugin/abstrackt/NotImplementedAbstractMethod"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#getState()Ljava/lang/Object;"), ProblemLocation.fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented"))
          .put(new MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent#loadState(Ljava/lang/Object;)V"), ProblemLocation.fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented"))

          .put(new OverridingFinalMethodProblem("com/intellij/openapi/actionSystem/AnAction#isEnabledInModalContext()Z"), ProblemLocation.fromMethod("mock/plugin/OverrideFinalMethodProblem", "isEnabledInModalContext()Z"))

          .put(new IllegalMethodAccessProblem("com/intellij/openapi/diagnostic/LogUtil#<init>()V", IllegalMethodAccessProblem.MethodAccess.PRIVATE), ProblemLocation.fromMethod("mock/plugin/AccessChangedProblem", "foo()V"))
          .build();


  private Ide myIde;
  private Plugin myPlugin;
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
    File pluginFile = findLatestPlugin();
//    File pluginFile = new File("../for_tests/ideavim-0.29.zip");
//    File pluginFile = new File("../for_tests/Maven_Sync.zip");
//    File pluginFile = new File("../for_tests/keymap.zip");

    IdeRuntime javaRuntime = IdeRuntimeManager.getJdkManager().createRuntime(jdkFile);

    myIde = IdeManager.getIdeaManager().createIde(ideaFile);
    myPlugin = PluginManager.getIdeaPluginManager().createPlugin(pluginFile);

    List<String> args = Collections.singletonList("");
    final CommandLine commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args.toArray(new String[args.size()]));

    VerificationContextImpl ctx = new VerificationContextImpl(PluginVerifierOptions.parseOpts(commandLine), myIde, javaRuntime, null);
    Verifiers.processAllVerifiers(myPlugin, ctx);

    myProblemSet = ctx.getProblemSet();
    myProblems = myProblemSet.asMap();
  }


  @NotNull
  private File findLatestPlugin() throws FileNotFoundException {
    Pattern compile = Pattern.compile("mock-plugin-(\\d+\\.\\d+).jar");
    File file = new File("../mock-plugin/target/");

    File[] files = file.listFiles();

    File result = null;
    double best = 0.0;

    if (files != null) {
      for (File f : files) {
        String name = f.getName();
        Matcher matcher = compile.matcher(name);
        if (matcher.matches()) {
          String group = matcher.group(1);
          double cur = Double.parseDouble(group);
          if (best < cur) {
            best = cur;
            result = f;
          }
        }
      }
    }

    if (result == null) {
      throw new FileNotFoundException("Plugin for tests is not found");
    }
    return result;
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

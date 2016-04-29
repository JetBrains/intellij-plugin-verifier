package verifier.tests;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.*;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.problems.statics.InvokeStaticOnInstanceMethodProblem;
import com.jetbrains.pluginverifier.problems.statics.InvokeVirtualOnStaticMethodProblem;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.verifiers.PluginVerifierOptions;
import com.jetbrains.pluginverifier.verifiers.VerificationContextImpl;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
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

  public static final IdeVersion DUMMY_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500");
  private static final String IDEA_14_0_4 = TestData.IDEA_IC_14_0_4_ZIP;
  private final ImmutableMultimap<Problem, ProblemLocation> MY_ACTUAL_PROBLEMS =
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

          .put(new IllegalMethodAccessProblem("com/intellij/openapi/diagnostic/LogUtil#<init>()V", AccessType.PRIVATE), ProblemLocation.fromMethod("mock/plugin/AccessChangedProblem", "foo()V"))

          .put(new InvokeVirtualOnStaticMethodProblem("com/intellij/lang/SmartEnterProcessor#commit()V"), ProblemLocation.fromMethod("mock/plugin/invokeVirtualOnStatic/SmartEnterProcessorUser", "main()V"))
          .put(new InvokeStaticOnInstanceMethodProblem("invocation/InvocationProblems#wasStatic()V"), ProblemLocation.fromMethod("mock/plugin/invokeStaticOnInstance/InvocationProblemsUser", "foo()V"))

          .put(new MissingDependencyProblem("org.some.company.plugin", "DevKit", "Plugin org.some.company.plugin depends on the other plugin DevKit which has not a compatible build with IU-145.500"), ProblemLocation.fromPlugin("org.some.company.plugin"))

          .build();

  private final ImmutableMultimap<Problem, ProblemLocation> RUBY_ACTUAL_PROBLEMS =
      ImmutableMultimap.<Problem, ProblemLocation>builder()
          .put(new MethodNotFoundProblem("com/intellij/util/ui/ReloadableComboBoxPanel#setDataProvider(Lcom/intellij/util/ui/ReloadableComboBoxPanel$DataProvider;)V"), ProblemLocation.fromMethod("org/jetbrains/plugins/ruby/rails/facet/ui/wizard/ui/tabs/RailsAppSampleConfigurableTab", "createUIComponents()V"))
          .put(new ClassNotFoundProblem("com/intellij/util/ui/ReloadableComboBoxPanel$DataProvider"), ProblemLocation.fromClass("org/jetbrains/plugins/ruby/rails/facet/ui/wizard/ui/tabs/RailsAppSampleProvider"))
          .build();


  private Ide myIde;
  private Plugin myPlugin;
  private Jdk myJavaRuntime;

  private static void testFoundProblems(Map<Problem, Set<ProblemLocation>> foundProblems, Multimap<Problem, ProblemLocation> actualProblems) throws Exception {

    Multimap<Problem, ProblemLocation> redundantProblems = HashMultimap.create();
    for (Map.Entry<Problem, Set<ProblemLocation>> entry : foundProblems.entrySet()) {
      for (ProblemLocation location : entry.getValue()) {
        redundantProblems.put(entry.getKey(), location);
      }
    }

    for (Map.Entry<Problem, ProblemLocation> entry : actualProblems.entries()) {
      Problem problem = entry.getKey();
      ProblemLocation location = entry.getValue();
      Assert.assertTrue("problem " + problem + " should be found, but it isn't", foundProblems.containsKey(problem));
      Assert.assertTrue("problem " + problem + " should be found in the following location " + location, foundProblems.get(problem).contains(location));
      redundantProblems.remove(problem, location);
    }


    if (!redundantProblems.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Problem, ProblemLocation> entry : redundantProblems.entries()) {
//        CodeLocation loc = (CodeLocation) entry.getValue();
//        System.out.println("xxxxxxx|" + loc.getClassName() + " " + loc.getMethodDescriptor() + " " + loc.getFieldName() + "||||");

        builder.append(entry.getKey()).append(" at ").append(entry.getValue()).append("\n");
      }
      Assert.fail("Found redundant problems which shouldn't be found:\n" + builder.toString());
    }

  }

  @Before
  public void setUp() throws Exception {
    String jdkPath = System.getenv("JAVA_HOME");
    if (jdkPath == null) {
      jdkPath = "/usr/lib/jvm/java-6-oracle";
    }
    myJavaRuntime = Jdk.createJdk(new File(jdkPath));


  }

  @Test
  public void testRuby20160127() throws Exception {
    File idea = TestData.fetchResource("ideaIU-144.3600.7.zip", true);
    File rubyPlugin = TestData.fetchResource("ruby-8.0.0.20160127.zip", false);
    testFoundProblems(idea, rubyPlugin, RUBY_ACTUAL_PROBLEMS, false);
  }

  @Test
  public void testMyPlugin() throws Exception {
    File ideaFile = new File("build/mocks/after-idea");
    File pluginFile = findLatestFile(new File("build/mocks"), "mock-plugin");
    testFoundProblems(ideaFile, pluginFile, MY_ACTUAL_PROBLEMS, true);
  }


  @NotNull
  private File findLatestFile(File dir, final String fileName) throws FileNotFoundException {
    Pattern compile = Pattern.compile(fileName + "-(\\d+\\.\\d+).jar");

    File[] files = dir.listFiles();

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

  private void testFoundProblems(File ideaFile, File pluginFile, ImmutableMultimap<Problem, ProblemLocation> actualProblems, boolean dummyIdeVersion) throws Exception {
    if (dummyIdeVersion) {
      myIde = IdeManager.getInstance().createIde(ideaFile, DUMMY_IDE_VERSION);
    } else {
      myIde = IdeManager.getInstance().createIde(ideaFile);
    }

    myPlugin = PluginManager.getInstance().createPlugin(pluginFile);

    final CommandLine commandLine = new GnuParser().parse(Util.CMD_OPTIONS, new String[]{});

    VerificationContextImpl ctx = new VerificationContextImpl(myPlugin, myIde, myJavaRuntime, null, PluginVerifierOptions.parseOpts(commandLine));
    Verifiers.processAllVerifiers(ctx);

    testFoundProblems(ctx.getProblemSet().asMap(), actualProblems);
  }
}

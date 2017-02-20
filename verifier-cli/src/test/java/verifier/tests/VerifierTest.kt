package verifier.tests

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.location.AccessFlags
import com.jetbrains.pluginverifier.location.ClassPath
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.location.ProblemLocation.Companion.fromClass
import com.jetbrains.pluginverifier.location.ProblemLocation.Companion.fromField
import com.jetbrains.pluginverifier.location.ProblemLocation.Companion.fromMethod
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.util.regex.Pattern

/**
 * NOTE: this test should be run AFTER mock-plugin and mock-idea artifacts
 * are generated.
 * The best way to run this test is a Maven PACKAGE goal (because all
 * the module artifact-dependencies will be resolved automatically)

 * @author Sergey Patrikeev
 */
class VerifierTest {

  private fun MY_ACTUAL_PROBLEMS(classPath: ClassPath) = ImmutableMultimap.builder<Problem, ProblemLocation>()

      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromField(fromClass("mock/plugin/FieldTypeNotFound", null, classPath, AccessFlags(0x21)), "myNonExistingClass", "Lnon/existing/NonExistingClass;", null, AccessFlags(0x2)))
      .put(ClassNotFoundProblem("non/existing/NonExistingInterface"), fromClass("mock/plugin/NotFoundInterface", null, classPath, AccessFlags(0x601)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenReturn", "()Lnon/existing/NonExistingClass;", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenArg", "(Lnon/existing/NonExistingClass;)V", listOf("brokenArg"), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenLocalVar", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingException"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenThrows", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingException"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenCatch", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenDotClass", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenMultiArray", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenMultiArray", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenInvocation", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromClass("mock/plugin/ParentDoesntExist", null, classPath, AccessFlags(0x21)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/ParentDoesntExist", null, classPath, AccessFlags(0x21)), "<init>", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/arrays/ANewArrayInsn", null, classPath, AccessFlags(0x21)), "foo", "(JDLjava/lang/Object;)V", listOf("l", "d", "a"), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/arrays/ANewArrayInsn", null, classPath, AccessFlags(0x21)), "foo2", "(JDLjava/lang/Object;)V", listOf("l", "d", "a"), null, AccessFlags(0x9)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessUnknownClassOfArray", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/DeletedClass"), fromMethod(fromClass("mock/plugin/inheritance/PluginClass", null, classPath, AccessFlags(0x21)), "<init>", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/DeletedClass"), fromClass("mock/plugin/inheritance/PluginClass", null, classPath, AccessFlags(0x21)))
      .put(InheritFromFinalClassProblem("finals/BecomeFinal"), fromClass("mock/plugin/finals/InheritFromFinalClass", null, classPath, AccessFlags(0x21)))
      .put(MethodNotFoundProblem("com/intellij/openapi/actionSystem/AnAction", "nonExistingMethod", "()V"), fromMethod(fromClass("mock/plugin/MethodProblems", null, classPath, AccessFlags(0x21)), "brokenNonFoundMethod", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(AbstractClassInstantiationProblem("misc/BecomeAbstract"), fromMethod(fromClass("mock/plugin/news/NewProblems", null, classPath, AccessFlags(0x21)), "abstractClass", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(InterfaceInstantiationProblem("misc/BecomeInterface"), fromMethod(fromClass("mock/plugin/news/NewProblems", null, classPath, AccessFlags(0x21)), "newInterface", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(IncompatibleClassToInterfaceChangeProblem(SymbolicReference.classFrom("misc/BecomeInterface")), fromMethod(fromClass("mock/plugin/news/NewProblems", null, classPath, AccessFlags(0x21)), "newInterface", "()V", emptyList(), null, AccessFlags(0x1)))

      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "getState", "()Ljava/lang/Object;"), fromClass("mock/plugin/NotImplementedProblem", null, classPath, AccessFlags(0x21)))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), fromClass("mock/plugin/NotImplementedProblem", null, classPath, AccessFlags(0x21)))
      .put(MethodNotImplementedProblem("com/intellij/psi/search/UseScopeEnlarger", "getAdditionalUseScope", "(Lcom/intellij/psi/PsiElement;)Lcom/intellij/psi/search/SearchScope;"), fromClass("mock/plugin/abstrackt/NotImplementedAbstractMethod", null, classPath, AccessFlags(0x21)))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "getState", "()Ljava/lang/Object;"), fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented", "Ljava/lang/Object;Lcom/intellij/openapi/components/PersistentStateComponent<Ljava/lang/String;>;", classPath, AccessFlags(0x21)))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented", "Ljava/lang/Object;Lcom/intellij/openapi/components/PersistentStateComponent<Ljava/lang/String;>;", classPath, AccessFlags(0x21)))

      .put(AbstractMethodInvocationProblem("invokespecial/AbstractParent", "foo", "()V"), fromMethod(fromClass("mock/plugin/invokespecial/Child", null, classPath, AccessFlags(0x421)), "bar", "()V", emptyList(), null, AccessFlags(0x1)))

      .put(OverridingFinalMethodProblem("com/intellij/openapi/actionSystem/AnAction", "isEnabledInModalContext", "()Z"), fromMethod(fromClass("mock/plugin/OverrideFinalMethodProblem", null, classPath, AccessFlags(0x21)), "isEnabledInModalContext", "()Z", emptyList(), null, AccessFlags(0x1)))
      .put(IllegalMethodAccessProblem("com/intellij/openapi/diagnostic/LogUtil", "<init>", "()V", AccessType.PRIVATE), fromMethod(fromClass("mock/plugin/AccessChangedProblem", null, classPath, AccessFlags(0x21)), "foo", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(InvokeVirtualOnStaticMethodProblem("com/intellij/lang/SmartEnterProcessor", "commit", "()V"), fromMethod(fromClass("mock/plugin/invokeVirtualOnStatic/SmartEnterProcessorUser", null, classPath, AccessFlags(0x21)), "main", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(InvokeStaticOnInstanceMethodProblem("invocation/InvocationProblems", "wasStatic", "()V"), fromMethod(fromClass("mock/plugin/invokeStaticOnInstance/InvocationProblemsUser", null, classPath, AccessFlags(0x21)), "foo", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(FieldNotFoundProblem("fields/FieldsContainer", "deletedField", "I"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessDeletedField", "()V", emptyList(), null, AccessFlags(0x1)))//field problems

//      protected members access check
//      .put(IllegalFieldAccessProblem("mock/plugin/access/Point3d", "x", "I", AccessType.PROTECTED), ProblemLocation.Companion.fromMethod("mock/plugin/access/Point3d", "delta", "(Laccess/points/Point;)V"))
//      .put(IllegalFieldAccessProblem("access/AccessProblemBase", "x", "I", AccessType.PROTECTED), ProblemLocation.fromMethod("mock/plugin/access/IllegalAccess", "main", "([Ljava/lang/String;)V"))
//      .put(IllegalMethodAccessProblem("access/AccessProblemBase", "foo", "()V", AccessType.PROTECTED), ProblemLocation.fromMethod("mock/plugin/access/IllegalAccess", "main", "([Ljava/lang/String;)V"))

      .put(IllegalFieldAccessProblem("fields/FieldsContainer", "privateField", "I", AccessType.PRIVATE), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessPrivateField", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(IllegalFieldAccessProblem("fields/otherPackage/OtherFieldsContainer", "protectedField", "I", AccessType.PROTECTED), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessProtectedField", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(IllegalFieldAccessProblem("fields/otherPackage/OtherFieldsContainer", "packageField", "I", AccessType.PACKAGE_PRIVATE), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessPackageField", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(InstanceAccessOfStaticFieldProblem("fields/FieldsContainer", "staticField", "I"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "instanceAccessOnStatic", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(StaticAccessOfInstanceFieldProblem("fields/FieldsContainer", "instanceField", "I"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "staticAccessOnInstance", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "accessUnknownClass", "()V", emptyList(), null, AccessFlags(0x1)))
      .put(ChangeFinalFieldProblem("fields/FieldsContainer", "finalField", "I"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "setOnFinalFieldFromNotInitMethod", "()V", emptyList(), null, AccessFlags(0x1)))

      //missing default constructor
      .put(MethodNotFoundProblem("constructors/DeletedDefaultConstructor", "<init>", "()V"), fromMethod(fromClass("mock/plugin/constructors/MissingDefaultConstructor", null, classPath, AccessFlags(0x21)), "<init>", "()V", emptyList(), null, AccessFlags(0x1)))

      .put(ChangeFinalFieldProblem("fields/FieldsContainer", "staticFinalField", "I"), fromMethod(fromClass("mock/plugin/field/FieldProblemsContainer", null, classPath, AccessFlags(0x21)), "setOnStaticFinalFieldFromNotClinitMethod", "()V", emptyList(), null, AccessFlags(0x1))).build()

  @Test
  @Throws(Exception::class)
  fun testMyPlugin() {
    val ideaFile = File("build/mocks/after-idea")
    val pluginFile = findLatestFile(File("build/mocks"), "mock-plugin")
    testFoundProblems(ideaFile, pluginFile, MY_ACTUAL_PROBLEMS(ClassPath(ClassPath.Type.ROOT, pluginFile.name)), true, true)
  }


  @Throws(FileNotFoundException::class)
  private fun findLatestFile(dir: File, fileName: String): File {
    val compile = Pattern.compile("$fileName-(\\d+\\.\\d+).jar")

    val files = dir.listFiles()

    var result: File? = null
    var best = 0.0

    if (files != null) {
      for (f in files) {
        val name = f.name
        val matcher = compile.matcher(name)
        if (matcher.matches()) {
          val group = matcher.group(1)
          val cur = java.lang.Double.parseDouble(group)
          if (best < cur) {
            best = cur
            result = f
          }
        }
      }
    }

    if (result == null) {
      throw FileNotFoundException("Plugin for tests is not found")
    }
    return result
  }

  @Throws(Exception::class)
  private fun testFoundProblems(ideaFile: File, pluginFile: File, actualProblems: ImmutableMultimap<Problem, ProblemLocation>, dummyIdeVersion: Boolean, isMyPlugin: Boolean) {
    val ide: Ide
    if (dummyIdeVersion) {
      ide = IdeManager.getInstance().createIde(ideaFile, DUMMY_IDE_VERSION)
    } else {
      ide = IdeManager.getInstance().createIde(ideaFile)
    }

    val plugin = PluginManager.getInstance().createPlugin(pluginFile)


    var jdkPath: String? = System.getenv("JAVA_HOME")
    if (jdkPath == null) {
      jdkPath = "/usr/lib/jvm/java-8-oracle"
    }

    Resolver.createIdeResolver(ide).use { ideResolver ->
      val pluginDescriptor = PluginDescriptor.ByInstance(plugin)
      val ideDescriptor = IdeDescriptor.ByInstance(ide, ideResolver)
      val vOptions = VOptionsUtil.parseOpts(CmdOpts())
      val results = VManager.verify(VParams(JdkDescriptor.ByFile(jdkPath!!), listOf(Pair<PluginDescriptor, IdeDescriptor>(pluginDescriptor, ideDescriptor)), vOptions, Resolver.getEmptyResolver(), true))
      val result = results.results[0]
      Assert.assertTrue(result is VResult.Problems)

      testFoundProblems((result as VResult.Problems).problems, actualProblems)

      if (isMyPlugin) {
        checkMissingDeps(result)
      }
    }

  }

  private fun checkMissingDeps(result: VResult.Problems) {
    Assert.assertFalse(result.dependenciesGraph.start.missingDependencies.isEmpty())
    Assert.assertTrue(result.dependenciesGraph.start.missingDependencies.containsKey(PluginDependencyImpl("MissingPlugin", true)))
  }

  companion object {

    private val DUMMY_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")


    @Throws(Exception::class)
    private fun testFoundProblems(foundProblems: Multimap<Problem, ProblemLocation>, actualProblems: Multimap<Problem, ProblemLocation>) {

//      println("Found problems: ${foundProblems.entries().joinToString { "$it\n" }}")

      val redundantProblems = HashMultimap.create(foundProblems)

      for ((problem, location) in actualProblems.entries()) {
        Assert.assertTrue("problem $problem should be found at $location, but it isn't", foundProblems.containsKey(problem))
        try {
          val contains = foundProblems.get(problem).contains(location)
          if (!contains) {
            println("ax")
          }
          Assert.assertTrue("problem " + problem + " should be found in the following location " + location + " but it is found in " + foundProblems.get(problem), contains)
        } catch (e: Exception) {
          e.printStackTrace()
        }

        redundantProblems.remove(problem, location)
      }


      if (!redundantProblems.isEmpty) {
        val builder = StringBuilder()
        for ((key, value) in redundantProblems.entries()) {
          //        CodeLocation loc = (CodeLocation) entry.getValue();
          //        System.out.println("xxxxxxx|" + loc.getClassName() + " " + loc.getMethodDescriptor() + " " + loc.getFieldName() + "||||");

          builder.append(key).append(" at ").append(value).append("\n")
        }
        Assert.fail("Found redundant problems which shouldn't be found:\n" + builder.toString())
      }

    }
  }
}

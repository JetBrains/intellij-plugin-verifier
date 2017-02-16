import com.github.salomonbrys.kotson.fromJson
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingReason
import com.jetbrains.pluginverifier.location.ClassPath
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.warnings.Warning
import org.junit.Assert
import org.junit.Test

private fun Any.toGson(): String = GsonHolder.GSON.toJson(this)

private inline fun <reified T : Any> String.fromGson() = GsonHolder.GSON.fromJson<T>(this)


/**
 * @author Sergey Patrikeev
 */
class Results {

  //assert to-from
  private inline fun <reified T : Any> assert(init: Any) {
    val json = init.toGson()
    val instance = json.fromGson<T>()
    Assert.assertEquals(init, instance)
  }

  private inline fun <reified T : Any> assertBlock(init: T, assertBlock: (T) -> Unit) {
    val json = init.toGson()
    val gson = json.fromGson<T>()
    assertBlock(gson)
  }

  @Test
  fun pluginDescriptor() {
    assert<PluginDescriptor>(PluginDescriptor.ByXmlId("xmlId", "123"))
  }

  @Test
  fun ideDescriptor() {
    assert<IdeDescriptor>(IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-143.15")))
  }

  @Test
  fun nice() {
    val init: VResult = someNicePlugin()
    assertBlock<VResult>(init, { Assert.assertEquals(init.toString(), it.toString()) })
  }

  @Test
  fun problems() {
    val init = someProblematicPlugin()
    assertBlock<VResult>(init, { Assert.assertEquals(init.toString(), it.toString()) })
  }

  @Test
  fun bad() {
    val init = someBadPlugin()
    assertBlock<VResult>(init, { Assert.assertEquals(init.toString(), it.toString()) })
  }

  private fun someNicePlugin(): VResult.Nice {
    val node = DependencyNode("pid", "", emptyMap())
    return VResult.Nice(PluginDescriptor.ByXmlId("pluginId", "version"), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-143.15")), listOf(Warning("warn#1"), Warning("warn#2")), DependenciesGraph(node, listOf(node), emptyList()))
  }

  private fun someProblematicPlugin(): VResult.Problems {
    val pluginDescriptor = PluginDescriptor.ByXmlId("pluginId", "version")
    val ideDescriptor = IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-123.456.789"))
    val multimap: Multimap<Problem, ProblemLocation> = ImmutableMultimap.of(ClassNotFoundProblem(SymbolicReference.classFrom("NotFoundClass")), ProblemLocation.fromClass("UserOfNotFoundClass", null, ClassPath(ClassPath.Type.JAR_FILE, "some.jar")))

    val node = DependencyNode("pluginId", "version", mapOf(PluginDependencyImpl("id", false) to MissingReason("reason")))
    val depImpl = PluginDependencyImpl("id2", true)
    val node2 = DependencyNode("pluginId2", "version2", mapOf(depImpl to MissingReason("reason2")))
    val dependenciesGraph = DependenciesGraph(node, listOf(node, node2), listOf(DependencyEdge(node, node2, depImpl)))

    val problems = VResult.Problems(pluginDescriptor, ideDescriptor, multimap, dependenciesGraph, listOf(Warning("one"), Warning("two")))
    return problems
  }

  private fun someBadPlugin() = VResult.BadPlugin(PluginDescriptor.ByXmlId("pluginId", "version"), "I am bad")

}
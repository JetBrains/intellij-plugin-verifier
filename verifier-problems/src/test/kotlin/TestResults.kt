import com.github.salomonbrys.kotson.fromJson
import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.persistence.GsonHolder
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
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
    assert<PluginDescriptor>(PluginDescriptor.ByBuildId("pluginId", "version", 1))
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

  private fun someNicePlugin() = VResult.Nice(PluginDescriptor.ByBuildId("pluginId", "version", 1), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-143.15")), "overview")

  private fun someProblematicPlugin() = VResult.Problems(PluginDescriptor.ByBuildId("pluginId", "version", 1), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-123.456.789")), "overview",
      ImmutableMultimap.of(ClassNotFoundProblem("NotFoundClass"), ProblemLocation.fromClass("UserOfNotFoundClass")))

  private fun someBadPlugin() = VResult.BadPlugin(PluginDescriptor.ByBuildId("pluginId", "version", 1), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-140.1")), "I am bad")

}
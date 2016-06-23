import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.persistence.fromGson
import com.jetbrains.pluginverifier.persistence.toGson
import com.jetbrains.pluginverifier.persistence.toGsonTyped
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.Problem
import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class Results {

  @Test
  fun pluginDescriptor() {
    assert<PluginDescriptor>(PluginDescriptor.ByBuildId(1))
    assert<PluginDescriptor>(PluginDescriptor.ByFile(File("/home/")))
    assert<PluginDescriptor>(PluginDescriptor.ByFile("/home/"))
    assert<PluginDescriptor>(PluginDescriptor.ByXmlId("xmlId", "123"))
  }

  @Test
  fun ideDescriptor() {
    assert<IdeDescriptor>(IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-143.15")))
    assert<IdeDescriptor>(IdeDescriptor.ByFile("/home/ide"))
  }

  @Test
  fun nice() {
    assertBlock<VResult.Nice>(VResult.Nice(PluginDescriptor.ByBuildId(1), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-143.15")), "overview"),
        {
          check(it.overview.equals("overview"))
        })
  }

  @Test
  fun problems() {
    assertBlock<VResult.Problems>(VResult.Problems(PluginDescriptor.ByBuildId(1), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-145.1111")), "overview",
        ImmutableMultimap.of(ClassNotFoundProblem("class"), ProblemLocation.fromClass("a"))), {
      check(it.overview.equals("overview"))
    })
  }

  private inline fun <reified T : Any> assertBlock(x: T, block: (T) -> Unit) {
    val json = x.toGson()
    println(json)
    block(json.fromGson<T>())
  }

  private inline fun <reified T : Any> assert(x: Any) {
    val json = x.toGson()
    println(json)
    val instance = json.fromGson<T>()
    Assert.assertEquals(x, instance)
  }

  @Test
  fun results() {
    val problem = ClassNotFoundProblem("not_found")
    val of: Multimap<Problem, ProblemLocation> = ImmutableMultimap.of(problem, ProblemLocation.fromPlugin("pluginId"))
    val problems = VResult.Problems(PluginDescriptor.ByXmlId("pluginId", "123"), IdeDescriptor.ByVersion(IdeVersion.createIdeVersion("IU-145.12.1")), "overview", of)

    val ps: String = of.toGsonTyped<Multimap<Problem, ProblemLocation>>()
    println(ps)

    val multimap = ps.fromGson<Multimap<Problem, ProblemLocation>>()

    println(multimap)

//    val s = GsonHolder.GSON.toJson(problems)
//    println(s)

  }
}
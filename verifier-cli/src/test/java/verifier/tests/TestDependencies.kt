package verifier.tests

import com.intellij.structure.impl.domain.IdeManagerImpl
import com.intellij.structure.impl.utils.StringUtil
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.utils.dependencies.Dependencies
import com.jetbrains.pluginverifier.utils.dependencies.getTransitiveDependencies
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class TestDependencies {

  private val dependencies = Arrays.asList<String>("JavaScriptDebugger:1.0", "com.intellij.copyright:8.1", "JUnit:1.0", "TestNG-J:8.0", "com.jetbrains.plugins.webDeployment:0.1", "AntSupport:1.0", "Coverage:null", "com.intellij.database:1.0", "com.intellij.properties:null", "org.intellij.intelliLang:8.0", "com.intellij.css:null", "JavaScript:1.0", "org.jetbrains.plugins.gradle:144.3742", "com.intellij.persistence:1.0", "org.jetbrains.plugins.terminal:0.1", "com.intellij.java-i18n:144.3742", "ByteCodeViewer:0.1", "org.jetbrains.plugins.remote-run:0.1", "XPathView:4", "org.jetbrains.plugins.haml:null", "cucumber:999.999", "com.intellij.diagram:1.0", "org.jetbrains.plugins.slim:8.0.0.20151215", "org.jetbrains.idea.maven:144.3742", "com.intellij.javaee:1.0", "org.jetbrains.plugins.sass:null", "com.intellij.plugins.watcher:144.988", "org.jetbrains.plugins.yaml:null", "org.intellij.groovy:9.0")

  @Test
  @Throws(Exception::class)
  fun getDependenciesWithTransitive() {
    val idea144_3600 = TestData.fetchResource("ideaIU-144.3600.7.zip", true)
    val pluginFile = TestData.fetchResource("ruby-8.0.0.20160127.zip", false)

    val ide = IdeManagerImpl.getInstance().createIde(idea144_3600)
    val plugin = PluginCache.createPlugin(pluginFile)

    val (graph, vertex) = Dependencies.calcDependencies(plugin, ide)

    val deps = graph.getTransitiveDependencies(vertex).map { it.plugin }

    assertNotNull(deps)
    assertEquals("Missing transitive dependencies", dependencies.size.toLong(), deps.size.toLong())

    for (s in dependencies) {
      var found = false
      for (dep in deps) {
        if (StringUtil.equal(s.split((":").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0], dep.pluginId)) {
          found = true
        }
      }
      assertTrue("Dependency " + s + " is not found", found)
    }
  }
}

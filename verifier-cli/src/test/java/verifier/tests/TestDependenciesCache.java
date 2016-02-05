package verifier.tests;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.impl.domain.IdeaManager;
import com.intellij.structure.impl.utils.StringUtil;
import com.jetbrains.pluginverifier.misc.DependenciesCache;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class TestDependenciesCache {

  final List<String> dependencies = Arrays.asList("JavaScriptDebugger:1.0", "com.intellij.copyright:8.1", "JUnit:1.0", "TestNG-J:8.0", "com.jetbrains.plugins.webDeployment:0.1", "AntSupport:1.0", "Coverage:null", "com.intellij.database:1.0", "com.intellij.properties:null", "org.intellij.intelliLang:8.0", "com.intellij.css:null", "JavaScript:1.0", "org.jetbrains.plugins.gradle:144.3742", "com.intellij.persistence:1.0", "org.jetbrains.plugins.terminal:0.1", "com.intellij.java-i18n:144.3742", "ByteCodeViewer:0.1", "org.jetbrains.plugins.remote-run:0.1", "XPathView:4", "org.jetbrains.plugins.haml:null", "cucumber:999.999", "com.intellij.diagram:1.0", "org.jetbrains.plugins.slim:8.0.0.20151215", "org.jetbrains.idea.maven:144.3742", "com.intellij.javaee:1.0", "org.jetbrains.plugins.sass:null", "com.intellij.plugins.watcher:144.988", "org.jetbrains.plugins.yaml:null", "org.intellij.groovy:9.0");

  @Test
  public void getDependenciesWithTransitive() throws Exception {
    File idea144_3600 = TestData.fetchResource("ideaIU-144.3600.7.zip", true);
    File pluginFile = TestData.fetchResource("ruby-8.0.0.20160127.zip", false);

    Ide ide = IdeaManager.getIdeaManager().createIde(idea144_3600);
    Plugin plugin = PluginManager.getIdeaPluginManager().createPlugin(pluginFile);

    DependenciesCache.PluginDependenciesDescriptor descriptor = DependenciesCache.getInstance().getDependenciesWithTransitive(ide, plugin, new ArrayList<DependenciesCache.PluginDependenciesDescriptor>());
    Set<Plugin> deps = descriptor.getDependencies();
    assertNotNull(deps);
    assertEquals("Missing transitive dependencies", dependencies.size(), deps.size());

    for (String s : dependencies) {
      boolean found = false;
      for (Plugin dep : deps) {
        String s1 = dep.getPluginId() + ":" + (dep.getPluginVersion().isEmpty() ? "null" : dep.getPluginVersion());
        if (StringUtil.equal(s, s1)) {
          found = true;
        }
      }
      assertTrue("Dependency " + s + " is not found", found);
    }
  }
}

package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class PluginTest_PHP {

  private static Plugin plugin;

  @Before
  public void setUp() throws Exception {
    if (plugin != null) return;

    File pluginFile = TestUtils.downloadPlugin(TestUtils.PHP_URL, "php-plugin.zip");

    plugin = PluginManager.getInstance().createPlugin(pluginFile);
  }

  /*@Test
  public void getAllXmlInRoot() throws Exception {
    Map<String, Document> allXmlInRoot = plugin.getAllXmlInRoot();

    List<String> list = Arrays.asList("META-INF/uml-support.xml", "META-INF/php-deployment-aware.xml", "META-INF/php-coverage.xml", "META-INF/plugin.xml", "META-INF/liveEdit-support.xml", "META-INF/intellilang-php-support.xml");
    assertTrue(allXmlInRoot.keySet().containsAll(list));
  }*/

  @Test
  public void testOptionalDependenciesConfigs() throws Exception {
    PluginImpl plugin = (PluginImpl) PluginTest_PHP.plugin;
    Map<PluginDependency, String> files = plugin.getOptionalDependenciesConfigFiles();
    assertEquals(6, files.size());

  }

  @Test
  public void getSinceBuild() throws Exception {
    IdeVersion sinceBuild = plugin.getSinceBuild();
    assertNotNull(sinceBuild);
    assertEquals(142, sinceBuild.getBaselineVersion());
    assertEquals(5068, sinceBuild.getBuild());
  }

  @Test
  public void getUntilBuild() throws Exception {
    assertNull(plugin.getUntilBuild());
  }

  @Test(expected = IllegalArgumentException.class)
  public void isCompatibleWithIde() throws Exception {
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("142.5068")));
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("142.5069")));
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("143.5069")));
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("144.5069")));
    assertTrue(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("145.5069")));
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("142.5067")));
    assertFalse(plugin.isCompatibleWithIde(IdeVersion.createIdeVersion("BAD_VERSION")));
  }

  @Test
  public void getDependencies() throws Exception {
    List<String> list = Arrays.asList("com.intellij.css", "com.intellij.java-i18n", "com.intellij.diagram", "Coverage", "com.jetbrains.plugins.webDeployment", "org.intellij.intelliLang", "com.intellij.plugins.html.instantEditing");
    List<String> list2 = Arrays.asList("com.intellij.diagram", "Coverage", "com.jetbrains.plugins.webDeployment", "org.intellij.intelliLang", "com.intellij.plugins.html.instantEditing");
    for (PluginDependency dependency : plugin.getDependencies()) {
      assertTrue(list.contains(dependency.getId()));
      assertEquals(list2.contains(dependency.getId()), dependency.isOptional());
    }
  }

  @Test
  public void getModuleDependencies() throws Exception {
    assertTrue(TestUtils.toStrings(plugin.getModuleDependencies()).containsAll(Arrays.asList("com.intellij.modules.xml", "com.intellij.modules.ultimate", "com.intellij.modules.coverage (optional)")));
  }

  @Test
  public void getPluginName() throws Exception {
    assertEquals("PHP", plugin.getPluginName());
  }

  @Test
  public void getPluginVersion() throws Exception {
    assertEquals("143.1184.87", plugin.getPluginVersion());
  }

  @Test
  public void getPluginId() throws Exception {
    assertEquals("com.jetbrains.php", plugin.getPluginId());
  }

  @Test
  public void getPluginVendor() throws Exception {
    assertEquals("JetBrains", plugin.getVendor());
  }

  @Test
  public void getDefinedModules() throws Exception {
    assertTrue(plugin.getDefinedModules().isEmpty());
  }

  @Test
  public void getPluginResolver() throws Exception {
    Resolver pluginResolver = Resolver.createCacheResolver(Resolver.createPluginResolver(plugin));
    Collection<String> allClasses = pluginResolver.getAllClasses();
    pluginResolver.close();

    assertTrue(allClasses.contains("com/intellij/structuralsearch/PhpStructuralSearchProfile"));
    assertTrue(allClasses.contains("com/jetbrains/php/PhpClassHierarchyUtils$8"));
    assertTrue(allClasses.contains("com/jetbrains/php/config/sdk/PhpLocalScriptRunner$2$1"));
    assertTrue(allClasses.contains("com/jetbrains/php/config/interpreters/PhpConfigurationFilePanelGenerator$PhpExpandableConfigurationFilePanel$1"));

    assertEquals(3497 + 226, allClasses.size());
  }

  @Test
  public void testOtherProperties() throws Exception {
    assertEquals("PHP 5.3-7 editing and debugging, PHPUnit, Smarty, Twig and various frameworks support", plugin.getDescription());
    assertEquals("https://www.jetbrains.com/phpstorm", plugin.getVendorUrl());
    assertEquals("JetBrains", plugin.getVendor());
  }
}
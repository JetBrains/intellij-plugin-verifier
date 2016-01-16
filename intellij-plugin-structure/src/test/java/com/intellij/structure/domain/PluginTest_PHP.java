package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.IdeaPluginManager;
import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.TestUtils;
import org.jdom.Document;
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

    plugin = IdeaPluginManager.getInstance().createPlugin(pluginFile);
  }

  @Test
  public void getPluginXml() throws Exception {
    Document pluginXml = plugin.getPluginXml();
    assertNotNull(pluginXml);
  }

  @Test
  public void getAllXmlInRoot() throws Exception {
    Map<String, Document> allXmlInRoot = plugin.getAllXmlInRoot();
    List<String> list = Arrays.asList("META-INF/uml-support.xml", "META-INF/php-deployment-aware.xml", "META-INF/php-coverage.xml", "META-INF/plugin.xml", "META-INF/liveEdit-support.xml", "META-INF/intellilang-php-support.xml");
    assertTrue(allXmlInRoot.keySet().containsAll(list));
  }

  @Test
  public void getSinceBuild() throws Exception {
    IdeVersion sinceBuild = plugin.getSinceBuild();
    assertNotNull(sinceBuild);
    assertEquals(142, sinceBuild.getBranch());
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
    assertTrue(TestUtils.toStrings(plugin.getDependencies()).containsAll(Arrays.asList("com.intellij.css", "com.intellij.java-i18n", "com.intellij.diagram", "Coverage", "com.jetbrains.plugins.webDeployment", "org.intellij.intelliLang", "com.intellij.plugins.html.instantEditing")));
  }

  @Test
  public void getModuleDependencies() throws Exception {
    assertTrue(TestUtils.toStrings(plugin.getModuleDependencies()).containsAll(Arrays.asList("com.intellij.modules.xml", "com.intellij.modules.ultimate", "com.intellij.modules.coverage")));
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
    assertEquals("JetBrains", plugin.getPluginVendor());
  }

  @Test
  public void getDefinedModules() throws Exception {
    assertTrue(plugin.getDefinedModules().isEmpty());
  }

  @Test
  public void getPluginClassPool() throws Exception {
    Resolver pluginResolver = plugin.getPluginClassPool();
    Collection<String> allClasses = pluginResolver.getAllClasses();

    assertTrue(allClasses.contains("com/intellij/structuralsearch/PhpStructuralSearchProfile"));
    assertTrue(allClasses.contains("com/jetbrains/php/PhpClassHierarchyUtils$8"));
    assertTrue(allClasses.contains("com/jetbrains/php/config/sdk/PhpLocalScriptRunner$2$1"));
    assertTrue(allClasses.contains("com/jetbrains/php/config/interpreters/PhpConfigurationFilePanelGenerator$PhpExpandableConfigurationFilePanel$1"));

    assertEquals(3497, allClasses.size());
  }

  @Test
  public void getLibraryClassPool() throws Exception {

    Resolver libraryResolver = plugin.getLibraryClassPool();
    Collection<String> allClasses = libraryResolver.getAllClasses();

    assertEquals(226, allClasses.size());
  }

}
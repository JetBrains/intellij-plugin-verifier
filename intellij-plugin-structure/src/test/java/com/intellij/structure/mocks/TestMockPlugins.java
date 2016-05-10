package com.intellij.structure.mocks;

import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.impl.domain.PluginDependencyImpl;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;


/**
 * Created by Sergey Patrikeev
 */
public class TestMockPlugins {

  @NotNull
  private static File getMocksDir() {
    return new File("build" + File.separator + "mocks");
  }

  @NotNull
  private static File getMockPlugin(String mockName) {
    File file = new File(getMocksDir(), mockName);
    Assert.assertTrue("mock plugin " + mockName + " is not found in " + file, file.exists());
    return file;
  }

  /*@Test
  public void name() throws Exception {
    File file = new File("/home/user/Documents/intellij-plugin-verifier/for_tests/scalarR.zip");
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    Set<String> allClassesReferencedFromXml = plugin.getAllClassesReferencedFromXml();
    System.out.println(allClassesReferencedFromXml);
  }*/

  private void testMock3Configs(Plugin plugin) {
    assertEquals("http://kotlinlang.org", plugin.getUrl());
    assertEquals("Kotlin", plugin.getPluginName());
    assertEquals("1.0.0-beta-1038-IJ141-17", plugin.getPluginVersion());

    assertEquals("vendor_email", plugin.getVendorEmail());
    assertEquals("http://www.jetbrains.com", plugin.getVendorUrl());
    assertEquals("JetBrains s.r.o.", plugin.getVendor());

    assertEquals("Kotlin language support", plugin.getDescription());
    assertEquals(IdeVersion.createIdeVersion("141.1009.5"), plugin.getSinceBuild());
    assertEquals(IdeVersion.createIdeVersion("141.9999999"), plugin.getUntilBuild());

    assertEquals("change_notes", plugin.getChangeNotes());
  }

  @Test
  public void testMock3() throws Exception {
    //also read classes
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3.jar")), true);
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3jarAsZip.zip")), true);
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3-dir")), true);
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3-lib.zip")), true);
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3-classes")), true);
    testMock3(PluginManager.getInstance().createPlugin(getMockPlugin("mock-plugin3-classes-zip.zip")), true);

    //without reading classes
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3.jar")), false);
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3jarAsZip.zip")), false);
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3-dir")), false);
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3-lib.zip")), false);
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3-classes")), false);
    testMock3(PluginManager.getInstance().createPluginWithEmptyResolver(getMockPlugin("mock-plugin3-classes-zip.zip")), false);
  }

  private void testMock3(Plugin plugin, boolean checkClasses) {
    testMock3Configs(plugin);
    testMock3ClassesFromXml(plugin);
    testMock3ExtensionPoints(plugin);
    testMock3DependenciesAndModules(plugin);
    testMock3OptDescriptors(plugin);
    testMock3UnderlyingDocument(plugin);

    if (checkClasses) {
      testMock3Classes(plugin);
    }
  }

  private void testMock3UnderlyingDocument(Plugin plugin) {
    Document document = plugin.getUnderlyingDocument();
    Element rootElement = document.getRootElement();
    assertNotNull(rootElement);
    assertEquals("idea-plugin", rootElement.getName());
  }

  private void testMock3OptDescriptors(Plugin plugin) {
    Map<String, Plugin> optionalDescriptors = plugin.getOptionalDescriptors();
    assertEquals(3, optionalDescriptors.size());
    assertContains(optionalDescriptors.keySet(), "extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml");

    assertContains(optionalDescriptors.get("extension.xml").getAllClassesReferencedFromXml(), "org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/'));
    assertContains(optionalDescriptors.get("optionals/optional.xml").getAllClassesReferencedFromXml(), "com.intellij.BeanClass".replace('.', '/'));
    assertContains(optionalDescriptors.get("../optionalsDir/otherDirOptional.xml").getAllClassesReferencedFromXml(), "com.intellij.optional.BeanClass".replace('.', '/'));
  }

  private void testMock3DependenciesAndModules(Plugin plugin) {
    assertEquals(4, plugin.getDependencies().size());
    List<PluginDependencyImpl> dependencies = Arrays.asList(new PluginDependencyImpl("JUnit", true), new PluginDependencyImpl("optionalDependency", true), new PluginDependencyImpl("otherDirOptionalDependency", true), new PluginDependencyImpl("mandatoryDependency", false));
    assertEquals(dependencies, plugin.getDependencies());

    //check module dependencies
    assertEquals(Collections.singletonList(new PluginDependencyImpl("com.intellij.modules.mandatoryDependency", false)), plugin.getModuleDependencies());

    assertEquals(new HashSet<String>(Arrays.asList("one_module", "two_module")), plugin.getDefinedModules());
  }

  private void testMock3ExtensionPoints(Plugin plugin) {
    Multimap<String, Element> extensions = plugin.getExtensions();
    assertTrue(extensions.containsKey("com.intellij.referenceImporter"));
    assertTrue(extensions.containsKey("org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider"));
  }

  private void testMock3ClassesFromXml(Plugin plugin) {
    Set<String> set = plugin.getAllClassesReferencedFromXml();
    assertContains(set, "org.jetbrains.kotlin.idea.compiler.JetCompilerManager".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages$Extension".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.js.resolve.diagnostics.DefaultErrorMessagesJs".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.UnfoldReturnToWhenIntention".replace('.', '/'));
    assertFalse(set.contains("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')));
  }

  private void testMock3Classes(Plugin plugin) {
    Set<String> allClasses = plugin.getPluginResolver().getAllClasses();
    assertEquals(4, allClasses.size());
    assertContains(allClasses, "packagename/InFileClassOne", "packagename/ClassOne$ClassOneInnerStatic", "packagename/ClassOne$ClassOneInner", "packagename/InFileClassOne");
  }

  private <T> void assertContains(Collection<T> collection, T... elem) {
    for (T t : elem) {
      if (!collection.contains(t)) {
        System.out.println(collection);
        throw new AssertionError("Collection must contain an element " + t);
      }
    }

  }
}

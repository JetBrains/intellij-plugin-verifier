package com.intellij.structure.mocks;

import com.google.common.collect.Multimap;
import com.intellij.structure.domain.*;
import com.intellij.structure.impl.domain.PluginDependencyImpl;
import com.intellij.structure.impl.domain.PluginImpl;
import com.intellij.structure.impl.domain.PluginProblemImpl;
import com.intellij.structure.resolvers.Resolver;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

    assertEquals("/icons/plugin.png", plugin.getVendorLogoUrl());
    assertNotNull(plugin.getVendorLogo());
    assertEquals(1, plugin.getVendorLogo().length);

    assertEquals("vendor_email", plugin.getVendorEmail());
    assertEquals("http://www.jetbrains.com", plugin.getVendorUrl());
    assertEquals("JetBrains s.r.o.", plugin.getVendor());

    assertEquals("Kotlin language support", plugin.getDescription());
    assertEquals(IdeVersion.createIdeVersion("141.1009.5"), plugin.getSinceBuild());
    assertEquals(IdeVersion.createIdeVersion("141.9999999"), plugin.getUntilBuild());

    assertEquals("change_notes", plugin.getChangeNotes());

    assertContains(plugin.getProblems(),
        new PluginProblemImpl("Plugin dependency missingDependency config-file missingFile specified in META-INF/plugin.xml is not found", PluginProblem.Level.WARNING));
  }

  @Test
  public void testMock3() throws Exception {
    //also read classes
    testMock3(getMockPlugin("mock-plugin3.jar"), "");
    testMock3(getMockPlugin("mock-plugin3jarAsZip.zip"), "");
    testMock3(getMockPlugin("mock-plugin3-dir"), "lib/mock-plugin3.jar");
    testMock3(getMockPlugin("mock-plugin3-lib.zip"), "lib/mock-plugin3.jar");
    testMock3(getMockPlugin("mock-plugin3-classes"), "classes");
    testMock3(getMockPlugin("mock-plugin3-classes-zip.zip"), "classes");
    testMock3(getMockPlugin("mock-plugin3-jar-in-zip.zip"), "");
  }

  private void testMock3IdeCompatibility(Plugin plugin) throws IOException {
//  <idea-version since-build="141.1009.5" until-build="141.9999999"/>
    checkCompatible(plugin, "141.1009.5", true);
    checkCompatible(plugin, "141.99999", true);
    checkCompatible(plugin, "142.0", false);
    checkCompatible(plugin, "141.1009.4", false);
    checkCompatible(plugin, "141", false);
  }

  private void checkCompatible(Plugin plugin, String version, boolean compatible) {
    assertEquals(compatible, plugin.isCompatibleWithIde(IdeVersion.createIdeVersion(version)));
  }

  private void testMock3(File pluginFile, String... classPath) throws Exception {
    Plugin plugin = PluginManager.getInstance().createPlugin(pluginFile);
    assertEquals(pluginFile, plugin.getPluginFile());
    testMock3Classes(plugin, classPath);
    testMock3Configs(plugin);
    testMock3ClassesFromXml(plugin);
    testMock3ExtensionPoints(plugin);
    testMock3DependenciesAndModules(plugin);
    testMock3OptDescriptors(plugin);
    testMock3UnderlyingDocument(plugin);
    testMock3IdeCompatibility(plugin);
  }

  private void testMock3UnderlyingDocument(Plugin plugin) {
    Document document = plugin.getUnderlyingDocument();
    Element rootElement = document.getRootElement();
    assertNotNull(rootElement);
    assertEquals("idea-plugin", rootElement.getName());
  }

  private void testMock3OptDescriptors(Plugin plugin) {
    Map<String, Plugin> optionalDescriptors = plugin.getOptionalDescriptors();
    assertEquals(4, optionalDescriptors.size());
    assertContains(optionalDescriptors.keySet(), "extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml", "/META-INF/referencedFromRoot.xml");

    assertContains(optionalDescriptors.get("extension.xml").getAllClassesReferencedFromXml(), "org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/'));
    assertContains(optionalDescriptors.get("optionals/optional.xml").getAllClassesReferencedFromXml(), "com.intellij.BeanClass".replace('.', '/'));
    assertContains(optionalDescriptors.get("../optionalsDir/otherDirOptional.xml").getAllClassesReferencedFromXml(), "com.intellij.optional.BeanClass".replace('.', '/'));
  }

  private void testMock3DependenciesAndModules(Plugin plugin) {
    assertEquals(6, plugin.getDependencies().size());
    List<PluginDependencyImpl> dependencies = Arrays.asList(new PluginDependencyImpl("JUnit", true), new PluginDependencyImpl("optionalDependency", true), new PluginDependencyImpl("otherDirOptionalDependency", true), new PluginDependencyImpl("mandatoryDependency", false), new PluginDependencyImpl("referenceFromRoot", true), new PluginDependencyImpl("missingDependency", true));
    assertContains(plugin.getDependencies(), (PluginDependency[]) dependencies.toArray());

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

  private void testMock3Classes(Plugin plugin, String... classPath) throws Exception {
    Resolver resolver = Resolver.createPluginResolver(plugin);
    try {
      Set<String> allClasses = resolver.getAllClasses();
      assertEquals(4, allClasses.size());
      assertContains(allClasses, "packagename/InFileClassOne", "packagename/ClassOne$ClassOneInnerStatic", "packagename/ClassOne$ClassOneInner", "packagename/InFileClassOne");

      File extracted = getExtractedPath(resolver).getCanonicalFile();
      assertClassPath(resolver, extracted, classPath);
    } finally {
      resolver.close();
    }
  }

  private void assertClassPath(Resolver resolver, File extracted, String[] classPath) throws IOException {
    List<File> pluginClassPath = resolver.getClassPath();
    for (String cp : classPath) {
      File shouldBe = new File(extracted, cp);
      boolean found = false;
      for (File pcp : pluginClassPath) {
        if (pcp.getCanonicalPath().equals(shouldBe.getCanonicalPath())) {
          found = true;
        }
      }
      Assert.assertTrue("The class path " + shouldBe + " is not found", found);
    }
  }

  private File getExtractedPath(Resolver resolver) throws NoSuchFieldException, IllegalAccessException {
    Field field = resolver.getClass().getDeclaredField("myPluginFile");
    field.setAccessible(true);
    return (File) field.get(resolver);
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

package com.intellij.structure.mocks;

import com.google.common.collect.Multimap;
import com.intellij.structure.domain.*;
import com.intellij.structure.impl.domain.PluginDependencyImpl;
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
  private static File getMockPluginFile(String mockName) {
    File pluginFile = new File("build" + File.separator + "mocks", mockName);
    if (!pluginFile.exists()) {
      pluginFile = new File("intellij-plugin-structure" + File.separator + "tests", pluginFile.getPath());
    }
    Assert.assertTrue("mock plugin " + mockName + " is not found in " + pluginFile.getAbsolutePath(), pluginFile.exists());
    return pluginFile;
  }


  private void testMockConfigs(Plugin plugin) {
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

  private void testMockWarnings(List<PluginProblem> problems) {
    assertContains(problems,
        new PluginProblemImpl("Plugin dependency missingDependency config-file missingFile specified in META-INF/plugin.xml is not found", PluginProblem.Level.WARNING));
  }

  @Test
  public void jarInZip() throws Exception {
    testMock("mock-plugin-jar-in-zip.zip", "");
  }

  @Test
  public void classesInZip() throws Exception {
    testMock("mock-plugin-classes-zip.zip", "classes");
  }

  @Test
  public void justClasses() throws Exception {
    testMock("mock-plugin-classes", "classes");
  }

  @Test
  public void libInZip() throws Exception {
    testMock("mock-plugin-lib.zip", "lib/mock-plugin.jar");
  }

  @Test
  public void fromDir() throws Exception {
    testMock("mock-plugin-dir", "lib/mock-plugin.jar");
  }

  @Test
  public void jarAsZip() throws Exception {
    testMock("mock-pluginJarAsZip.zip", "");

  }

  @Test
  public void jar() throws Exception {
    testMock("mock-plugin.jar", "");
  }

  private void testMockIdeCompatibility(Plugin plugin) throws IOException {
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

  private void testMock(String pluginPath, String... classPath) throws Exception {
    File pluginFile = getMockPluginFile(pluginPath);
    PluginCreationResult pluginCreationResult = PluginManager.getInstance().createPlugin(pluginFile);
    assertTrue(pluginCreationResult instanceof PluginCreationSuccess);
    Plugin plugin = ((PluginCreationSuccess) pluginCreationResult).getPlugin();
    assertEquals(pluginFile, plugin.getPluginFile());
    testMockClasses(plugin, classPath);
    testMockConfigs(plugin);
    testMockWarnings(((PluginCreationSuccess) pluginCreationResult).getWarnings());
    testMockClassesFromXml(plugin);
    testMockExtensionPoints(plugin);
    testMockDependenciesAndModules(plugin);
    testMockOptDescriptors(plugin);
    testMockUnderlyingDocument(plugin);
    testMockIdeCompatibility(plugin);
  }

  private void testMockUnderlyingDocument(Plugin plugin) {
    Document document = plugin.getUnderlyingDocument();
    Element rootElement = document.getRootElement();
    assertNotNull(rootElement);
    assertEquals("idea-plugin", rootElement.getName());
  }

  private void testMockOptDescriptors(Plugin plugin) {
    Map<String, Plugin> optionalDescriptors = plugin.getOptionalDescriptors();
    assertEquals(4, optionalDescriptors.size());
    assertContains(optionalDescriptors.keySet(), "extension.xml", "optionals/optional.xml", "../optionalsDir/otherDirOptional.xml", "/META-INF/referencedFromRoot.xml");

    assertContains(optionalDescriptors.get("extension.xml").getAllClassesReferencedFromXml(), "org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/'));
    assertContains(optionalDescriptors.get("optionals/optional.xml").getAllClassesReferencedFromXml(), "com.intellij.BeanClass".replace('.', '/'));
    assertContains(optionalDescriptors.get("../optionalsDir/otherDirOptional.xml").getAllClassesReferencedFromXml(), "com.intellij.optional.BeanClass".replace('.', '/'));
  }

  private void testMockDependenciesAndModules(Plugin plugin) {
    assertEquals(6, plugin.getDependencies().size());
    List<PluginDependencyImpl> dependencies = Arrays.asList(new PluginDependencyImpl("JUnit", true), new PluginDependencyImpl("optionalDependency", true), new PluginDependencyImpl("otherDirOptionalDependency", true), new PluginDependencyImpl("mandatoryDependency", false), new PluginDependencyImpl("referenceFromRoot", true), new PluginDependencyImpl("missingDependency", true));
    assertContains(plugin.getDependencies(), (PluginDependency[]) dependencies.toArray());

    //check module dependencies
    assertEquals(Collections.singletonList(new PluginDependencyImpl("com.intellij.modules.mandatoryDependency", false)), plugin.getModuleDependencies());

    assertEquals(new HashSet<String>(Arrays.asList("one_module", "two_module")), plugin.getDefinedModules());
  }

  private void testMockExtensionPoints(Plugin plugin) {
    Multimap<String, Element> extensions = plugin.getExtensions();
    assertTrue(extensions.containsKey("com.intellij.referenceImporter"));
    assertTrue(extensions.containsKey("org.intellij.scala.scalaTestDefaultWorkingDirectoryProvider"));
  }

  private void testMockClassesFromXml(Plugin plugin) {
    Set<String> set = plugin.getAllClassesReferencedFromXml();
    assertContains(set, "org.jetbrains.kotlin.idea.compiler.JetCompilerManager".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages$Extension".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.js.resolve.diagnostics.DefaultErrorMessagesJs".replace('.', '/'));
    assertContains(set, "org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.UnfoldReturnToWhenIntention".replace('.', '/'));
    assertFalse(set.contains("org.jetbrains.plugins.scala.project.maven.MavenWorkingDirectoryProviderImpl".replace('.', '/')));
  }

  private void testMockClasses(Plugin plugin, String... classPath) throws Exception {
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

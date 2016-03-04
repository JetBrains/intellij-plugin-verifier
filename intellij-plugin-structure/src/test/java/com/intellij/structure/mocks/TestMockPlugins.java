package com.intellij.structure.mocks;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.impl.domain.PluginDependencyImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;


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

  @Test
  public void testMock1() throws Exception {
    File file = getMockPlugin("mock-plugin1-1.0.jar");
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    assertEquals(file, plugin.getPluginPath());

//    assertEquals("format_version_attr", plugin.getFormatVersion());
//    assertEquals("true", plugin.useIdeaClassLoader());

    assertEquals("my_url_attr", plugin.getUrl());
    assertEquals("Mock name 1", plugin.getPluginName());
    assertEquals("1.0", plugin.getPluginVersion());

    assertEquals("nonono.com", plugin.getVendorEmail());
    assertEquals("http://www.HornsAndHooves.com", plugin.getVendorUrl());
//    assertEquals("logo_path", plugin.getVendorLogoPath());
    assertEquals("Vendor", plugin.getVendor());

    assertEquals("description", plugin.getDescription());
    assertEquals(IdeVersion.createIdeVersion("131"), plugin.getSinceBuild());
    assertEquals(IdeVersion.createIdeVersion("999"), plugin.getUntilBuild());

    assertEquals("change_notes", plugin.getChangeNotes());
//    assertEquals("my_category", plugin.getCategory());

    assertEquals(Arrays.asList(new PluginDependencyImpl("dependent.id", false), new PluginDependencyImpl("opt.dependent.id", true)), plugin.getDependencies());
    assertEquals(Collections.singletonList(new PluginDependencyImpl("com.intellij.modules.moduledependency", false)), plugin.getModuleDependencies());

//    assertEquals("my_resourceBundle", plugin.getResourceBundle());
    assertEquals(new HashSet<String>(Arrays.asList("one_module", "two_module")), plugin.getDefinedModules());

  }

  @Test
  public void testMock2() throws Exception {
    File file = getMockPlugin("mock-plugin2");
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    assertEquals(4, plugin.getPluginResolver().getAllClasses().size());
  }

/*
  @Test
  public void testMock3() throws Exception {
    File file = getMockPlugin("mock-plugin3");
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    assertEquals(4, plugin.getPluginResolver().getAllClasses().size());
  }
*/
}

package com.intellij.structure.mocks;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.utils.TestUtils;
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

  @Test
  public void test1() throws Exception {
    File file = TestUtils.getMockPlugin(1);
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    assertEquals(file, plugin.getPluginPath());

//    assertEquals("format_version_attr", plugin.getFormatVersion());
//    assertEquals("true", plugin.useIdeaClassLoader());

    assertEquals("my_url_attr", plugin.getUrl());
    assertEquals("Mock name 1", plugin.getPluginName());
    assertEquals("1.0", plugin.getPluginVersion());

    assertEquals("nonono.com", plugin.getVendorEmail());
    assertEquals("http://www.HornsAndHooves.com", plugin.getVendorUrl());
//    assertEquals("logo_path", plugin.getVendorLogoPath()); //TODO: change with InputStream
    assertEquals("Vendor", plugin.getVendor());

    assertEquals("description", plugin.getDescription());
    assertEquals(IdeVersion.createIdeVersion("131"), plugin.getSinceBuild());
    assertEquals(IdeVersion.createIdeVersion("999"), plugin.getUntilBuild());

    assertEquals("change_notes", plugin.getChangeNotes());
//    assertEquals("my_category", plugin.getCategory());

    assertEquals(Arrays.asList(new PluginDependency("dependent.id", false), new PluginDependency("opt.dependent.id", true)), plugin.getDependencies());
    assertEquals(Collections.singletonList(new PluginDependency("com.intellij.modules.moduledependency", false)), plugin.getModuleDependencies());

//    assertEquals("my_resourceBundle", plugin.getResourceBundle());
    assertEquals(new HashSet<String>(Arrays.asList("one_module", "two_module")), plugin.getDefinedModules());

  }

}

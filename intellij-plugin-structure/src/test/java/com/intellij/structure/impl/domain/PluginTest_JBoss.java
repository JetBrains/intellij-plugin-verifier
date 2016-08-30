package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.impl.utils.PluginExtractor;
import com.intellij.structure.utils.TestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * This plugin contains a broken .xml file forge-idea.zip!/org.jboss.forge.plugin.idea/lib/resources-api-3.1.1.Final.jar!/META-INF/beans.xml
 * an attempt to read the file throws an {@link org.xml.sax.SAXParseException}.
 * But this .xml is not relevant to plugin. So creation of a plugin must pass without an exception.
 * <p>
 * Created by Sergey Patrikeev
 */
public class PluginTest_JBoss {

  @Test
  public void downloadAndOpenAsZip() throws Exception {
    Plugin plugin = PluginManager.getInstance().createPlugin(TestUtils.downloadPlugin(TestUtils.JBOSS, "jboss.zip"));
    checkPlugin(plugin);
  }

  @Test
  public void downloadAndOpenAsDir() throws Exception {
    File archive = TestUtils.downloadPlugin(TestUtils.JBOSS, "jboss.zip");
    Plugin plugin = PluginManager.getInstance().createPlugin(archive);
    File asDir = PluginExtractor.extractPlugin(plugin, archive);
    try {
      Plugin pluginDir = PluginManager.getInstance().createPlugin(asDir);
      checkPlugin(pluginDir);
    } finally {
      FileUtils.forceDelete(asDir);
    }
  }

  private void checkPlugin(Plugin plugin) {
    Assert.assertEquals("org.jboss.forge.plugin.idea", plugin.getPluginId());
    Assert.assertEquals("JBoss Forge IDEA Plugin", plugin.getPluginName());
    Assert.assertEquals("1.1.11.Final", plugin.getPluginVersion());
  }


}

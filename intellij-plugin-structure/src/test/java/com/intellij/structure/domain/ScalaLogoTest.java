package com.intellij.structure.domain;

import com.intellij.structure.utils.TestUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Patrikeev
 */
public class ScalaLogoTest {

  private static Plugin plugin;

  @Before
  public void setUp() throws Exception {
    if (plugin != null) return;

    File pluginFile = TestUtils.downloadPlugin(TestUtils.SCALA_URL, "scala.zip");

    plugin = PluginManager.getIdeaPluginManager().createPlugin(pluginFile);
  }

  @Test
  public void getLogoTest() throws IOException {
    Document pluginXml = plugin.getPluginXml();
    assertNotNull(pluginXml);
    Element root = pluginXml.getRootElement();
    assertNotNull(root);
    Element vendor = root.getChild("vendor");
    assertNotNull(vendor);
    String logo = vendor.getAttributeValue("logo");
    assertNotNull(logo);

    InputStream file = plugin.getResourceFile(logo);
    assertNotNull(file);
    file.close();

  }
}

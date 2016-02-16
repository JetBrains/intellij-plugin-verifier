package com.intellij.structure.domain;

import com.intellij.structure.utils.TestUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Patrikeev
 */
public class GetResourceTest {

  private Plugin getPlugin(String s, String n) throws IOException {
    return PluginManager.getIdeaPluginManager().createPlugin(TestUtils.downloadPlugin(s, n));
  }

  @Test
  public void getScalaLogo() throws IOException {
    Plugin plugin = getPlugin(TestUtils.SCALA_URL, "scala.zip");

    Document pluginXml = plugin.getPluginXml();
    assertNotNull(pluginXml);
    Element root = pluginXml.getRootElement();
    assertNotNull(root);
    Element vendor = root.getChild("vendor");
    assertNotNull(vendor);
    String logo = vendor.getAttributeValue("logo");
    assertNotNull(logo);

    test(plugin, logo);
  }

  @Test
  public void getGoSomeFile() throws Exception {
    Plugin plugin = getPlugin(TestUtils.GO_URL, "go.zip");
    test(plugin, "META-INF/plugin.xml");
    test(plugin, "META-INF/java-deps.xml");
    test(plugin, "classes/com/goide/util/GoUtil.class");
  }

  @Test
  public void getRubySomeFile() throws Exception {
    Plugin plugin = getPlugin(TestUtils.RUBY_URL, "ruby.zip");
    test(plugin, "META-INF/plugin.xml");
    test(plugin, "com/intellij/database/rails/NavigateToDatabaseProvider$1.class");
  }

  private void test(Plugin p, String file) throws IOException {
    InputStream is = p.getResourceFile(file);
    assertNotNull(is);
    is.close();
  }
}

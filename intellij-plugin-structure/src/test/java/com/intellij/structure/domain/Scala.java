package com.intellij.structure.domain;

import com.intellij.structure.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author Sergey Patrikeev
 */
public class Scala {
  @Test
  public void testLogo() throws Exception {
    File pluginFile = TestUtils.downloadPlugin(TestUtils.SCALA_URL, "scala-plugin-logo-test.zip");
    Plugin plugin = PluginManager.getInstance().createPlugin(pluginFile);
    byte[] vendorLogo = plugin.getVendorLogo();
    Assert.assertNotNull(vendorLogo);
    Assert.assertEquals(488, vendorLogo.length);
  }
}

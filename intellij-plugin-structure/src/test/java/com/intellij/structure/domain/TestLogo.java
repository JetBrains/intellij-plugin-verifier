package com.intellij.structure.domain;

import com.intellij.structure.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class TestLogo {

  @Test
  public void testGerritLogo() throws Exception {
    //gerrit plugin specifies logo path to be "/icons/gerrit.png"
    //but it is actually located in <plugin_root>/classes/icons/gerrit.png.
    testPluginLogo(TestUtils.downloadPlugin(TestUtils.GERRIT, "gerrit.zip"), 471);
  }

  private void testPluginLogo(File file, int expectedSize) throws IOException {
    Plugin plugin = PluginManager.getInstance().createPlugin(file);
    byte[] vendorLogo = plugin.getVendorLogo();
    Assert.assertNotNull(vendorLogo);
    Assert.assertEquals(expectedSize, vendorLogo.length);
  }
}

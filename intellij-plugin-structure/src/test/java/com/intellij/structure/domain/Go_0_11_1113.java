package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.ClassFileUtilTest;
import com.intellij.structure.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

/**
 * @author Sergey Patrikeev
 */
public class Go_0_11_1113 {

  private static Plugin plugin;

  @Before
  public void setUp() throws Exception {
    if (plugin != null) return;

    File pluginFile = TestUtils.downloadPlugin(TestUtils.GO_URL, "go-plugin.zip");

    plugin = PluginManager.getPluginManager().createPlugin(pluginFile);
  }

  @Test
  public void testFoundClass() throws Exception {
    Resolver pool = plugin.getPluginClassPool();
    Set<String> allClasses = ClassFileUtilTest.loadAllGoClassNames();
    for (String clazz : allClasses) {
      assertNotNull(pool.findClass(clazz));
    }
  }
}
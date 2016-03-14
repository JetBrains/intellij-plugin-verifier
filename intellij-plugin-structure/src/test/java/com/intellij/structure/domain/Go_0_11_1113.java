package com.intellij.structure.domain;

import com.intellij.structure.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class Go_0_11_1113 {

  private static Plugin plugin;

  @Before
  public void setUp() throws Exception {
    if (plugin != null) return;

    File pluginFile = TestUtils.downloadPlugin(TestUtils.GO_URL, "go-plugin.zip");

    plugin = PluginManager.getInstance().createPlugin(pluginFile);
  }

  @Test
  public void testClassesFound() throws Exception {
    Collection<String> allClasses = plugin.getPluginResolver().getAllClasses();
    assertEquals(allClasses.size(), new HashSet<String>(allClasses).size()); //no duplicates?
    assertEquals(1004, allClasses.size());

    Set<String> foundNames = new HashSet<String>();

    for (String name : allClasses) {
      ClassNode node = plugin.getPluginResolver().findClass(name);
      assertNotNull(node);
      foundNames.add(node.name);
    }

    Set<String> needNames = new HashSet<String>(Arrays.asList("com/goide/runconfig/testing/coverage/GoCoverageRunner$1$1", "com/goide/psi/impl/GoRangeClauseImpl"));

    needNames.removeAll(foundNames);

    assertTrue("not found names" + needNames, needNames.isEmpty());
  }

}
package com.intellij.structure.utils;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Sergey Patrikeev
 */
public class ClassFileUtilTest {

  public static Set<String> loadAllGoClassNames() throws IOException {
    InputStream inputStream = ClassFileUtilTest.class.getResourceAsStream("goPluginClasses.txt");
    String s = IOUtils.toString(inputStream);
    Set<String> set = new HashSet<String>();
    Collections.addAll(set, s.split(" "));
    return set;
  }

  @Test
  public void extractFromBytes() throws Exception {
    File goPluginFile = TestUtils.downloadPlugin(TestUtils.GO_URL, "go-plugin.zip");
    Plugin plugin = PluginManager.getPluginManager().createPlugin(goPluginFile);
    Collection<String> allClasses = plugin.getPluginClassPool().getAllClasses();
    assertTrue(allClasses.size() == 951);

    Set<String> foundNames = new HashSet<String>();

    for (String name : allClasses) {
      ClassNode node = plugin.getPluginClassPool().findClass(name);
      assertNotNull(node);
      foundNames.add(node.name);
    }

    Set<String> needNames = new HashSet<String>(Arrays.asList("com/goide/runconfig/testing/coverage/GoCoverageRunner$1$1", "com/goide/psi/impl/GoRangeClauseImpl"));

    needNames.removeAll(foundNames);

    assertTrue("not found names" + needNames, needNames.isEmpty());
  }
}
package com.intellij.structure.utils;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.impl.utils.ClassFileUtil;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

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
    Plugin plugin = PluginManager.getIdeaPluginManager().createPlugin(goPluginFile);
    Collection<String> allClasses = plugin.getPluginClassPool().getAllClasses();

    Set<String> foundNames = new HashSet<String>();

    for (String name : allClasses) {
      ClassFile file = plugin.getPluginClassPool().findClass(name);
      assertNotNull(file);
      foundNames.add(ClassFileUtil.extractFromBytes(file.getBytecode()));
    }

    Set<String> needNames = loadAllGoClassNames();

    assertEquals(foundNames.size(), needNames.size());

    needNames.removeAll(foundNames);

    assertTrue(needNames.isEmpty());
  }
}
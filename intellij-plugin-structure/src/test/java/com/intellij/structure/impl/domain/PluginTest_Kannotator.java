package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.TestUtils;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Indicator test that ASM has a bug with JDK 1.5 compatibility
 *
 * @author Sergey Patrikeev
 */
public class PluginTest_Kannotator {

  @Test
  public void simple() throws Exception {
    File pluginFile = TestUtils.downloadPlugin(TestUtils.KANNOTATOR, "kannotator.zip");
    Plugin plugin = PluginManager.getInstance().createPlugin(pluginFile);

    Resolver pluginResolver = Resolver.createCacheResolver(plugin.getPluginResolver());

    List<Exception> exceptions = new ArrayList<Exception>();
    Set<String> brokenClasses = new HashSet<String>();

    for (String clazz : pluginResolver.getAllClasses()) {
      try {
        ClassNode node = pluginResolver.findClass(clazz);
        assertNotNull(node);
        assertNotNull(node.name);
      } catch (IOException e) {
        brokenClasses.add(clazz);
        exceptions.add(e);
      }
    }

    System.out.println(brokenClasses);
    assertTrue(brokenClasses.contains("checkers/regex/RegexUtil"));
    assertTrue(brokenClasses.contains("checkers/nullness/NullnessUtils"));
    assertEquals(2, exceptions.size());
  }
}
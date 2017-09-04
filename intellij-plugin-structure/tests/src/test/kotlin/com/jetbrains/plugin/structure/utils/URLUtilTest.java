package com.jetbrains.plugin.structure.utils;

import com.jetbrains.plugin.structure.impl.utils.xml.URLUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Patrikeev
 */
public class URLUtilTest {

  @Test
  public void splitJarUrl() {
    String[] strings = URLUtil.splitUrl("jar:jar:file:/tmp/plugin-verifier-test-data-temp-cache/ruby-plugin.zip!/ruby/lib/ruby.jar!/META-INF/plugin.xml");
    assertEquals("/tmp/plugin-verifier-test-data-temp-cache/ruby-plugin.zip", strings[0]);
    assertEquals("ruby/lib/ruby.jar", strings[1]);
    assertEquals("META-INF/plugin.xml", strings[2]);

  }
}
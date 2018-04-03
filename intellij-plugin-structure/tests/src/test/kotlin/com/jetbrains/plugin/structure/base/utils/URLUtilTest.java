package com.jetbrains.plugin.structure.base.utils;

import com.jetbrains.plugin.structure.intellij.utils.ThreeState;
import com.jetbrains.plugin.structure.intellij.utils.URLUtil;
import kotlin.Pair;
import kotlin.jvm.JvmField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import static org.junit.Assert.*;

public class URLUtilTest {

  @JvmField
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static void assertPair(Pair<String, String> pair,
                                 String expected1,
                                 String expected2) {
    assertNotNull(pair);
    assertEquals(expected1, pair.getFirst());
    assertEquals(expected2, pair.getSecond());
  }

  @Test
  public void testJarUrlSplitter() {
    assertNull(URLUtil.splitJarUrl("/path/to/jar.jar/resource.xml"));
    assertNull(URLUtil.splitJarUrl("/path/to/jar.jar!resource.xml"));

    assertPair(URLUtil.splitJarUrl("/path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");

    assertPair(URLUtil.splitJarUrl("file:/path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");
    assertPair(URLUtil.splitJarUrl("file:///path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");

    assertPair(URLUtil.splitJarUrl("jar:/path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");

    assertPair(URLUtil.splitJarUrl("jar:file:/path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");
    assertPair(URLUtil.splitJarUrl("jar:file:///path/to/jar.jar!/resource.xml"), "/path/to/jar.jar", "resource.xml");

    if (System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows")) {
      assertPair(URLUtil.splitJarUrl("file:/C:/path/to/jar.jar!/resource.xml"), "C:/path/to/jar.jar", "resource.xml");
      assertPair(URLUtil.splitJarUrl("file:////HOST/share/path/to/jar.jar!/resource.xml"), "//HOST/share/path/to/jar.jar", "resource.xml");
    } else {
      assertPair(URLUtil.splitJarUrl("file:/C:/path/to/jar.jar!/resource.xml"), "/C:/path/to/jar.jar", "resource.xml");
      assertPair(URLUtil.splitJarUrl("file:////HOST/share/path/to/jar.jar!/resource.xml"), "/HOST/share/path/to/jar.jar", "resource.xml");
    }

    assertPair(URLUtil.splitJarUrl("file:/path/to/jar%20with%20spaces.jar!/resource.xml"), "/path/to/jar with spaces.jar", "resource.xml");

    assertPair(URLUtil.splitJarUrl("file:/path/to/jar with spaces.jar!/resource.xml"), "/path/to/jar with spaces.jar", "resource.xml");
  }

  @Test
  public void resourceExistsForLocalFile() throws Exception {
    File dir = tempFolder.newFolder("UrlUtilTest");
    File existingFile = new File(dir, "a.txt");
    assertTrue(existingFile.createNewFile());
    assertEquals(ThreeState.YES, URLUtil.resourceExists(existingFile.toURI().toURL()));
    File nonExistingFile = new File(dir, "b.txt");
    assertEquals(ThreeState.NO, URLUtil.resourceExists(nonExistingFile.toURI().toURL()));
  }

  @Test
  public void resourceExistsForRemoteUrl() throws Exception {
    assertEquals(ThreeState.UNSURE, URLUtil.resourceExists(new URL("http://jetbrains.com")));
  }

  @Test
  public void resourceExistsForFileInJar() throws Exception {
    URL stringUrl = String.class.getResource("String.class");
    assertEquals(ThreeState.YES, URLUtil.resourceExists(stringUrl));
    URL xxxUrl = new URL(stringUrl.getProtocol(), "", -1, stringUrl.getPath() + "/xxx");
    assertEquals(ThreeState.NO, URLUtil.resourceExists(xxxUrl));
  }
}
package com.intellij.structure.utils;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class TestUtils {
  public static final String PHP_URL = "https://plugins.jetbrains.com/plugin/download?pr=&updateId=22827";
  public static final String RUBY_URL = "https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=22893";
  public static final String GO_URL = "https://plugins.jetbrains.com/plugin/download?pr=&updateId=23807";
  public static final String SCALA_URL = "https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=23664";

  @NotNull
  private static URL getFinalUrl(@NotNull String startUrl) throws IOException {
    URL url = new URL(startUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setInstanceFollowRedirects(false);

    if (connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {
      return url;
    }

    if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP
        || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
      String location = connection.getHeaderField("Location");
      if (location != null) {
        return new URL(location);
      }
    }
    return url;
  }

  public static void downloadFile(String loadUrl, File dest) throws IOException {
    if (dest.exists()) {
      return;
    }
    URL url = TestUtils.getFinalUrl(loadUrl);

    System.out.println("Downloading " + url + "...");
    FileUtils.copyURLToFile(url, dest);
    System.out.println("Loaded to " + dest.getAbsolutePath());
  }

  public static <T> Set<String> toStrings(List<T> list) {
    Set<String> set = new HashSet<String>();
    for (T t : list) {
      set.add(t.toString());
    }
    return set;
  }

  public static File getFileForDownload(String name) throws IOException {
    return new File(getTempRoot(), name);
  }

  public static File getTempRoot() {
    final File fetchRoot = new File(System.getProperty("java.io.tmpdir"), "plugin-verifier-test-data-temp-cache");
    //noinspection ResultOfMethodCallIgnored
    fetchRoot.mkdirs();

    return fetchRoot;
  }

  @NotNull
  public static File downloadPlugin(String pluginUrl, String pluginFileName) throws IOException {
    File pluginFile = getFileForDownload(pluginFileName);
    downloadFile(pluginUrl, pluginFile);
    return pluginFile;
  }

  @NotNull
  public static File getMockPlugin(int mockNumber) {
    File file = new File("build/mocks/mock-plugin" + mockNumber + "-1.0.jar");
    Assert.assertTrue("mock plugin " + mockNumber + " is not found in " + file, file.exists());
    return file;
  }
}

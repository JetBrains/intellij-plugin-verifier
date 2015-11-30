package com.intellij.structure.tests;

import com.intellij.structure.domain.IdeaPlugin;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Patrikeev
 */
public class DependsTest {

  //Plugin download url to number of .xml should be found
  public static final Map<String, Integer> MAP = new HashMap<String, Integer>();

  static {
    MAP.put("https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21835", 14); //Kotlin 1.0.0-beta-1038-IJ141-17
    MAP.put("https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21782", 16); //Scala 1.9.4
  }

  private static File getFetchRoot() {
    final File fetchRoot = new File(System.getProperty("java.io.tmpdir"), "plugin-verifier-test-data-temp-cache");
    //noinspection ResultOfMethodCallIgnored
    fetchRoot.mkdirs();

    return fetchRoot;
  }

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

  @Test
  public void testName() throws Exception {
    int idx = 0;
    for (Map.Entry<String, Integer> entry : MAP.entrySet()) {
      URL url = getFinalUrl(entry.getKey());
      File destination = new File(getFetchRoot(), "plugin" + (idx++) + ".zip");
      if (!destination.exists()) {
        System.out.println("Downloading " + url + "...");
        FileUtils.copyURLToFile(url, destination);
        System.out.println("Loaded to " + destination.getAbsolutePath());
      }
      System.out.println("Verifying...");

      IdeaPlugin ideaPlugin = IdeaPlugin.createIdeaPlugin(destination);
      Map<String, Document> xmlDocumentsInRoot = ideaPlugin.getXmlDocumentsInRoot();
      int size = xmlDocumentsInRoot.size();

      Assert.assertEquals((int) entry.getValue(), size);
    }

  }

}

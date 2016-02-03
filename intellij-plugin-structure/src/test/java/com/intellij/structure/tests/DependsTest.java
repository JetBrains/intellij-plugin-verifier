package com.intellij.structure.tests;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.utils.TestUtils;
import org.jdom.Document;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Patrikeev
 */
public class DependsTest {

  //Plugin download url to number of .xml should be found
  private static final Map<String, Integer> MAP = new HashMap<String, Integer>();

  static {
    MAP.put("https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21835", 14); //Kotlin 1.0.0-beta-1038-IJ141-17
    MAP.put("https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21782", 16); //Scala 1.9.4
  }

  @Test
  public void testDepends() throws Exception {
    int idx = 0;
    for (Map.Entry<String, Integer> entry : MAP.entrySet()) {

      File destination = TestUtils.getFileForDownload("plugin" + (idx++) + ".zip");
      TestUtils.downloadFile(entry.getKey(), destination);

      System.out.println("Verifying...");

      Plugin ideaPlugin = PluginManager.getIdeaPluginManager().createPlugin(destination);
      Map<String, Document> xmlDocumentsInRoot = ideaPlugin.getAllXmlInRoot();
      int size = xmlDocumentsInRoot.size();

      Assert.assertEquals((int) entry.getValue(), size);
    }

  }

}

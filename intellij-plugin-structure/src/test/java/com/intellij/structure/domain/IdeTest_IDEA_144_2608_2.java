package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.IdeaManager;
import com.intellij.structure.impl.domain.JdkManager;
import com.intellij.structure.utils.DummyPlugin;
import com.intellij.structure.utils.TestUtils;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sergey Patrikeev
 */
public class IdeTest_IDEA_144_2608_2 {


  private static final String IDEA_144_LOAD_URL = "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/144.2608.2/ideaIU-144.2608.2.zip";
  private Ide ide;
  private IdeRuntime runtime;

  @Before
  public void setUp() throws Exception {

    File fileForDownload = TestUtils.getFileForDownload("Idea144.2608.2.zip");
    TestUtils.downloadFile(IDEA_144_LOAD_URL, fileForDownload);


    File ideaDir = new File(TestUtils.getTempRoot(), "Idea144.2608.2");
    if (!ideaDir.isDirectory()) {
      ideaDir.mkdirs();

      ZipUnArchiver archiver = new ZipUnArchiver(fileForDownload);
      archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_WARN, ""));
      archiver.setDestDirectory(ideaDir);
      archiver.extract();
    }

    String jdkPath = System.getenv("JAVA_HOME");
    if (jdkPath == null) {
      jdkPath = "/usr/lib/jvm/java-6-oracle";
    }


    runtime = JdkManager.getInstance().createRuntime(new File(jdkPath));
    ide = IdeaManager.getInstance().createIde(ideaDir, runtime);
  }


  @Test
  public void getVersion() throws Exception {
    assertEquals("IU-144.2608.2", ide.getVersion());
  }

  @Test
  public void updateVersion() throws Exception {
    String v = ide.getVersion();
    ide.updateVersion("IU-140.40.40");
    assertEquals("IU-140.40.40", ide.getVersion());
    ide.updateVersion(v);
    assertEquals(v, ide.getVersion());
  }

  @Test
  public void addCustomPlugin() throws Exception {
    assertTrue(ide.getCustomPlugins().isEmpty());
    Plugin plugin = new DummyPlugin();
    ide.addCustomPlugin(plugin);
    assertTrue(!ide.getCustomPlugins().isEmpty());
    assertTrue(ide.getCustomPlugins().get(0) == plugin);

  }

  @Test
  public void getBundledPlugins() throws Exception {


  }

  @Test
  public void getPluginById() throws Exception {

  }

  @Test
  public void getPluginByModule() throws Exception {

  }

  @Test
  public void getResolver() throws Exception {

  }
}
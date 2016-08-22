package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class TestRepositoryManager {

  @Test
  public void updatesOfPlugin() throws Exception {
    assertTrue(RepositoryManager.INSTANCE.getAllCompatibleUpdatesOfPlugin(getIdeVersion(), "org.jetbrains.kotlin").size() > 0);
  }

  @Test
  public void lastUpdate() throws Exception {
    UpdateInfo info = RepositoryManager.INSTANCE.getLastCompatibleUpdateOfPlugin(getIdeVersion(), "org.jetbrains.kotlin");
    assertNotNull(info);
    assertTrue(info.getUpdateId() > 20000);
  }

  @NotNull
  private IdeVersion getIdeVersion() {
    return IdeVersion.createIdeVersion("IU-162.1132.10");
  }

  @Test
  public void downloadNonExistentPlugin() throws Exception {
    File id = RepositoryManager.INSTANCE.getPluginFile(-1000);
    assertNull(id);
  }

  @Test
  public void downloadExistentPlugin() throws Exception {
    File info = RepositoryManager.INSTANCE.getPluginFile(25128); //.gitignore 1.3.3
    assertNotNull(info);
    assertTrue(info.length() > 0);
  }

}

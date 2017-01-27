package com.jetbrains.pluginverifier.results;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.repository.IFileLock;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class TestRepositoryManager {

  @Test
  public void updatesOfPlugin() throws Exception {
    assertTrue(RepositoryManager.INSTANCE.getAllCompatibleUpdatesOfPlugin(getIdeVersion(), "ActionScript Profiler").size() > 0);
  }

  @Test
  public void lastUpdate() throws Exception {
    UpdateInfo info = RepositoryManager.INSTANCE.getLastCompatibleUpdateOfPlugin(getIdeVersion(), "org.jetbrains.kotlin");
    assertNotNull(info);
    assertTrue(info.getUpdateId() > 20000);
  }

  @Test
  public void lastCompatibleUpdates() throws Exception {
    List<UpdateInfo> updates = RepositoryManager.INSTANCE.getLastCompatibleUpdates(IdeVersion.createIdeVersion("IU-163.2112"));
    assertFalse(updates.isEmpty());
  }

  @NotNull
  private IdeVersion getIdeVersion() {
    return IdeVersion.createIdeVersion("IU-162.1132.10");
  }

  @Test
  public void downloadNonExistentPlugin() throws Exception {
    IFileLock id = RepositoryManager.INSTANCE.getPluginFile(-1000);
    assertNull(id);
  }

  @Test
  public void downloadExistentPlugin() throws Exception {
    IFileLock info = RepositoryManager.INSTANCE.getPluginFile(25128); //.gitignore 1.3.3
    assertNotNull(info);
    assertTrue(info.getFile().length() > 0);
    info.release();
  }

}

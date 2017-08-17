package com.jetbrains.pluginverifier.results;

import com.intellij.structure.ide.IdeVersion;
import com.jetbrains.pluginverifier.repository.FileLock;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.repository.UpdateInfo;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class TestRepositoryManager {

  @Test
  public void updatesOfPlugin() {
    assertTrue(RepositoryManager.INSTANCE.getAllCompatibleUpdatesOfPlugin(getIdeVersion(), "ActionScript Profiler").size() > 0);
  }

  @Test
  public void lastUpdate() {
    UpdateInfo info = RepositoryManager.INSTANCE.getLastCompatibleUpdateOfPlugin(getIdeVersion(), "org.jetbrains.kotlin");
    assertNotNull(info);
    assertTrue(info.getUpdateId() > 20000);
  }

  @Test
  public void lastCompatibleUpdates() {
    List<UpdateInfo> updates = RepositoryManager.INSTANCE.getLastCompatibleUpdates(IdeVersion.createIdeVersion("IU-163.2112"));
    assertFalse(updates.isEmpty());
  }

  @NotNull
  private IdeVersion getIdeVersion() {
    return IdeVersion.createIdeVersion("IU-162.1132.10");
  }

  @Test
  public void downloadNonExistentPlugin() {
    UpdateInfo updateInfo = RepositoryManager.INSTANCE.getUpdateInfoById(-1000);
    assertNull(updateInfo);
  }

  @Test
  public void downloadExistentPlugin() {
    UpdateInfo updateInfo = RepositoryManager.INSTANCE.getUpdateInfoById(25128); //.gitignore 1.3.3
    assertNotNull(updateInfo);
    FileLock fileLock = RepositoryManager.INSTANCE.getPluginFile(updateInfo);
    assertNotNull(fileLock);
    assertTrue(fileLock.getFile().length() > 0);
    fileLock.release();
  }

}

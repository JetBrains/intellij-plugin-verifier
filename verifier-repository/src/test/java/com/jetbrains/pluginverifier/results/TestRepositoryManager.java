package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Sergey Patrikeev
 */
public class TestRepositoryManager {
  @Test
  public void downloadNonExistentPlugin() throws Exception {
    UpdateInfo id = RepositoryManager.getInstance().findUpdateById(-1000);
    assertNull(id);
  }

  @Test
  public void downloadExistentPlugin() throws Exception {
    UpdateInfo info = RepositoryManager.getInstance().findUpdateById(25128); //.gitignore 1.3.3
    assertNotNull(info);
    File file = RepositoryManager.getInstance().getPluginFile(info);
    assertNotNull(file);
    assertTrue(file.length() > 0);
  }

}

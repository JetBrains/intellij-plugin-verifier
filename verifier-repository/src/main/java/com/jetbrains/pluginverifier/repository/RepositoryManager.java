package com.jetbrains.pluginverifier.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.DownloadManager;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
@ThreadSafe
public class RepositoryManager implements PluginRepository {

  private final static Logger LOG = LoggerFactory.getLogger(RepositoryManager.class);

  private static final Type updateListType = new TypeToken<List<UpdateInfo>>() {
  }.getType();

  private static final RepositoryManager INSTANCE = new RepositoryManager();

  @NotNull
  public static PluginRepository getInstance() {
    return INSTANCE;
  }

  private String getRepoUrl() {
    return RepositoryConfiguration.getInstance().getPluginRepositoryUrl();
  }

  @NotNull
  @Override
  public List<UpdateInfo> getLastCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException {
    LOG.debug("Loading list of plugins compatible with " + ideVersion + "... ");

    URL url = new URL(getRepoUrl() + "/manager/allCompatibleUpdates/?build=" + ideVersion);

    return new Gson().fromJson(IOUtils.toString(url), updateListType);
  }

  @Nullable
  @Override
  public UpdateInfo getLastCompatibleUpdateOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    LOG.debug("Fetching last compatible update of plugin {} with ide {}", pluginId, ideVersion);

    //search the given number in the all compatible updates
    List<UpdateInfo> all = getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId);
    UpdateInfo result = null;
    for (UpdateInfo info : all) {
      if (result == null || result.getUpdateId() < info.getUpdateId()) {
        result = info;
      }
    }

    return result;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getAllCompatibleUpdatesOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    LOG.debug("Fetching list of all compatible builds of a pluginId " + pluginId + " on IDE " + ideVersion);

    String urlSb = getRepoUrl() + "/manager/originalCompatibleUpdatesByPluginIds/?build=" + ideVersion +
        "&pluginIds=" + URLEncoder.encode(pluginId, "UTF-8");

    return new Gson().fromJson(IOUtils.toString(new URL(urlSb)), updateListType);
  }

  @Nullable
  @Override
  public File getPluginFile(@NotNull UpdateInfo update) throws IOException {
    return getPluginFile(update.getUpdateId());
  }

  @Nullable
  @Override
  public File getPluginFile(int updateId) throws IOException {
    return DownloadManager.getInstance().getOrLoadUpdate(updateId);
  }

}

package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.DownloadManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
@ThreadSafe
class GlobalRepository implements PluginRepository {

  private final static Logger LOG = LoggerFactory.getLogger(GlobalRepository.class);

  private static final Type updateListType = new TypeToken<List<UpdateInfo>>() {
  }.getType();

  private final String url;

  GlobalRepository(@NotNull String url) {
    this.url = url;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getLastCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException {
    LOG.info("Loading list of plugins compatible with " + ideVersion + "... ");

    URL url1 = new URL(url + "/manager/allCompatibleUpdates/?build=" + ideVersion);
    String text = IOUtils.toString(url1);

    return new Gson().fromJson(text, updateListType);
  }

  @Nullable
  @Override
  public UpdateInfo getLastCompatibleUpdateOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {

    //returns the last compatible update number
    URL u = new URL(url + "/manager/getCompatibleUpdateId/?build=" + ideVersion.asString() + "&pluginId=" + URLEncoder.encode(pluginId, "UTF-8"));

    int updateId = Integer.parseInt(IOUtils.toString(u));
    if (updateId == 0) {
      return null;
    }

    //search the given number in the all compatible updates
    List<UpdateInfo> all = getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId);
    for (UpdateInfo info : all) {
      if (info.getUpdateId() != null && info.getUpdateId() == updateId) {
        return info;
      }
    }

    return null;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getAllCompatibleUpdatesOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    LOG.info("Fetching list of all compatible builds of a pluginId " + pluginId + " on IDE " + ideVersion);

    String urlSb = url + "/manager/originalCompatibleUpdatesByPluginIds/?build=" + ideVersion +
        "&pluginIds=" + URLEncoder.encode(pluginId, "UTF-8");

    URL url1 = new URL(urlSb);
    String text = IOUtils.toString(url1);

    return new Gson().fromJson(text, updateListType);
  }

  @NotNull
  @Override
  public File getPluginFile(@NotNull UpdateInfo update) throws IOException {
    Preconditions.checkArgument(update.getUpdateId() != null, "UpdateId must contain a valid id to be downloaded");
    return DownloadManager.getInstance().getOrLoadUpdate(update, getUrlForUpdate(update));
  }

  @NotNull
  private URL getUrlForUpdate(@NotNull UpdateInfo update) throws MalformedURLException {
    return new URL(url + "/plugin/download/?noStatistic=true&updateId=" + update.getUpdateId());
  }

  @Nullable
  @Override
  public UpdateInfo findUpdateById(int updateId) throws IOException {
    UpdateInfo update = new UpdateInfo(updateId);
    if (DownloadManager.getInstance().doesUpdateExist(update, getUrlForUpdate(update))) {
      return update;
    }
    return null;
  }

}

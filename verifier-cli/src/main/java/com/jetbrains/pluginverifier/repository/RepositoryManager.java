package com.jetbrains.pluginverifier.repository;

import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.Configuration;
import com.jetbrains.pluginverifier.utils.DownloadUtils;
import com.jetbrains.pluginverifier.utils.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class RepositoryManager {

  private static final RepositoryManager INSTANCE = new RepositoryManager();

  private final GlobalRepository globalRepository;

  private final List<PluginRepository> repositories;

  private final WeakHashMap<UpdateInfo, PluginRepository> update2repository = new WeakHashMap<UpdateInfo, PluginRepository>();

  private final Map<Pair<String, String>, UpdateInfo> plugin2updateId = new HashMap<Pair<String, String>, UpdateInfo>();

  private RepositoryManager() {
    globalRepository = new GlobalRepository(Configuration.getInstance().getPluginRepositoryUrl());
    repositories = Collections.<PluginRepository>singletonList(globalRepository);
  }

  public static RepositoryManager getInstance() {
    return INSTANCE;
  }

  public List<PluginRepository> getRepositories() {
    return repositories;
  }

  public List<UpdateInfo> getAllCompatibleUpdates(@NotNull String ideVersion) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (PluginRepository repository : getRepositories()) {
      List<UpdateInfo> updates = repository.getAllCompatibleUpdates(ideVersion);
      for (UpdateInfo update : updates) {
        update2repository.put(update, repository);
      }

      res.addAll(updates);
    }

    return res;
  }

  public List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull String ideVersion, @NotNull List<String> pluginIds) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (PluginRepository repository : getRepositories()) {
      List<UpdateInfo> updates = repository.getCompatibleUpdatesForPlugins(ideVersion, pluginIds);
      for (UpdateInfo update : updates) {
        update2repository.put(update, repository);
      }

      res.addAll(updates);
    }

    return res;
  }

  private static String getCacheFileName(UpdateInfo update) {
    if (update.getUpdateId() != null) {
      return update.getUpdateId() + ".zip";
    }
    else {
      String updateAndVersion = update.getPluginId() + ":" + update.getVersion();
      return (updateAndVersion + '_' + Integer.toHexString(updateAndVersion.hashCode()) + ".zip").replaceAll("[^A-Z0-9_\\-.]+", "_");
    }
  }

  @NotNull
  public File getOrLoadUpdate(UpdateInfo update) throws IOException {
    File downloadDir = DownloadUtils.getOrCreateDownloadDir();

    File pluginInCache = new File(downloadDir, getCacheFileName(update));

    if (!pluginInCache.exists()) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

      System.out.println("Downloading " + update + "... ");

      boolean downloadFail = true;
      try {
        PluginRepository repository = update2repository.get(update);

        FileUtils.copyURLToFile(new URL(repository.getUpdateUrl(update)), currentDownload);

        if (currentDownload.length() < 200) {
          throw new IOException("Broken zip archive");
        }

        System.out.println("done");
        downloadFail = false;
      }
      finally {
        if (downloadFail) {
          System.out.println("error");
        }
      }

      FileUtils.moveFile(currentDownload, pluginInCache);
    }

    return pluginInCache;
  }

  @Nullable
  public UpdateInfo findUpdateById(int updateId) throws IOException {
    UpdateInfo updateInfo = new UpdateInfo();
    updateInfo.setUpdateId(updateId);

    update2repository.put(updateInfo, globalRepository);

    return updateInfo;
  }


  @Nullable
  public UpdateInfo findPlugin(@NotNull String ideVersion, @NotNull String pluginId) throws IOException {
    Pair<String, String> pair = Pair.create(ideVersion, pluginId);

    UpdateInfo res = plugin2updateId.get(pair);

    if (res == null) {
      for (PluginRepository repository : getRepositories()) {
        res = repository.findPlugin(ideVersion, pluginId);
        if (res != null) {
          update2repository.put(res, repository);
          plugin2updateId.put(pair, res);
          break;
        }
      }
    }

    return res;
  }


}

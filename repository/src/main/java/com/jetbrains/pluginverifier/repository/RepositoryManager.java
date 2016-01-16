package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Throwables;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.DownloadUtils;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.utils.Assert;
import com.jetbrains.pluginverifier.utils.Pair;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
    globalRepository = new GlobalRepository(RepositoryConfiguration.getInstance().getPluginRepositoryUrl());

    repositories = new ArrayList<PluginRepository>();
    repositories.add(globalRepository);

    String customRepositories = RepositoryConfiguration.getInstance().getCustomRepositories();
    if (StringUtil.isNotEmpty(customRepositories)) {
      for (StringTokenizer tokenizer = new StringTokenizer(customRepositories, ", "); tokenizer.hasMoreTokens(); ) {
        String repositoryUrl = tokenizer.nextToken();
        try {
          repositories.add(new CustomRepository(new URL(repositoryUrl)));
        }
        catch (MalformedURLException e) {
          throw Throwables.propagate(e);
        }
      }
    }
  }

  public static RepositoryManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  private List<PluginRepository> getRepositories() {
    return repositories;
  }

  //TODO: replace String of IDE-version by IdeVersion instance where possible
  @NotNull
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

  @NotNull
  public List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull String ideVersion,
                                                         @NotNull Collection<String> pluginIds) throws IOException {
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

  @NotNull
  public File getOrLoadUpdate(@NotNull UpdateInfo update) throws IOException {
    PluginRepository repository = update2repository.get(update);
    Assert.assertTrue(repository != null, "Unknown update, update should be found by RepositoryManager");

    return DownloadUtils.getOrLoadUpdate(update, new URL(repository.getUpdateUrl(update)));
  }

  @NotNull
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

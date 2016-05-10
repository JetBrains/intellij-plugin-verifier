package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Throwables;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.WeakHashMap;

/**
 * @author Sergey Evdokimov
 */
public class RepositoryManager implements PluginRepository {

  private static final RepositoryManager INSTANCE = new RepositoryManager();

  private final List<PluginRepository> repositories;

  private final WeakHashMap<UpdateInfo, PluginRepository> update2repository = new WeakHashMap<UpdateInfo, PluginRepository>();

  private RepositoryManager() {

    repositories = new ArrayList<PluginRepository>();
    repositories.add(new GlobalRepository(RepositoryConfiguration.getInstance().getPluginRepositoryUrl()));

    String customRepositories = RepositoryConfiguration.getInstance().getCustomRepositories();
    if (StringUtil.isNotEmpty(customRepositories)) {
      for (StringTokenizer tokenizer = new StringTokenizer(customRepositories, ", "); tokenizer.hasMoreTokens(); ) {
        String repositoryUrl = tokenizer.nextToken();
        try {
          repositories.add(new CustomRepository(new URL(repositoryUrl)));
        } catch (MalformedURLException e) {
          throw Throwables.propagate(e);
        }
      }
    }
  }

  public static RepositoryManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  public List<UpdateInfo> getLastCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (PluginRepository repository : repositories) {
      List<UpdateInfo> updates = repository.getLastCompatibleUpdates(ideVersion);
      for (UpdateInfo update : updates) {
        update2repository.put(update, repository);
      }

      res.addAll(updates);
    }

    return res;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getAllCompatibleUpdatesOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (PluginRepository repository : repositories) {
      List<UpdateInfo> updates = repository.getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId);
      for (UpdateInfo update : updates) {
        update2repository.put(update, repository);
      }

      res.addAll(updates);
    }

    return res;
  }


  @Override
  @Nullable
  public File getPluginFile(@NotNull UpdateInfo update) throws IOException {
    PluginRepository repository = update2repository.get(update);
    FailUtil.assertTrue(repository != null, "Unknown update, update should be found by RepositoryManager");
    return repository.getPluginFile(update);
  }


  @Override
  @Nullable
  public UpdateInfo findUpdateById(int updateId) throws IOException {
    for (PluginRepository repository : repositories) {
      UpdateInfo update = repository.findUpdateById(updateId);
      if (update != null) {
        update2repository.put(update, repository);
        return update;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public UpdateInfo getLastCompatibleUpdateOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    for (PluginRepository repository : repositories) {
      UpdateInfo update = repository.getLastCompatibleUpdateOfPlugin(ideVersion, pluginId);
      if (update != null) {
        update2repository.put(update, repository);
        return update;
      }
    }
    return null;
  }

}

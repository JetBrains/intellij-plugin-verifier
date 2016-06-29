package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Sergey Evdokimov
 */
@ThreadSafe
public class RepositoryManager implements PluginRepository {

  private static final RepositoryManager INSTANCE = new RepositoryManager();

  private final ImmutableList<PluginRepository> repositories;

  private final ConcurrentMap<UpdateInfo, PluginRepository> update2repository = new ConcurrentHashMap<UpdateInfo, PluginRepository>();

  private RepositoryManager() {

    List<PluginRepository> reps = new ArrayList<PluginRepository>();
    reps.add(new GlobalRepository(RepositoryConfiguration.getInstance().getPluginRepositoryUrl()));

    String customRepositories = RepositoryConfiguration.getInstance().getCustomRepositories();
    if (StringUtil.isNotEmpty(customRepositories)) {
      for (StringTokenizer tokenizer = new StringTokenizer(customRepositories, ","); tokenizer.hasMoreTokens(); ) {
        String repositoryUrl = tokenizer.nextToken().trim();
        try {
          reps.add(new CustomRepository(new URL(repositoryUrl)));
        } catch (MalformedURLException e) {
          throw FailUtil.fail("Unable to connect custom repository by " + repositoryUrl, e);
        }
      }
    }

    repositories = ImmutableList.copyOf(reps);
  }

  public static RepositoryManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  @Override
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
    Preconditions.checkArgument(repository != null, "Unknown update " + update + ", it should be registered in the RepositoryManager");
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

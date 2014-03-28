package com.jetbrains.pluginverifier.repository;

import com.jetbrains.pluginverifier.problems.UpdateInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public abstract class PluginRepository {

  public abstract List<UpdateInfo> getAllCompatibleUpdates(@NotNull String ideVersion) throws IOException;

  @Nullable
  public abstract UpdateInfo findPlugin(@NotNull String ideVersion, @NotNull String pluginId) throws IOException;

  public abstract List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull String ideVersion, List<String> pluginIds) throws IOException;

  @NotNull
  public abstract String getUpdateUrl(UpdateInfo update);

}

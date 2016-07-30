package com.jetbrains.pluginverifier.repository;

import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
interface PluginRepository {

  @NotNull
  List<UpdateInfo> getLastCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException;

  @Nullable
  UpdateInfo getLastCompatibleUpdateOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException;

  @NotNull
  List<UpdateInfo> getAllCompatibleUpdatesOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException;

  @Nullable
  UpdateInfo findUpdateById(int updateId) throws IOException;

  @Nullable
  File getPluginFile(@NotNull UpdateInfo update) throws IOException;

}

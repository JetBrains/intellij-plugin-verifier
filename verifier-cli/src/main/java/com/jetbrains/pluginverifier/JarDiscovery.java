package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.BrokenPluginException;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.utils.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class JarDiscovery {

  @NotNull
  public static IdeaPlugin createIdeaPlugin(@NotNull final File pluginFile) throws IOException, BrokenPluginException {
    if (!pluginFile.exists()) {
      throw new IOException("Plugin not found: " + pluginFile);
    }

    if (pluginFile.isFile()) {
      if (pluginFile.getName().endsWith(".zip")) {
        return IdeaPlugin.createFromZip(pluginFile);
      }
      if (pluginFile.getName().endsWith(".jar")) {
        return IdeaPlugin.createFromJar(pluginFile);
      }
      throw new IOException("Unknown input file: " + pluginFile);
    }

    final String[] topLevelList = pluginFile.list();
    assert topLevelList != null;

    if (topLevelList.length == 0) {
      throw new BrokenPluginException("Plugin root directory '" + pluginFile + "' is empty");
    }

    if (topLevelList.length > 1) {
      throw new BrokenPluginException("Plugin root directory '" + pluginFile + "' contains more than one child");
    }

    return IdeaPlugin.createFromDirectory(new File(pluginFile, topLevelList[0]));
  }
}

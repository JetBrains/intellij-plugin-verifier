package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.BrokenPluginException;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.utils.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class JarDiscovery {

  @NotNull
  public static IdeaPlugin createIdeaPlugin(@NotNull final File pluginDir) throws IOException, BrokenPluginException {
    if (!pluginDir.exists()) {
      throw  Util.fail("Plugin not found: " + pluginDir);
    }

    final File realDir;
    if (pluginDir.isFile() && pluginDir.getName().endsWith(".zip")) {
      return IdeaPlugin.createFromZip(pluginDir);
    } else if (pluginDir.isDirectory()) {
      realDir = pluginDir;
    } else {
      throw Util.fail("Unknown input file: " + pluginDir);
    }

    final String[] topLevelList = realDir.list();
    assert topLevelList != null;

    if (topLevelList.length == 0) {
      throw Util.fail("Plugin root is empty");
    }

    if (topLevelList.length > 1) {
      throw Util.fail("Plugin root contains more than one element");
    }

    return IdeaPlugin.createFromDirectory(new File(realDir, topLevelList[0]));
  }
}

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
      throw Util.fail("Plugin not found: " + pluginDir);
    }

    if (pluginDir.isFile() && pluginDir.getName().endsWith(".zip")) {
      return IdeaPlugin.createFromZip(pluginDir);
    }
    else if (!pluginDir.isDirectory()) {
      throw Util.fail("Unknown input file: " + pluginDir);
    }

    final String[] topLevelList = pluginDir.list();
    assert topLevelList != null;

    if (topLevelList.length == 0) {
      throw Util.fail("Plugin root directory '" + pluginDir + "' is empty");
    }

    if (topLevelList.length > 1) {
      throw Util.fail("Plugin root directory '" + pluginDir + "' contains more than one child");
    }

    return IdeaPlugin.createFromDirectory(new File(pluginDir, topLevelList[0]));
  }
}

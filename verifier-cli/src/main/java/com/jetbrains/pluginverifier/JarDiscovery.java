package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.BrokenPluginException;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.util.Util;

import java.io.File;
import java.io.IOException;

public class JarDiscovery {
  public static IdeaPlugin createIdeaPlugin(final File pluginDir, final Idea idea) throws IOException, BrokenPluginException {
    if (!pluginDir.exists()) return null;

    final File realDir;
    if (pluginDir.isFile() && pluginDir.getName().endsWith(".zip")) {
      return IdeaPlugin.createFromZip(idea, pluginDir);
    } else if (pluginDir.isDirectory()) {
      realDir = pluginDir;
    } else {
      Util.fail("Unknown input file: " + pluginDir);
      return null;
    }

    final String[] topLevelList = realDir.list();
    assert topLevelList != null;

    if (topLevelList.length == 0)
      Util.fail("Plugin root is empty");

    if (topLevelList.length > 1) {
      Util.fail("Plugin root contains more than one element");
    }

    return IdeaPlugin.createFromDirectory(idea, new File(realDir, topLevelList[0]));
  }
}

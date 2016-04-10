package com.jetbrains.pluginverifier.misc;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class PluginCache {
  private static final Map<File, SoftReference<Plugin>> pluginsMap = new HashMap<File, SoftReference<Plugin>>();
  private static final PluginCache INSTANCE = new PluginCache();

  private PluginCache() {
  }

  public static PluginCache getInstance() {
    return INSTANCE;
  }

  /**
   * Returns a plugin from cache or creates it from the specified file
   *
   * @param pluginZip file of a plugin
   * @param readClasses whether to try to load plugin classes or just read the <i>plugin.xml</i>
   * @return null if plugin is not found in the cache
   * @throws IOException if IO error occurs during attempt to create a plugin
   * @throws IncorrectPluginException if the given plugin file is incorrect
   */
  @NotNull
  public Plugin createPlugin(File pluginZip, boolean readClasses) throws IOException, IncorrectPluginException {
    if (!pluginZip.exists()) {
      throw new IOException("Plugin file does not exist: " + pluginZip.getAbsoluteFile());
    }

    SoftReference<Plugin> softReference = pluginsMap.get(pluginZip);

    Plugin res = softReference == null ? null : softReference.get();

    if (res == null) {
      if (readClasses) {
        res = PluginManager.getInstance().createPlugin(pluginZip);
      } else {
        res = PluginManager.getInstance().createPluginWithEmptyResolver(pluginZip);
      }
      pluginsMap.put(pluginZip, new SoftReference<Plugin>(res));
    }

    return res;
  }


}

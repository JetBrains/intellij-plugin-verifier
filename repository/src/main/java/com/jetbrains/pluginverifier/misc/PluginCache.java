package com.jetbrains.pluginverifier.misc;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.domain.IdeaPluginManager;
import org.jetbrains.annotations.Nullable;

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

  @Nullable
  public Plugin getPlugin(File pluginZip) {
    SoftReference<Plugin> softReference = pluginsMap.get(pluginZip);

    if (softReference == null && pluginsMap.containsKey(pluginZip)) {
      return null; // means failed to download plugin or plugin is broken
    }

    Plugin res = softReference == null ? null : softReference.get();

    if (res == null) {
      SoftReference<Plugin> ref = null;

      try {
        res = IdeaPluginManager.getInstance().createPlugin(pluginZip);
        ref = new SoftReference<Plugin>(res);
      } catch (IOException e) {
        System.out.println("Plugin is broken: " + pluginZip);
        e.printStackTrace();
      } catch (IncorrectPluginException e) {
        System.out.println("Plugin is broken: " + pluginZip);
        e.printStackTrace();
      } finally {
        pluginsMap.put(pluginZip, ref);
      }
    }

    return res;
  }


}

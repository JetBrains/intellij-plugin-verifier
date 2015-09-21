package com.intellij.structure.domain;

import com.intellij.structure.errors.BrokenPluginException;
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
  private static final Map<File, SoftReference<IdeaPlugin>> pluginsMap = new HashMap<File, SoftReference<IdeaPlugin>>();
  private static PluginCache ourInstance = new PluginCache();

  private PluginCache() {
  }

  public static PluginCache getInstance() {
    return ourInstance;
  }

  @Nullable
  public IdeaPlugin getPlugin(File pluginZip) {
    SoftReference<IdeaPlugin> softReference = pluginsMap.get(pluginZip);

    if (softReference == null && pluginsMap.containsKey(pluginZip)) {
      return null; // means failed to download plugin or plugin is broken
    }

    IdeaPlugin res = softReference == null ? null : softReference.get();

    if (res == null) {
      SoftReference<IdeaPlugin> ref = null;

      try {
        res = IdeaPlugin.createFromZip(pluginZip);
        ref = new SoftReference<IdeaPlugin>(res);
      }
      catch (IOException e) {
        System.out.println("Plugin is broken: " + pluginZip);
        e.printStackTrace();
      }
      catch (BrokenPluginException e) {
        System.out.println("Plugin is broken: " + pluginZip);
        e.printStackTrace();
      }
      finally {
        pluginsMap.put(pluginZip, ref);
      }
    }

    return res;
  }


}

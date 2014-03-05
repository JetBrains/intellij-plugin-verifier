package com.jetbrains.pluginverifier.domain;

import com.jetbrains.pluginverifier.utils.Configuration;
import com.jetbrains.pluginverifier.utils.DownloadUtils;
import com.jetbrains.pluginverifier.utils.Pair;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class RemotePluginCache {
  private static RemotePluginCache ourInstance = new RemotePluginCache();

  //private static final Set<String> notAPlugin = ImmutableSet.of("com.intellij.modules.ultimate", "com.intellij.modules.java");

  public static RemotePluginCache getInstance() {
    return ourInstance;
  }

  private static final Map<Pair<String, String>, Integer> plugin2updateId = new HashMap<Pair<String, String>, Integer>();

  private static final Map<Integer, SoftReference<IdeaPlugin>> pluginsMap = new HashMap<Integer, SoftReference<IdeaPlugin>>();

  private RemotePluginCache() {

  }

  public int getUpdateId(String ideVersion, String pluginId) throws IOException {
    Pair<String, String> pair = Pair.create(ideVersion, pluginId);

    Integer res = plugin2updateId.get(pair);

    if (res == null) {
      URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/getCompatibleUpdateId/?build=" + ideVersion + "&pluginId=" +
                        URLEncoder.encode(pluginId, "UTF-8"));

      res = Integer.parseInt(IOUtils.toString(url));

      plugin2updateId.put(pair, res);
    }

    return res;
  }

  public IdeaPlugin getUpdate(String ideVersion, String pluginId) throws IOException {
    int updateId = getUpdateId(ideVersion, pluginId);
    if (updateId <= 0) return null;

    SoftReference<IdeaPlugin> softReference = pluginsMap.get(updateId);

    if (softReference == null && pluginsMap.containsKey(updateId)) {
      return null; // means failed to download plugin or plugin is broken
    }

    IdeaPlugin res = softReference == null ? null : softReference.get();

    if (res == null) {
      SoftReference<IdeaPlugin> ref = null;

      try {
        File update = DownloadUtils.getUpdate(updateId);
        res = IdeaPlugin.createFromZip(update);
        ref = new SoftReference<IdeaPlugin>(res);
      }
      catch (IOException e) {
        System.out.println("failed to download: " + e.getMessage());
      }
      catch (BrokenPluginException e) {
        System.out.println("Plugin is broken: " + pluginId + '#' + updateId);
        e.printStackTrace();
      }
      finally {
        pluginsMap.put(updateId, ref);
      }
    }

    return res;
  }

}

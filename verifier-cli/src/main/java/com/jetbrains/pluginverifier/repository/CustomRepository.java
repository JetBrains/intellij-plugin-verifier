package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.PluginCache;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.DownloadUtils;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CustomRepository extends PluginRepository {

  private final URL url;

  private Map<UpdateInfo, String> repositoriesMap;

  public CustomRepository(URL url) {
    this.url = url;
  }

  public Map<UpdateInfo, String> getRepositoriesMap() {
    Map<UpdateInfo, String> res = repositoriesMap;

    if (res == null) {
      res = new LinkedHashMap<UpdateInfo, String>();

      try {
        String pluginListXml = IOUtils.toString(url);

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(pluginListXml));

        List<Element> pluginList = document.getRootElement().getChildren("plugin");

        for (Element element : pluginList) {
          UpdateInfo update = new UpdateInfo();

          update.setPluginId(element.getAttributeValue("id"));
          update.setVersion(element.getAttributeValue("version"));
          if (!update.validate()) continue;

          String pluginUrl = getPluginUrl(url.toExternalForm(), element.getAttributeValue("url"));

          res.put(update, pluginUrl);
        }
      }
      catch (IOException e) {
        System.out.printf("Failed to download plugin list from %s (%s)\n", url, e.getLocalizedMessage());
      }
      catch (JDOMException e) {
        System.out.printf("Failed to parse plugin list from %s (%s)\n", url, e.getLocalizedMessage());
      }

      repositoriesMap = res;
    }

    return res;
  }

  private static String getPluginUrl(String pluginListUrl, String pluginUrl) {
    if (pluginUrl.contains("://")) return pluginUrl;

    int idx = pluginListUrl.lastIndexOf('/');
    if (idx != -1) {
      return pluginListUrl.substring(0, idx + 1) + pluginUrl;
    }

    return pluginUrl;
  }

  private List<UpdateInfo> getUpdates(@NotNull String ideVersion, Predicate<UpdateInfo> predicate) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (Map.Entry<UpdateInfo, String> entry : Maps.filterKeys(getRepositoriesMap(), predicate).entrySet()) {
      File update = DownloadUtils.getOrLoadUpdate(entry.getKey(), new URL(entry.getValue()));
      IdeaPlugin ideaPlugin = PluginCache.getInstance().getPlugin(update);
      if (ideaPlugin != null && ideaPlugin.isCompatibleWithIde(ideVersion)) {
        res.add(entry.getKey());
      }
    }

    return res;
  }

  @Override
  public List<UpdateInfo> getAllCompatibleUpdates(@NotNull String ideVersion) throws IOException {
    return getUpdates(ideVersion, Predicates.<UpdateInfo>alwaysTrue());
  }

  @Nullable
  @Override
  public UpdateInfo findPlugin(@NotNull String ideVersion, @NotNull String pluginId) throws IOException {
    return null;
  }

  @Override
  public List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull String ideVersion, final Collection<String> pluginIds) throws IOException {
    return getUpdates(ideVersion, new Predicate<UpdateInfo>() {
      @Override
      public boolean apply(UpdateInfo input) {
        return pluginIds.contains(input.getPluginId());
      }
    });
  }

  @NotNull
  @Override
  public String getUpdateUrl(UpdateInfo update) {
    return getRepositoriesMap().get(update);
  }
}

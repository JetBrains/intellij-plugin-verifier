package com.jetbrains.pluginverifier.repository;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.DownloadUtils;
import com.jetbrains.pluginverifier.misc.PluginCache;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */

class CustomRepository implements PluginRepository {

  private final URL url;

  private Map<UpdateInfo, String> repositoriesMap;

  CustomRepository(URL url) {
    this.url = url;
  }

  private static String getPluginUrl(String pluginListUrl, String pluginUrl) {
    if (pluginUrl.contains("://")) return pluginUrl;

    int idx = pluginListUrl.lastIndexOf('/');
    if (idx != -1) {
      return pluginListUrl.substring(0, idx + 1) + pluginUrl;
    }

    return pluginUrl;
  }

  private Map<UpdateInfo, String> getRepositoriesMap() {
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

          boolean valid = update.getUpdateId() != null || (update.getPluginId() != null && !update.getPluginId().isEmpty() && update.getVersion() != null && !update.getVersion().isEmpty());

          if (!valid) {
            continue;
          }

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

  private List<UpdateInfo> getUpdates(@NotNull IdeVersion ideVersion, Predicate<UpdateInfo> predicate) throws IOException {
    List<UpdateInfo> res = new ArrayList<UpdateInfo>();

    for (Map.Entry<UpdateInfo, String> entry : Maps.filterKeys(getRepositoriesMap(), predicate).entrySet()) {
      File update = DownloadUtils.getOrLoadUpdate(entry.getKey(), new URL(entry.getValue()));

      Plugin ideaPlugin;
      try {
        ideaPlugin = PluginCache.getInstance().createPlugin(update, false);
      } catch (IncorrectPluginException e) {
        //TODO: add log
        continue;
      }

      if (ideaPlugin.isCompatibleWithIde(ideVersion)) {
        res.add(entry.getKey());
      }
    }

    return res;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getLastCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException {
    return getUpdates(ideVersion, Predicates.<UpdateInfo>alwaysTrue());
  }

  @Nullable
  @Override
  public UpdateInfo getLastCompatibleUpdateOfPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    List<UpdateInfo> list = getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId);
    if (list.isEmpty()) {
      return null;
    }

    UpdateInfo res = list.get(0);
    for (UpdateInfo info : list) {
      if (UpdateInfo.UPDATE_NUMBER_COMPARATOR.compare(res, info) < 0) {
        res = info;
      }
    }

    return res;
  }

  @NotNull
  @Override
  public List<UpdateInfo> getAllCompatibleUpdatesOfPlugin(@NotNull IdeVersion ideVersion, @NotNull final String pluginId) throws IOException {
    return getUpdates(ideVersion, new Predicate<UpdateInfo>() {
      @Override
      public boolean apply(UpdateInfo input) {
        return pluginId.equals(input.getPluginId());
      }
    });
  }

  @Override
  @Nullable
  public File getPluginFile(@NotNull UpdateInfo update) throws IOException {
    String url = getRepositoriesMap().get(update);
    if (url == null) {
      return null;
    }
    return DownloadUtils.getOrLoadUpdate(update, new URL(url));
  }

  @Nullable
  @Override
  public UpdateInfo findUpdateById(int updateId) throws IOException {
    return null;
  }

}

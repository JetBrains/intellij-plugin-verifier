package com.jetbrains.pluginverifier.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.utils.FailUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
class GlobalRepository extends PluginRepository {

  private static final Type updateListType = new TypeToken<List<UpdateInfo>>() {
  }.getType();

  private final String url;

  GlobalRepository(@NotNull String url) {
    this.url = url;
  }

  @Override
  public List<UpdateInfo> getAllCompatibleUpdates(@NotNull IdeVersion ideVersion) throws IOException {
    System.out.println("Loading compatible plugins list... ");

    URL url1 = new URL(RepositoryConfiguration.getInstance().getPluginRepositoryUrl() + "/manager/allCompatibleUpdates/?build=" + ideVersion);
    String text = IOUtils.toString(url1);

    return new Gson().fromJson(text, updateListType);
  }

  @Nullable
  @Override
  public UpdateInfo findPlugin(@NotNull IdeVersion ideVersion, @NotNull String pluginId) throws IOException {
    URL u = new URL(url + "/manager/getCompatibleUpdateId/?build=" + ideVersion + "&pluginId=" + URLEncoder.encode(pluginId, "UTF-8"));

    int updateId = Integer.parseInt(IOUtils.toString(u));
    if (updateId == 0) {
      return null;
    }

    UpdateInfo res = new UpdateInfo();
    res.setUpdateId(updateId);

    return res;
  }

  @Override
  public List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull IdeVersion ideVersion, @NotNull Collection<String> pluginIds) throws IOException {
    /*
    pluginIds = new HashSet<String>(pluginIds);

    List<UpdateInfo> result = new ArrayList<UpdateInfo>();
    for (UpdateInfo update : getAllCompatibleUpdates(ideVersion)) {
      String pluginId = update.getPluginId();
      if (pluginId != null && pluginIds.contains(pluginId)) {
        result.add(update);
      }
    }
    return result;
    */


    System.out.println("Loading compatible plugins list... ");

    StringBuilder urlSb = new StringBuilder();
    urlSb.append(RepositoryConfiguration.getInstance().getPluginRepositoryUrl())
        .append("/manager/originalCompatibleUpdatesByPluginIds/?build=").append(ideVersion);

    for (String id : pluginIds) {
      urlSb.append("&pluginIds=").append(URLEncoder.encode(id, "UTF-8"));
    }

    URL url1 = new URL(urlSb.toString());
    String text = IOUtils.toString(url1);

    return new Gson().fromJson(text, updateListType);
  }

  @NotNull
  @Override
  public String getUpdateUrl(UpdateInfo update) {
    FailUtil.assertTrue(update.getUpdateId() != null, update.toString());

    return url + "/plugin/download/?noStatistic=true&updateId=" + update.getUpdateId();
  }

}

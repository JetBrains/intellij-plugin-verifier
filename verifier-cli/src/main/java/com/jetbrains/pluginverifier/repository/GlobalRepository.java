package com.jetbrains.pluginverifier.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.Configuration;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Sergey Evdokimov
 */
public class GlobalRepository extends PluginRepository {

  public static final Type updateListType = new TypeToken<List<UpdateInfo>>() {}.getType();

  private final String url;

  public GlobalRepository(String url) {
    this.url = url;
  }

  /**
   * Returns list of already checked IDEA builds (for which report was loaded)
   */
  public static List<String> loadAvailableCheckResultsList() throws IOException {
    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/problems/resultList?format=txt");

    String text = IOUtils.toString(url);

    List<String> res = new ArrayList<String>();

    for (StringTokenizer st = new StringTokenizer(text, " ,"); st.hasMoreTokens(); ) {
      res.add(st.nextToken());
    }

    return res;
  }

  @Override
  public List<UpdateInfo> getAllCompatibleUpdates(@NotNull String ideVersion) throws IOException {
    System.out.println("Loading compatible plugins list... ");

    URL url1 = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/allCompatibleUpdates/?build=" + ideVersion);
    String text = IOUtils.toString(url1);

    return new Gson().fromJson(text, updateListType);
  }

  @Nullable
  @Override
  public UpdateInfo findPlugin(@NotNull String ideVersion, @NotNull String pluginId) throws IOException {
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
  public List<UpdateInfo> getCompatibleUpdatesForPlugins(@NotNull String ideVersion, Collection<String> pluginIds) throws IOException {
    System.out.println("Loading compatible plugins list... ");

    StringBuilder urlSb = new StringBuilder();
    urlSb.append(Configuration.getInstance().getPluginRepositoryUrl())
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
    assert update.getUpdateId() != null;

    return url + "/plugin/download/?noStatistic=true&updateId=" + update.getUpdateId();
  }

}

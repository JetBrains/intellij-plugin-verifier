package com.jetbrains.pluginverifier.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Sergey Evdokimov
 */
public class PRUtil {

  public static final Type updateListType = new TypeToken<List<UpdateInfo>>() {}.getType();

  public static List<UpdateInfo> getAllCompatibleUpdates(@NotNull String ideVersion) throws IOException {
    System.out.println("Loading compatible plugins list... ");

    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/allCompatibleUpdates/?build=" + ideVersion);
    String text = IOUtils.toString(url);

    return new Gson().fromJson(text, updateListType);
  }

  public static List<UpdateInfo> getOriginalCompatibleUpdatesByPluginIds(@NotNull String ideVersion, @NotNull List<String> pluginIds) throws IOException {
    System.out.println("Loading compatible plugins list... ");

    StringBuilder urlSb = new StringBuilder();
    urlSb.append(Configuration.getInstance().getPluginRepositoryUrl())
      .append("/manager/originalCompatibleUpdatesByPluginIds/?build=").append(ideVersion);

    for (String id : pluginIds) {
      urlSb.append("&pluginIds=").append(URLEncoder.encode(id, "UTF-8"));
    }

    URL url = new URL(urlSb.toString());
    String text = IOUtils.toString(url);

    return new Gson().fromJson(text, updateListType);
  }

  public static List<String> loadAvailableCheckResultsList() throws IOException {
    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/problems/resultList?format=txt");

    String text = IOUtils.toString(url);

    List<String> res = new ArrayList<String>();

    for (StringTokenizer st = new StringTokenizer(text, " ,"); st.hasMoreTokens(); ) {
      res.add(st.nextToken());
    }

    return res;
  }

}

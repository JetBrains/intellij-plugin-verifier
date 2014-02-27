package com.jetbrains.pluginverifier.utils;

import com.google.common.io.Files;
import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Sergey Evdokimov
 */
public class DownloadUtils {

  private static File getDownloadDir() throws IOException {
    File downloadDir = Util.getPluginCacheDir();
    if (!downloadDir.isDirectory()) {
      downloadDir.mkdirs();
      if (!downloadDir.isDirectory()) {
        throw new IOException("Failed to create temp directory: " + downloadDir);
      }
    }

    return downloadDir;
  }

  public static File getUpdate(int updateId) throws IOException {
    File downloadDir = getDownloadDir();

    File pluginInCache = new File(downloadDir, updateId + ".zip");

    if (!pluginInCache.exists()) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

      System.out.print("Downloading update #" + updateId + "... ");

      FileUtils.copyURLToFile(new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/plugin/download/?noStatistic=true&updateId=" + updateId), currentDownload);

      if (currentDownload.length() < 200) {
        throw new IOException("Broken zip archive");
      }

      Files.move(currentDownload, pluginInCache);

      System.out.println("done");
    }

    return pluginInCache;
  }

  public static File getCheckResult(String build) throws IOException {
    File downloadDir = getDownloadDir();

    File checkResDir = new File(downloadDir, "checkResult");
    checkResDir.mkdirs();

    File res = new File(checkResDir, build + ".xml");

    updateFile(new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/files/checkResults/" + build + ".xml"), res);

    return res;
  }

  private static String toHttpDate(Date date) {
    SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    return httpDateFormat.format(date);
  }

  private static void updateFile(URL url, File file) throws IOException {
    long lastModified = file.lastModified();

    if (lastModified == 0) {
      FileUtils.copyURLToFile(url, file);
    }
    else {
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();

      connection.addRequestProperty(HttpHeaders.IF_MODIFIED_SINCE, toHttpDate(new Date(lastModified)));
      connection.addRequestProperty(HttpHeaders.CACHE_CONTROL, "max-age=0");
      connection.connect();
      int responseCode = connection.getResponseCode();

      try {
        if (responseCode == 200) {
          FileUtils.copyInputStreamToFile(connection.getInputStream(), file);

        }
        else if (responseCode == 304) {
          // Not modified
        }
        else {
          throw new IOException("Failed to download check result: " + responseCode);
        }
      }
      finally {
        connection.disconnect();
      }
    }
  }
}

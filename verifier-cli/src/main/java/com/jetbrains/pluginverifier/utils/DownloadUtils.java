package com.jetbrains.pluginverifier.utils;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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

    if (!res.exists()) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

      FileUtils.copyURLToFile(new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/files/checkResults/" + build + ".xml"), currentDownload);

      Files.move(currentDownload, res);
    }

    return res;
  }

}

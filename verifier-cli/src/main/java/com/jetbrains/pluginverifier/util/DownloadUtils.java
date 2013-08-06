package com.jetbrains.pluginverifier.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author Sergey Evdokimov
 */
public class DownloadUtils {

  public static File getUpdate(int updateId) throws IOException {
    File downloadDir = Util.getPluginCacheDir();
    if (!downloadDir.isDirectory()) {
      downloadDir.mkdirs();
      if (!downloadDir.isDirectory()) {
        throw new IOException("Failed to create temp directory: " + downloadDir);
      }
    }

    File pluginInCache = new File(downloadDir, updateId + ".zip");

    if (!pluginInCache.exists()) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

      System.out.print("Downloading plugin #" + updateId + "... ");

      FileUtils.copyURLToFile(new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/plugin/download/?noStatistic=true&updateId=" + updateId), currentDownload);

      if (currentDownload.length() < 200) {
        throw new IOException("Broken zip archive");
      }

      FileUtils.moveFile(currentDownload, pluginInCache);

      System.out.println("done");
    }

    return pluginInCache;
  }


}

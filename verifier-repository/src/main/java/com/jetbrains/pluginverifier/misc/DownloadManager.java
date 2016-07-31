package com.jetbrains.pluginverifier.misc;

import org.apache.commons.io.FileUtils;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Sergey Evdokimov
 */
@ThreadSafe
public class DownloadManager {

  private final static Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

  private static final int BROKEN_ZIP_THRESHOLD = 200;
  private static final DateFormat httpDateFormat;
  private static final DownloadManager INSTANCE = new DownloadManager();

  static {
    httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private DownloadManager() {
  }

  public static DownloadManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  private File getOrCreateDownloadDir() throws IOException {
    File downloadDir = RepositoryConfiguration.getInstance().getPluginCacheDir();
    if (!downloadDir.isDirectory()) {
      FileUtils.forceMkdir(downloadDir);
      if (!downloadDir.isDirectory()) {
        throw new IOException("Failed to create temp directory: " + downloadDir);
      }
    }

    return downloadDir;
  }

  @NotNull
  private String getCacheFileName(int updateId) {
    return updateId + ".zip";
  }

  /**
   * Performs necessary redirection
   */
  @NotNull
  private URL getFinalUrl(@NotNull URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setInstanceFollowRedirects(false);

    if (connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {
      return url;
    }

    if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP
        || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
      String location = connection.getHeaderField("Location");
      if (location != null) {
        return new URL(location);
      }
    }
    return url;
  }

  @NotNull
  private URL getUrlForUpdate(int updateId) throws MalformedURLException {
    return new URL(RepositoryConfiguration.getInstance().getPluginRepositoryUrl() + "/plugin/download/?noStatistic=true&updateId=" + updateId);
  }

  @Nullable
  public File getOrLoadUpdate(int updateId) throws IOException {
    File downloadDir = getOrCreateDownloadDir();

    URL url = getUrlForUpdate(updateId);
    try {
      url = getFinalUrl(url);
    } catch (IOException e) {
      throw new IOException("The repository " + url.getHost() + " problems", e);
    }

    File pluginInCache = new File(downloadDir, getCacheFileName(updateId));

    if (!pluginInCache.exists() || pluginInCache.length() < BROKEN_ZIP_THRESHOLD) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

      LOG.debug("Downloading {} by url {}... ", updateId, url);

      boolean downloadFail = true;
      try {
        FileUtils.copyURLToFile(url, currentDownload);

        if (currentDownload.length() < BROKEN_ZIP_THRESHOLD) {
          LOG.error("Broken zip archive by url {} of file {}", url, currentDownload);
          return null;
        }

        LOG.debug("Plugin {} is downloaded", updateId);
        downloadFail = false;
      } catch (Exception e) {
        LOG.error("Error loading plugin " + updateId + " by " + url.toExternalForm(), e);
        return null;
      } finally {
        if (downloadFail) {
          deleteLogged(currentDownload);
        }
      }

      //provides the thread safety while multiple threads attempt to load the same plugin.
      synchronized (this) {
        try {
          if (pluginInCache.exists()) {
            //remove the old (possibly broken plugin)
            FileUtils.forceDelete(pluginInCache);
          }
        } catch (Exception e) {
          LOG.error("Unable to delete cached plugin file " + pluginInCache, e);
          deleteLogged(currentDownload);
          throw e;
        }
        try {
          FileUtils.moveFile(currentDownload, pluginInCache);
        } catch (Exception e) {
          LOG.error("Unable to move downloaded plugin file " + currentDownload + " to " + pluginInCache, e);
          deleteLogged(currentDownload);
          deleteLogged(pluginInCache);
          throw e;
        }
      }

    }

    return pluginInCache;
  }

  private void deleteLogged(File file) {
    if (file.exists()) {
      try {
        FileUtils.forceDelete(file);
      } catch (Exception ce) {
        LOG.error("Unable to delete file " + file, ce);
      }
    }
  }

}

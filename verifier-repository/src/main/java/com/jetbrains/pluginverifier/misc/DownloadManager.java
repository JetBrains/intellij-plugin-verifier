package com.jetbrains.pluginverifier.misc;

import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.annotation.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
  private static final int HTTP_OK_STATUS = 200;
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

  private void updateFile(URL url, File file) throws IOException {
    long lastModified = file.lastModified();

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    if (lastModified > 0) {
      connection.addRequestProperty(HttpHeaders.IF_MODIFIED_SINCE, httpDateFormat.format(new Date(lastModified)));
      connection.addRequestProperty(HttpHeaders.CACHE_CONTROL, "max-age=0");
    }

    int responseCode = connection.getResponseCode();

    try {
      if (responseCode == HTTP_OK_STATUS) {
        String lastModifiedResStr = connection.getHeaderField(HttpHeaders.LAST_MODIFIED);
        if (lastModifiedResStr == null) {
          throw new IOException(HttpHeaders.LAST_MODIFIED + " header can not be null");
        }

        Date lastModifiedRes;

        try {
          lastModifiedRes = httpDateFormat.parse(lastModifiedResStr);
        } catch (ParseException e) {
          throw Throwables.propagate(e);
        }

        FileUtils.copyInputStreamToFile(connection.getInputStream(), file);

        //noinspection ResultOfMethodCallIgnored
        file.setLastModified(lastModifiedRes.getTime());

      } else
        //noinspection StatementWithEmptyBody
        if (responseCode == 304) { /* Not modified */

        } else {
          throw new IOException("Failed to download check result: " + responseCode);
        }
    } finally {
      connection.disconnect();
    }
  }

  @NotNull
  private String getCacheFileName(UpdateInfo update) {
    if (update.getUpdateId() != null) {
      return update.getUpdateId() + ".zip";
    } else {
      String updateAndVersion = update.getPluginId() + ":" + update.getVersion();
      return (updateAndVersion + '_' + Integer.toHexString(updateAndVersion.hashCode()) + ".zip").replaceAll("[^a-zA-Z0-9_\\-.]+", "_");
    }
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

  public boolean doesUpdateExist(@NotNull UpdateInfo updateInfo, @NotNull URL url) throws IOException {
    File cachedFile = new File(getOrCreateDownloadDir(), getCacheFileName(updateInfo));
    if (cachedFile.exists() && cachedFile.length() > BROKEN_ZIP_THRESHOLD) {
      //file exists in the cache
      return true;
    }

    url = getFinalUrl(url);
    try {
      InputStream stream = null;
      try {
        stream = url.openStream();
        return stream.read() != -1;
      } finally {
        IOUtils.closeQuietly(stream);
      }
    } catch (IOException e) {
      return false;
    }
  }

  @NotNull
  public File getOrLoadUpdate(@NotNull UpdateInfo update, @NotNull URL url) throws IOException {
    File downloadDir = getOrCreateDownloadDir();

    url = getFinalUrl(url);

    File pluginInCache = new File(downloadDir, getCacheFileName(update));

    if (!pluginInCache.exists() || pluginInCache.length() < BROKEN_ZIP_THRESHOLD) {
      File currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir);

//      LOG.info("Downloading " + update + "... ");

      boolean downloadFail = true;
      try {
        FileUtils.copyURLToFile(url, currentDownload);

        if (currentDownload.length() < BROKEN_ZIP_THRESHOLD) {
          throw new IOException("Broken zip archive");
        }

//        LOG.info("downloading " + update + " done!");
        downloadFail = false;
      } catch (IOException e) {
        LOG.error("Error loading plugin " + update + " by " + url.toExternalForm(), e);
        throw e;
      } finally {
        if (downloadFail) {
          if (currentDownload.exists()) {
            try {
              FileUtils.forceDelete(currentDownload);
            } catch (Exception ce) {
              LOG.error("Unable to delete temporary file " + currentDownload, ce);
            }
          }
        }
      }

      //provides the thread safety while multiple threads attempt to load the same plugin.
      synchronized (this) {
        if (pluginInCache.exists()) {
          //remove the old (possible broken plugin)
          FileUtils.forceDelete(pluginInCache);
        }
        FileUtils.moveFile(currentDownload, pluginInCache);
      }

    }

    return pluginInCache;
  }

}

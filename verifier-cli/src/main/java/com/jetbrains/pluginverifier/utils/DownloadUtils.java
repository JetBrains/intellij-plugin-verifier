package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Throwables;
import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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
public class DownloadUtils {

  private static final DateFormat httpDateFormat;
  static {
    httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public static File getOrCreateDownloadDir() {
    File downloadDir = Util.getPluginCacheDir();
    if (!downloadDir.isDirectory()) {
      downloadDir.mkdirs();
      if (!downloadDir.isDirectory()) {
        throw new RuntimeException("Failed to create temp directory: " + downloadDir);
      }
    }

    return downloadDir;
  }

  public static File getCheckResult(String build) throws IOException {
    File downloadDir = getOrCreateDownloadDir();

    File checkResDir = new File(downloadDir, "checkResult");
    checkResDir.mkdirs();

    File res = new File(checkResDir, build + ".xml");

    updateFile(new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/files/checkResults/" + build + ".xml"), res);

    return res;
  }

  private static void updateFile(URL url, File file) throws IOException {
    long lastModified = file.lastModified();

    HttpURLConnection connection = (HttpURLConnection)url.openConnection();

    if (lastModified > 0) {
      connection.addRequestProperty(HttpHeaders.IF_MODIFIED_SINCE, httpDateFormat.format(new Date(lastModified)));
      connection.addRequestProperty(HttpHeaders.CACHE_CONTROL, "max-age=0");
    }

    int responseCode = connection.getResponseCode();

    try {
      if (responseCode == 200) {
        String lastModifiedResStr = connection.getHeaderField(HttpHeaders.LAST_MODIFIED);
        if (lastModifiedResStr == null) {
          throw new IOException(HttpHeaders.LAST_MODIFIED + " header can not be null");
        }

        Date lastModifiedRes;

        try {
          lastModifiedRes = httpDateFormat.parse(lastModifiedResStr);
        }
        catch (ParseException e) {
          throw Throwables.propagate(e);
        }

        FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
        file.setLastModified(lastModifiedRes.getTime());
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

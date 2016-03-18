package verifier.tests;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestData {
  private static Map<String, String> wellKnownResources = new HashMap<String, String>();

  static {
    wellKnownResources.put("AWSCloudFormation-0.3.9.zip", "https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=17907");
    wellKnownResources.put("AWSCloudFormation-0.3.16.zip", "https://plugins.jetbrains.com/plugin/download?pr=&updateId=19088");
    wellKnownResources.put("ideaIC-14.0.4.tar.gz", "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/14.0.4/ideaIC-14.0.4.zip");
    wellKnownResources.put("ideaIU-144.3600.7.zip", "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/144.3600.7/ideaIU-144.3600.7.zip");
    wellKnownResources.put("ruby-8.0.0.20160127.zip", "https://plugins.jetbrains.com/plugin/download?pr=idea&updateId=23753");
    wellKnownResources.put("KotlinIJ141-17.zip", "https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21835");
    wellKnownResources.put("Scala1_9_4.zip", "https://plugins.jetbrains.com/plugin/download?pr=idea_ce&updateId=21782");
  }

  public static File fetchResource(String id, boolean extract) throws NoSuchArchiverException, IOException {
    final File resource = onlyFetchResource(id);
    if (!extract) {
      return resource;
    }

    final File fetchRoot = getFetchRoot();
    final File dest = new File(fetchRoot, id + ".d");
    if (!dest.isDirectory()) {
      final File tmpdest = new File(fetchRoot, id + ".d.tmp");
      tmpdest.delete();
      tmpdest.mkdirs();

      System.err.println("Extracting " + resource + " to " + dest);

      final AbstractUnArchiver ua = createUnArchiver(resource);
      ua.enableLogging(new ConsoleLogger(Logger.LEVEL_WARN, ""));
      ua.setDestDirectory(tmpdest);
      ua.extract();

      stripTopLevelDirectory(tmpdest);

      tmpdest.renameTo(dest);
    }

    return dest;
  }

  private static void stripTopLevelDirectory(File tmpdest) {
    final String[] entries = tmpdest.list();
    if (entries == null || entries.length != 1 || !new File(tmpdest, entries[0]).isDirectory()) {
      return;
    }

    File topLevelEntry = new File(tmpdest, entries[0]);
    for (String entry : topLevelEntry.list()) {
      if (entry.equals(topLevelEntry.getName())) {
        continue;
      }

      new File(topLevelEntry, entry).renameTo(new File(tmpdest, entry));
    }

    topLevelEntry.delete();
  }

  private static AbstractUnArchiver createUnArchiver(File file) {
    final String name = file.getName().toLowerCase();

    if (name.endsWith(".tar.gz")) {
      return new TarGZipUnArchiver(file);
    } else if (name.endsWith(".tar.bz2")) {
      return new TarBZip2UnArchiver(file);
    } else if (name.endsWith(".zip")) {
      return new ZipUnArchiver(file);
    } else {
      throw new RuntimeException("Unable to extract - unknown file extension: " + name);
    }
  }

  private static File onlyFetchResource(String id) throws IOException {
    final String url = wellKnownResources.get(id);
    if (url == null) {
      throw new RuntimeException("Resource is not found: " + id);
    }

    final File fetchRoot = getFetchRoot();

    final File resourceFile = new File(fetchRoot, id);
    if (!resourceFile.exists()) {
      final File tmpfile = new File(fetchRoot, id + ".tmp");
      tmpfile.delete();

      System.err.println("Downloading " + url + " to " + resourceFile);
      downloadFile(url, tmpfile);

      tmpfile.renameTo(resourceFile);
    }

    return resourceFile;
  }

  private static File getFetchRoot() {
    final File fetchRoot = new File(System.getProperty("java.io.tmpdir"), "plugin-verifier-test-data-temp-cache");
    fetchRoot.mkdirs();

    return fetchRoot;
  }

  private static boolean isRedirected(Map<String, List<String>> header) {
    for (String hv : header.get(null)) {
      if (hv.contains(" 301 ") || hv.contains(" 302 ")) return true;
    }

    return false;
  }

  private static void downloadFile(String link, File file) throws IOException {
    URL url = new URL(link);
    HttpURLConnection http = (HttpURLConnection) url.openConnection();
    Map<String, List<String>> header = http.getHeaderFields();
    while (isRedirected(header)) {
      link = header.get("Location").get(0);
      System.err.println("Redirected to " + link);
      url = new URL(link);
      http = (HttpURLConnection) url.openConnection();
      header = http.getHeaderFields();
    }

    InputStream input = http.getInputStream();
    FileUtils.copyInputStreamToFile(input, file);
  }
}

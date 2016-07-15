package com.intellij.structure.impl.utils;

import com.intellij.structure.errors.IncorrectPluginException;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Sergey Patrikeev
 */
public class PluginExtractor {

  private static final int TEMP_DIR_ATTEMPTS = 10000;

  @NotNull
  public static File extractPlugin(@NotNull File archive) throws IOException, IncorrectPluginException {
    //TODO: add some caching of the extracted plugins

    File tmp = createTempDir(archive);

    final AbstractUnArchiver ua = createUnArchiver(archive);
    ua.enableLogging(new ConsoleLogger(Logger.LEVEL_WARN, ""));
    ua.setDestDirectory(tmp);
    ua.extract();

    /*
      Check if the given .zip file actually contains a single .jar entry (a.zip!/b.jar!/META-INF/plugin.xml)
      If so we should return this single .jar file because it is actually a plugin.
    */
    Collection<File> files = FileUtils.listFiles(tmp, new String[]{"jar"}, false);
    if (files.size() > 1) {
      throw new IncorrectPluginException("Plugin archive contains multiple .jar files representing plugins");
    }
    if (files.size() == 1) {
      File singleJar = files.iterator().next();

      //move this single jar outside from the extracted directory
      File file = File.createTempFile("plugin_", ".jar", getCacheDir());
      FileUtils.copyFile(singleJar, file);

      //delete firstly extracted directory
      FileUtils.deleteQuietly(tmp);
      return file;
    }

    stripTopLevelDirectory(tmp);
    FileUtils.forceDeleteOnExit(tmp);
    return tmp;
  }

  @NotNull
  private static File createTempDir(@NotNull File archive) throws IOException {
    File cacheDir = getCacheDir();
    String baseName = "plugin_" + archive.getName() + "_" + System.currentTimeMillis();
    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(cacheDir, baseName + counter);
      try {
        FileUtils.forceMkdir(tempDir);
        return tempDir;
      } catch (IOException ignored) {
      }
    }
    throw new IllegalStateException("Failed to create directory within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
  }

  @NotNull
  private static File getTempDirectory() {
    String tempDir = System.getProperty("intellij.structure.temp.dir");
    if (tempDir != null) {
      return new File(tempDir);
    }
    return FileUtils.getTempDirectory();
  }

  @NotNull
  private static File getCacheDir() throws IOException {
    final File dir = new File(getTempDirectory(), "intellij-plugin-structure-cache");
    if (!dir.isDirectory()) {
      if (dir.exists()) {
        FileUtils.forceDelete(dir);
      }
      try {
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new IOException("Unable to create plugins cache directory " + dir + " (check access permissions)", e);
      }
    }
    return dir;
  }

  private static void stripTopLevelDirectory(@NotNull File dir) throws IOException {
    final String[] entries = dir.list();
    if (entries == null || entries.length != 1 || !new File(dir, entries[0]).isDirectory()) {
      return;
    }

    File topLevelEntry = new File(dir, entries[0]);
    String[] list = topLevelEntry.list();
    if (list == null) {
      return;
    }

    if (topLevelEntry.isDirectory() && topLevelEntry.getName().equals("lib")) {
      //this plugin wrapped in a .zip-archive doesn't contain an intermediate plugin-name folder => no need to strip
      return;
    }

    for (String entry : list) {
      if (entry.equals(topLevelEntry.getName())) {
        continue;
      }

      File file = new File(topLevelEntry, entry);
      File dest = new File(dir, entry);
      if (!file.renameTo(dest)) {
        throw new IOException("Unable to strip the top level directory " + file);
      }
    }

    FileUtils.deleteQuietly(topLevelEntry);
  }


  @NotNull
  private static AbstractUnArchiver createUnArchiver(@NotNull File file) {
    final String name = file.getName().toLowerCase();

    if (name.endsWith(".tar.gz")) {
      return new TarGZipUnArchiver(file);
    } else if (name.endsWith(".tar.bz2")) {
      return new TarBZip2UnArchiver(file);
    } else if (name.endsWith(".zip")) {
      return new ZipUnArchiver(file);
    }
    throw new IllegalArgumentException("Unable to extract " + file + "- unknown file extension: " + name);
  }
}

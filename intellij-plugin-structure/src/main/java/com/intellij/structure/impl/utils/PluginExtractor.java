package com.intellij.structure.impl.utils;

import com.google.common.base.Throwables;
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
  public static File extractPlugin(@NotNull File archive) throws IOException {
    if (!JarsUtils.isZip(archive)) {
      throw new IllegalArgumentException("Must be a zip archive: " + archive);
    }

    File tempFile = extractPluginToTemporaryDirectory(archive);

    /*
      Check if the given .zip file actually contains a single .jar entry (a.zip!/b.jar!/META-INF/plugin.xml)
      If so we should return this single .jar file because it is actually a plugin.
    */
    Collection<File> files = FileUtils.listFiles(tempFile, new String[]{"jar"}, false);
    if (files.size() > 1) {
      FileUtils.deleteQuietly(tempFile);
      throw new IllegalArgumentException("Plugin archive contains multiple .jar files representing plugins");
    }
    if (files.size() == 1) {
      try {
        File singleJar = files.iterator().next();

        //move this single jar outside from the extracted directory
        File tmpFile = File.createTempFile("plugin_", ".jar", getCacheDir());
        try {
          FileUtils.copyFile(singleJar, tmpFile);
          return tmpFile;
        } catch (Throwable e) {
          FileUtils.deleteQuietly(tmpFile);
          Throwables.propagate(e);
        }
      } finally {
        //delete firstly extracted directory
        FileUtils.deleteQuietly(tempFile);
      }
    }

    try {
      stripTopLevelDirectory(tempFile);
    } catch (Throwable e) {
      FileUtils.deleteQuietly(tempFile);
      throw Throwables.propagate(e);
    }
    return tempFile;
  }

  @NotNull
  private static File extractPluginToTemporaryDirectory(@NotNull File pluginZip) throws IOException {
    File tmp = createTempDir("plugin_");
    try {
      final AbstractUnArchiver ua = createUnArchiver(pluginZip);
      ua.enableLogging(new ConsoleLogger(Logger.LEVEL_WARN, ""));
      ua.setDestDirectory(tmp);
      ua.extract();
    } catch (Throwable e) {
      FileUtils.deleteQuietly(tmp);
      throw Throwables.propagate(e);
    }
    return tmp;
  }

  //it's synchronized because otherwise there is a possibility of two threads creating the same directory
  @NotNull
  private synchronized static File createTempDir(String prefix) throws IOException {
    File tmpDirs = getCacheDir();
    String baseName = prefix + "_" + System.currentTimeMillis();
    IOException lastException = null;
    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(tmpDirs, baseName + "_" + counter);
      if (!tempDir.exists()) {
        try {
          FileUtils.forceMkdir(tempDir);
          return tempDir;
        } catch (IOException ioe) {
          lastException = ioe;
        }
      }
    }
    throw new IllegalStateException("Failed to create directory under " + tmpDirs.getAbsolutePath() + " within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "_0 to " + baseName + "_" + (TEMP_DIR_ATTEMPTS - 1) + ')', lastException);
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
      try {
        if (dir.exists()) {
          FileUtils.forceDelete(dir);
        }
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

    File badEntry = null;
    for (String entry : list) {
      File fileOrDir = new File(topLevelEntry, entry);
      if (entry.equals(topLevelEntry.getName())) {
        //The entry has the same name (.../dir/topLevelEntry/topLevelEntry), we can't move right now
        badEntry = fileOrDir;
      } else {
        FileUtils.moveToDirectory(fileOrDir, dir, false);
      }
    }

    if (badEntry != null) {
      File tempDir = createTempDir("tmp_");
      try {
        FileUtils.moveToDirectory(badEntry, tempDir, false);

        //assert the plugin-directory is empty now
        File[] files = topLevelEntry.listFiles();
        if (files != null && files.length > 0) {
          throw new IOException("Unable to strip plugin directory " + dir + " [topLevelEntry=" + topLevelEntry + "]");
        }

        FileUtils.forceDelete(topLevelEntry);
        FileUtils.moveToDirectory(new File(tempDir, badEntry.getName()), dir, false);
      } finally {
        FileUtils.deleteQuietly(tempDir);
      }
    } else {
      FileUtils.forceDelete(topLevelEntry);
    }

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

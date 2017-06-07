package com.intellij.structure.impl.utils;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class ZipUtil {

  public static void extractZip(@NotNull File pluginZip, @NotNull File destDir) throws IOException {
    try {
      final AbstractUnArchiver ua = createUnArchiver(pluginZip);
      ua.enableLogging(new ConsoleLogger(Logger.LEVEL_WARN, ""));
      ua.setDestDirectory(destDir);
      ua.extract();
    } catch (Throwable e) {
      FileUtils.deleteQuietly(destDir);
      throw Throwables.propagate(e);
    }
  }

  public static void archiveDirectory(File directory, File destination) throws IOException {
    AbstractArchiver archiver = createArchiver(destination);
    archiver.enableLogging(new ConsoleLogger(Logger.LEVEL_ERROR, "Unarchive logger"));
    archiver.addDirectory(directory, directory.getName() + "/");
    archiver.setDestFile(destination);
    archiver.createArchive();
  }

  @NotNull
  public static AbstractArchiver createArchiver(@NotNull File file) {
    final String name = file.getName().toLowerCase();

    if (name.endsWith(".tar.gz")) {
      return new TarArchiver();
    } else if (name.endsWith(".zip")) {
      return new ZipArchiver();
    } else if (name.endsWith(".jar")) {
      return new JarArchiver();
    }
    throw new IllegalArgumentException("Unable to extract " + file + "- unknown file extension: " + name);
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

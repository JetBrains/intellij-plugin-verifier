package com.jetbrains.pluginverifier.util;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Dennis.Ushakov
 */
public class Unzip {
  private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int len;

    while ((len = in.read(buffer)) >= 0)
      out.write(buffer, 0, len);

    in.close();
    out.close();
  }

  public static void unzipJars(final File where, final ZipFile zipFile) throws IOException {
    final Enumeration<? extends ZipEntry> entries = zipFile.entries();

    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();

      final String name = entry.getName();
      if (name.contains(".."))
        continue;
      if (!name.endsWith(".jar")) {
        continue;
      }

      final File entryFile = new File(where, name);
      Util.createParentDirs(entryFile);

      System.out.println("Extracting jar file: " + name);
      copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(entryFile)));
    }

    zipFile.close();
  }
}
package com.jetbrains.pluginverifier;

import com.google.common.io.Files;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.util.Unzip;
import com.jetbrains.pluginverifier.util.Util;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class JarDiscovery {
  public static IdeaPlugin createIdeaPlugin(final File pluginDir, final Idea idea) throws JDOMException, IOException {
    if (!pluginDir.exists()) return null;

    final File realDir;
    if (pluginDir.getName().endsWith(".zip")) {
      realDir = unzipPlugin(pluginDir);
    } else if (pluginDir.isDirectory()) {
      realDir = pluginDir;
    } else {
      Util.fail("Unknown input file: " + pluginDir);
      return null;
    }

    return new IdeaPlugin(idea, realDir);
  }

  private static File unzipPlugin(final File zipFile) throws IOException {
    final ZipFile zip = new ZipFile(zipFile);

    final File tempDir = Files.createTempDir();
    tempDir.deleteOnExit(); // Yes, I've read why deleteOnExit is evil

    final File pluginDir = new File(tempDir, zipFile.getName());
    assert pluginDir.mkdir();

    System.out.println("Unpacking plugin: " + zipFile.getName());
    Unzip.unzipJars(pluginDir, zip);
    System.out.println("Plugin unpacked to: " + pluginDir);

    return pluginDir;
  }
}

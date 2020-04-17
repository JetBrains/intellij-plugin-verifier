/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.utils;

import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class URLUtil {
  public static final String SCHEME_SEPARATOR = "://";
  public static final String FILE_PROTOCOL = "file";
  public static final String HTTP_PROTOCOL = "http";
  public static final String JAR_PROTOCOL = "jar";
  public static final String JAR_SEPARATOR = "!/";

  private URLUtil() {
  }

  /**
   * Opens a url stream. The semantics is the sames as {@link URL#openStream()}. The
   * separate method is needed, since jar URLs open jars via JarFactory and thus keep them
   * mapped into memory.
   */
  @NotNull
  public static InputStream openStream(@NotNull URL url) throws IOException {
    String protocol = url.getProtocol();
    return protocol.equals(JAR_PROTOCOL) ? openJarStream(url) : url.openStream();
  }

  @NotNull
  public static URL fileToUrl(@NotNull File file) throws IOException {
    return file.getCanonicalFile().toURI().toURL();
  }

  /**
   * Checks whether a local resource specified by {@code url} exists.
   * <p/>
   * Returns {@link ThreeState#UNSURE} if {@code url} points to a remote resource.
   */
  @NotNull
  public static ThreeState resourceExists(@NotNull URL url) {
    if (url.getProtocol().equals(FILE_PROTOCOL)) {
      return ThreeState.fromBoolean(urlToFile(url).exists());
    }
    if (url.getProtocol().equals(JAR_PROTOCOL)) {
      Pair<String, String> paths = splitJarUrl(url.getFile());
      if (paths == null) {
        return ThreeState.NO;
      }
      File file = new File(paths.getFirst());
      if (!file.isFile()) {
        return ThreeState.NO;
      }
      try (ZipFile zipFile = new ZipFile(file)) {
        return ThreeState.fromBoolean(zipFile.getEntry(paths.getSecond()) != null);
      } catch (IOException e) {
        return ThreeState.NO;
      }
    }
    return ThreeState.UNSURE;
  }

  @NotNull
  public static File urlToFile(@NotNull URL url) {
    try {
      return new File(url.toURI().getSchemeSpecificPart());
    } catch (URISyntaxException e) {
      String str = url.toString();
      if (str.contains(" ")) {
        try {
          return new File(new URL(str.replace(" ", "%20")).toURI());
        } catch (URISyntaxException | MalformedURLException e2) {
          throw new IllegalArgumentException("URL='" + url.toString() + "'", e2);
        }
      }
      throw new IllegalArgumentException("URL='" + url.toString() + "'", e);
    }
  }

  @NotNull
  private static InputStream openJarStream(@NotNull URL url) throws IOException {
    Pair<String, String> paths = splitJarUrl(url.getFile());
    if (paths == null) {
      throw new MalformedURLException(url.getFile());
    }

    final ZipFile zipFile = new ZipFile(paths.getFirst());
    ZipEntry zipEntry = zipFile.getEntry(paths.getSecond());
    if (zipEntry == null) {
      zipFile.close();
      throw new FileNotFoundException("Entry " + paths.getSecond() + " is not found in " + paths.getFirst());
    }

    return new FilterInputStream(zipFile.getInputStream(zipEntry)) {
      @Override
      public void close() throws IOException {
        super.close();
        zipFile.close();
      }
    };
  }

  @NotNull
  public static URL getJarEntryURL(@NotNull File file, @NotNull String pathInJar) throws MalformedURLException {
    String fileURL = file.toURI().toASCIIString().replace("!", "%21");
    return new URL(JAR_PROTOCOL + ':' + fileURL + JAR_SEPARATOR + StringsKt.trimStart(pathInJar, '/'));
  }

  /**
   * Splits .jar URL along a separator and strips "jar" and "file" prefixes, if any.
   * <p/>
   * Returns a pair of path to a .jar file and entry name inside a .jar,
   * or null if the URL does not contain a separator.
   * <p/>
   * E.g. "jar:file:///path/to/jar.jar!/resource.xml" is converted into ["/path/to/jar.jar", "resource.xml"].
   * <p>
   * Please note that the first part is platform-dependent.
   */
  @Nullable
  public static Pair<String, String> splitJarUrl(@NotNull String url) {
    int pivot = url.indexOf(JAR_SEPARATOR);
    if (pivot < 0) return null;

    String resourcePath = url.substring(pivot + JAR_SEPARATOR.length());
    String jarPath = url.substring(0, pivot);

    if (jarPath.startsWith(JAR_PROTOCOL + ":")) {
      jarPath = jarPath.substring(JAR_PROTOCOL.length() + 1);
    }

    if (jarPath.startsWith(FILE_PROTOCOL)) {
      try {
        jarPath = urlToFile(new URL(jarPath)).getPath().replace('\\', '/');
      } catch (Exception e) {
        jarPath = jarPath.substring(FILE_PROTOCOL.length());
        if (jarPath.startsWith(SCHEME_SEPARATOR)) {
          jarPath = jarPath.substring(SCHEME_SEPARATOR.length());
        } else if (jarPath.startsWith(":")) {
          jarPath = jarPath.substring(1);
        }
      }
    }

    return new Pair<>(jarPath, resourcePath);
  }
}

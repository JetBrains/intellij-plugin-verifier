/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.structure.impl.utils.xml;

import com.intellij.structure.impl.utils.StringUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

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
   *
   * @param url url
   * @return input stream
   * @throws IOException if problems
   */
  @NotNull
  public static InputStream openStream(@NotNull URL url) throws IOException {
    @NonNls String protocol = url.getProtocol();
    return protocol.equals(JAR_PROTOCOL) ? openRecursiveJarStream(url) : url.openStream();
  }

  @NotNull
  public static URL fileToUrl(@NotNull File file) throws IOException {
    return file.getCanonicalFile().toURI().toURL();
  }

  public static boolean resourceExists(@NotNull URL url) {
    try {
      InputStream inputStream = openStream(url);
      inputStream.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @NotNull
  public static InputStream openResourceStream(final URL url) throws IOException {
    try {
      return openStream(url);
    } catch (FileNotFoundException ex) {
      @NonNls final String protocol = url.getProtocol();
      String file = null;
      if (protocol.equals(FILE_PROTOCOL)) {
        file = url.getFile();
      } else if (protocol.equals(JAR_PROTOCOL)) {
        int pos = url.getFile().indexOf("!");
        if (pos >= 0) {
          file = url.getFile().substring(pos + 1);
        }
      }
      if (file != null && file.startsWith("/")) {
        InputStream resourceStream = URLUtil.class.getResourceAsStream(file);
        if (resourceStream != null) return resourceStream;
      }
      throw ex;
    }
  }

  @NotNull
  public static String unquote(@NotNull String urlString) {
    urlString = urlString.replace('/', File.separatorChar);
    return unescapePercentSequences(urlString);
  }

  private static int decode(char c) {
    if ((c >= '0') && (c <= '9'))
      return c - '0';
    if ((c >= 'a') && (c <= 'f'))
      return c - 'a' + 10;
    if ((c >= 'A') && (c <= 'F'))
      return c - 'A' + 10;
    return -1;
  }

  @NotNull
  public static File urlToFile(@NotNull URL url) throws URISyntaxException, MalformedURLException {
    try {
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      String str = url.toString();
      if (str.indexOf(' ') > 0) {
        return new File(new URL(StringUtil.replace(str, " ", "%20")).toURI());
      }
      throw e;
    }
  }

  @NotNull
  public static String unescapePercentSequences(@NotNull String s) {
    if (s.indexOf('%') == -1) {
      return s;
    }

    StringBuilder decoded = new StringBuilder();
    final int len = s.length();
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == '%') {
        List<Integer> bytes = new ArrayList<Integer>();
        while (i + 2 < len && s.charAt(i) == '%') {
          final int d1 = decode(s.charAt(i + 1));
          final int d2 = decode(s.charAt(i + 2));
          if (d1 != -1 && d2 != -1) {
            bytes.add(((d1 & 0xf) << 4 | d2 & 0xf));
            i += 3;
          } else {
            break;
          }
        }
        if (!bytes.isEmpty()) {
          final byte[] bytesArray = new byte[bytes.size()];
          for (int j = 0; j < bytes.size(); j++) {
            bytesArray[j] = bytes.get(j).byteValue();
          }
          decoded.append(new String(bytesArray, Charset.forName("UTF-8")));
          continue;
        }
      }

      decoded.append(c);
      i++;
    }
    return decoded.toString();
  }

  /**
   * Opens a .zip- (or .jar-) file stream which may be in some other .zip<p>
   * e.g. <i>jar:jar:file:/home/user/Documents/a.zip!/lib/b.jar!/META-INF/plugin.xml</i>
   * returns an input stream for plugin.xml
   *
   * @param url and url which represents a path to a zip, or to a .zip inside the other .zip
   * @return input stream of the resource
   * @throws IOException if URL is malformed or unable to open a stream
   */
  @NotNull
  public static InputStream openRecursiveJarStream(@NotNull URL url) throws IOException {
    String[] paths = splitUrl(url.toExternalForm());
    if (paths.length == 0) {
      throw new MalformedURLException(url.toExternalForm());
    }
    if (paths.length == 1) {
      try {
        return new ZipInputStream(new BufferedInputStream(FileUtils.openInputStream(new File(unquote(paths[0])))));
      } catch (FileNotFoundException e) {
        throw new MalformedURLException(url.toExternalForm());
      }
    }

    ZipFile zipFile = new ZipFile(unquote(paths[0]));
    ZipEntry entry = zipFile.getEntry(paths[1]);
    if (entry == null) {
      throw new FileNotFoundException("Entry " + Arrays.toString(paths) + " is not found");
    }

    InputStream inputStream = zipFile.getInputStream(entry);
    if (isJarOrZipEntry(entry.getName())) {
      inputStream = new ZipInputStream(inputStream);
    }

    if (paths.length == 2) {
      return inputStream;
    }

    if (!isJarOrZipEntry(entry.getName())) {
      throw new IOException("Entry " + entry.getName() + " inside " + paths[0] + " is not a .zip nor .jar archive.");
    }

    return openRecursiveJarStream((ZipInputStream) inputStream, paths, 2);
  }

  @NotNull
  private static InputStream openRecursiveJarStream(@NotNull final ZipInputStream zipStream, String[] entries, int pos) throws IOException {
    final String entry = entries[pos];

    ZipEntry zipEntry;
    while ((zipEntry = zipStream.getNextEntry()) != null) {
      if (StringUtil.equal(zipEntry.getName(), entry)) {

        if (pos == entries.length - 1) {
          if (isJarOrZipEntry(entry)) {
            return new ZipInputStream(zipStream);
          }
          return zipStream;
        }

        return openRecursiveJarStream(new ZipInputStream(zipStream), entries, pos + 1);
      }
    }

    throw new FileNotFoundException("Entry " + Arrays.toString(entries) + " is not found");
  }

  private static boolean isJarOrZipEntry(String entry) {
    return StringUtil.endsWithIgnoreCase(entry, ".jar") || StringUtil.endsWithIgnoreCase(entry, ".zip");
  }

  @NotNull
  public static URL getJarEntryURL(@NotNull File file, @NotNull String pathInJar) throws MalformedURLException {
    String fileURL = getFileEscapedUri(file);
    return new URL(JAR_PROTOCOL + ':' + fileURL + JAR_SEPARATOR + StringUtil.trimLeading(pathInJar, '/'));
  }

  @NotNull
  public static String[] splitUrl(@NotNull String path) {
    while (path.startsWith("jar:")) {
      path = StringUtil.trimStart(path, "jar:");
    }
    while (path.startsWith("file:")) {
      path = StringUtil.trimStart(path, "file:");
    }
    return path.split("\\!/");
  }

  @NotNull
  public static String getFileEscapedUri(@NotNull File file) {
    return StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
  }
}

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
package com.jetbrains.pluginverifier.utils.xml;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

  private URLUtil() { }

  /**
   * Opens a url stream. The semantics is the sames as {@link java.net.URL#openStream()}. The
   * separate method is needed, since jar URLs open jars via JarFactory and thus keep them
   * mapped into memory.
   */
  @NotNull
  public static InputStream openStream(@NotNull URL url) throws IOException {
    @NonNls String protocol = url.getProtocol();
    return protocol.equals(JAR_PROTOCOL) ? openJarStream(url) : url.openStream();
  }

  @NotNull
  public static InputStream openResourceStream(final URL url) throws IOException {
    try {
      return openStream(url);
    }
    catch(FileNotFoundException ex) {
      @NonNls final String protocol = url.getProtocol();
      String file = null;
      if (protocol.equals(FILE_PROTOCOL)) {
        file = url.getFile();
      }
      else if (protocol.equals(JAR_PROTOCOL)) {
        int pos = url.getFile().indexOf("!");
        if (pos >= 0) {
          file = url.getFile().substring(pos+1);
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
          }
          else {
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

  @NotNull
  private static InputStream openJarStream(@NotNull URL url) throws IOException {
    String[] paths = splitJarUrl(url.getFile());
    if (paths == null || paths.length == 1 || paths.length > 3) {
      throw new MalformedURLException(url.getFile());
    }

    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    final ZipFile zipFile = new ZipFile(unquote(paths[0]));
    ZipEntry zipEntry = zipFile.getEntry(paths[1]);
    if (zipEntry == null) {
      zipFile.close();
      throw new FileNotFoundException("Entry " + paths[1] + " not found in " + paths[0]);
    }

    InputStream in = null;

    if (paths.length == 2) {
      in = zipFile.getInputStream(zipEntry);
    }
    else {
      ZipInputStream innerJar = new ZipInputStream(zipFile.getInputStream(zipEntry));

      ZipEntry innerEntry;
      while ((innerEntry = innerJar.getNextEntry()) != null) {
        if (paths[2].equals(innerEntry.getName())) {
          in = innerJar;
          break;
        }
      }

      if (in == null) {
        zipFile.close();
        throw new FileNotFoundException("Entry " + paths[2] + " not found in " + paths[0] + "!/" + paths[1]);
      }
    }

    return new FilterInputStream(in) {
        @Override
        public void close() throws IOException {
          super.close();
          zipFile.close();
        }
      };
  }

  @Nullable
  public static String[] splitJarUrl(@NotNull String fullPath) {
    if (!fullPath.startsWith(FILE_PROTOCOL + ":")) return null;

    String path = fullPath.substring(FILE_PROTOCOL.length() + 1);

    return path.split("\\!/");
  }

}

package com.intellij.structure.impl.utils;

import com.intellij.structure.impl.utils.xml.URLUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class FileWrapperImpl extends FileWrapper {

  private final File myOriginalFile;
  private final URL myURL;
  private final boolean myIsZipWrapper;

  FileWrapperImpl(@NotNull File original, boolean isZipWrapper) throws IOException {
    myOriginalFile = original;
    myIsZipWrapper = isZipWrapper;
    if (isZipWrapper) {
      myURL = new URL("jar:" + StringUtil.replace(original.toURI().toASCIIString(), "!", "%21") + "!/");
    } else {
      myURL = original.toURI().toURL();
    }
  }

  FileWrapperImpl(@NotNull FileWrapper parent, @NotNull String relativePath) throws IOException {
    myOriginalFile = parent.getOriginalIOFile();
    myIsZipWrapper = parent.isZipWrapper();
    if (parent.isZipWrapper()) {
      myURL = new URL(parent.getUrl(), relativePath);
    } else {
      myURL = new File(myOriginalFile, relativePath).toURI().toURL();
    }
  }

  @Override
  public boolean exists() {
    try {
      InputStream stream = URLUtil.openStream(myURL);
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public File getOriginalIOFile() {
    return myOriginalFile;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public boolean isZipWrapper() {
    return myIsZipWrapper;
  }

  @Override
  public boolean isDirectory() {
    return myURL.getPath().endsWith("/");
  }

  @Override
  public List<FileWrapper> listFiles() {
    return Collections.singletonList((FileWrapper) this);
  }

  @Override
  public String getName() {
    return myURL.getFile();
  }

  @Override
  public String getPresentablePath() {
    return myURL.toExternalForm();
  }

  @Override
  public URL getUrl() throws MalformedURLException {
    return myURL;
  }

  @Override
  @NotNull
  public InputStream getInputStream() throws IOException {
    return URLUtil.openStream(myURL);
  }

  /*
  @NotNull
  private Map<String, ZipEntry> fillEntriesMap(List<? extends ZipEntry> entries) {
    Map<String, ZipEntry> entriesMap = new HashMap<String, ZipEntry>();
    for (ZipEntry entry : entries) {
      entriesMap.put(entry.getName(), entry);
    }
    return entriesMap;
  }

  @NotNull
  private List<ZipEntry> filterEntries(List<? extends ZipEntry> entries, ZipEntryFilter filter) {
    List<ZipEntry> result = new ArrayList<ZipEntry>();
    for (ZipEntry entry : entries) {
      if (filter.accept(entry)) {
        result.add(entry);
      }
    }
    return result;
  }

  private interface ZipEntryFilter {
    boolean accept(ZipEntry entry);
  }
  */
}

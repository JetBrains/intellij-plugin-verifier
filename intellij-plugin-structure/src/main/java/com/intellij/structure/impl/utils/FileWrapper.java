package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Helper class which allows to treat ZipFile as a regular java.io.File
 *
 * @author Sergey Patrikeev
 */
public abstract class FileWrapper implements Closeable {
  public static FileWrapperImpl createIOFileWrapper(@NotNull File file) throws IOException {
    return new FileWrapperImpl(file, false);
  }

  public static FileWrapperImpl createZipFileWrapper(@NotNull File zip) throws IOException {
    return new FileWrapperImpl(zip, true);
  }

  @NotNull
  public static FileWrapper createRelative(@NotNull FileWrapper parent, @NotNull String relativePath) throws IOException {
    return new FileWrapperImpl(parent, relativePath);
  }

  public abstract boolean isZipWrapper();

  public abstract boolean exists();

  public abstract File getOriginalIOFile();

  public abstract boolean isDirectory();

  public abstract List<FileWrapper> listFiles();

  public abstract String getName();

  public abstract String getPresentablePath();

  public abstract URL getUrl() throws MalformedURLException;

  @NotNull
  public abstract InputStream getInputStream() throws IOException;

}

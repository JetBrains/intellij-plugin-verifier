package com.intellij.structure.bytecode;

import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sergey Patrikeev
 */
public class ClassFile {
  private final String myClassName;
  private final byte[] myBytes;

  public ClassFile(@NotNull String className, @NotNull byte[] bytes) {
    myClassName = className;
    myBytes = bytes;
  }

  public ClassFile(@NotNull String className, @NotNull InputStream inputStream) throws IOException {
    myClassName = className;
    myBytes = ByteStreams.toByteArray(inputStream);
  }

  @NotNull
  public byte[] getBytes() {
    return myBytes;
  }

  @NotNull
  public String getClassName() {
    return myClassName;
  }
}

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
  private final byte[] myBytecode;

  public ClassFile(@NotNull String className, @NotNull byte[] bytecode) {
    myClassName = className;
    myBytecode = bytecode;
  }

  public ClassFile(@NotNull String className, @NotNull InputStream inputStream) throws IOException {
    myClassName = className;
    myBytecode = ByteStreams.toByteArray(inputStream);
  }

  @NotNull
  public byte[] getBytecode() {
    return myBytecode;
  }

  @NotNull
  public String getClassName() {
    return myClassName;
  }
}

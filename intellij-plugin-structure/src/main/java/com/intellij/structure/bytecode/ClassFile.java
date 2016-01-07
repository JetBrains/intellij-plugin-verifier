package com.intellij.structure.bytecode;

import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sergey Patrikeev
 */
public class ClassFile {
  private final byte[] myBytes;

  public ClassFile(byte[] bytes) {
    myBytes = bytes;
  }

  public ClassFile(@NotNull InputStream inputStream) throws IOException {
    myBytes = ByteStreams.toByteArray(inputStream);
  }

  public byte[] getBytes() {
    return myBytes;
  }
}

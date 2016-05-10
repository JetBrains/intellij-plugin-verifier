package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sergey Patrikeev
 */
public class AsmUtil {

  @NotNull
  public static ClassNode readClassNode(@NotNull String className, @NotNull InputStream inputStream) throws IOException {
    try {
      ClassNode node = new ClassNode();
      new ClassReader(inputStream).accept(node, 0);
      return node;
    } catch (RuntimeException e) {
      throw new IOException("Unable to read a class `" + className + "`. Perhaps it is an invalid class-file. " +
          "ASM internal exception " + getAsmProblemMessage(e) + ". You may try to recompile a class-file", e);
    }
  }

  private static String getAsmProblemMessage(RuntimeException e) {
    String message;
    if (e instanceof ArrayIndexOutOfBoundsException) {
      message = e.getClass().getName() + (e.getLocalizedMessage() != null ? " " + e.getLocalizedMessage() : "");
    } else {
      message = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getName();
    }
    return message;
  }
}

package com.jetbrains.plugin.structure.classes.utils;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
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

  @NotNull
  public static String readClassName(@NotNull File classFile) throws IOException {
    InputStream is = null;
    try {
      is = FileUtils.openInputStream(classFile);
      ClassReader classReader = new ClassReader(is);
      String className = classReader.getClassName();
      if (className == null) {
        throw new IOException("Unable to read class name from " + classFile);
      }
      return className;
    } catch (RuntimeException e) {
      throw new IOException("Unable to read class file from " + classFile, e);
    } finally {
      IOUtils.closeQuietly(is);
    }

  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull File classFile) throws IOException {
    InputStream is = null;
    try {
      is = FileUtils.openInputStream(classFile);
      String className = Files.getNameWithoutExtension(classFile.getName());
      return readClassNode(className, is);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}

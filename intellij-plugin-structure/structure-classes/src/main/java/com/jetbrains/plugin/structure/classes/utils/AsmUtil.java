package com.jetbrains.plugin.structure.classes.utils;

import com.google.common.io.Files;
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AsmUtil {

  @NotNull
  public static ClassNode readClassNode(@NotNull String className, @NotNull InputStream inputStream) throws IOException {
    try {
      ClassNode node = new ClassNode();
      new ClassReader(inputStream).accept(node, 0);
      return node;
    } catch (RuntimeException e) {
      throw new InvalidClassFileException(className, getAsmErrorMessage(e));
    }
  }

  private static String getAsmErrorMessage(RuntimeException e) {
    String message = e.getLocalizedMessage();
    return e.getClass().getName() + (message != null ? ": " + message : "");
  }

  @NotNull
  public static String readClassName(@NotNull File classFile) throws IOException {
    InputStream is = null;
    try {
      is = FileUtils.openInputStream(classFile);
      ClassReader classReader = new ClassReader(is);
      String className = classReader.getClassName();
      if (className == null) {
        throw new InvalidClassFileException(classFile.getName(), "class name is not available in bytecode.");
      }
      return className;
    } catch (RuntimeException e) {
      throw new InvalidClassFileException(classFile.getName(), getAsmErrorMessage(e));
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

package com.jetbrains.plugin.structure.classes.utils;

import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AsmUtil {

  @NotNull
  public static ClassNode readClassNode(@NotNull String className,
                                        @NotNull InputStream inputStream,
                                        boolean fully) throws IOException {
    try {
      ClassNode node = new ClassNode();
      int parsingOptions = fully ? 0 : (ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      new ClassReader(inputStream).accept(node, parsingOptions);
      return node;
    } catch (RuntimeException e) {
      throw new InvalidClassFileException(className, getAsmErrorMessage(e));
    }
  }

  @NotNull
  public static ClassNode readClassNode(@NotNull String className, @NotNull InputStream inputStream) throws IOException {
    return readClassNode(className, inputStream, true);
  }

  private static String getAsmErrorMessage(RuntimeException e) {
    String message = e.getLocalizedMessage();
    return e.getClass().getName() + (message != null ? ": " + message : "");
  }

  @NotNull
  public static String readClassName(@NotNull File classFile) throws IOException {
    try (InputStream is = FileUtils.openInputStream(classFile)) {
      ClassReader classReader = new ClassReader(is);
      String className = classReader.getClassName();
      if (className == null) {
        throw new InvalidClassFileException(classFile.getName(), "class name is not available in bytecode.");
      }
      return className;
    } catch (RuntimeException e) {
      throw new InvalidClassFileException(classFile.getName(), getAsmErrorMessage(e));
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className,
                                            @NotNull File classFile,
                                            boolean fully) throws IOException {
    try (InputStream is = FileUtils.openInputStream(classFile)) {
      return readClassNode(className, is, fully);
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className, @NotNull File classFile) throws IOException {
    return readClassFromFile(className, classFile, true);
  }
}

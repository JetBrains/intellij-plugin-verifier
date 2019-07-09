package com.jetbrains.plugin.structure.classes.utils;

import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException;
import kotlin.io.FilesKt;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AsmUtil {

  public static final int ASM_API_LEVEL = Opcodes.ASM7;

  @NotNull
  public static ClassNode readClassNode(@NotNull String className,
                                        @NotNull InputStream inputStream,
                                        boolean fully) throws InvalidClassFileException, IOException {
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
  public static ClassNode readClassNode(@NotNull String className, @NotNull InputStream inputStream) throws InvalidClassFileException, IOException {
    return readClassNode(className, inputStream, true);
  }

  private static String getAsmErrorMessage(RuntimeException e) {
    String message = e.getLocalizedMessage();
    return e.getClass().getName() + (message != null ? ": " + message : "");
  }

  @NotNull
  public static String readClassName(@NotNull File classFile) throws InvalidClassFileException, IOException {
    try (InputStream is = FileUtils.openInputStream(classFile)) {
      String className;
      try {
        className = new ClassReader(is).getClassName();
      } catch (RuntimeException e) {
        throw new InvalidClassFileException(FilesKt.getNameWithoutExtension(classFile), getAsmErrorMessage(e));
      }
      if (className == null) {
        throw new InvalidClassFileException(FilesKt.getNameWithoutExtension(classFile), "class name is not available in byte-code.");
      }
      return className;
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className,
                                            @NotNull File classFile,
                                            boolean fully) throws IOException, InvalidClassFileException {
    try (InputStream is = FileUtils.openInputStream(classFile)) {
      return readClassNode(className, is, fully);
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className, @NotNull File classFile) throws IOException, InvalidClassFileException {
    return readClassFromFile(className, classFile, true);
  }
}

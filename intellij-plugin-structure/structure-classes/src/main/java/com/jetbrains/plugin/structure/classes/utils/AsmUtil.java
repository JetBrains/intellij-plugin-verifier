/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.utils;

import com.jetbrains.plugin.structure.base.utils.FileUtilKt;
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsmUtil {

  public static final int ASM_API_LEVEL = Opcodes.ASM9;

  @NotNull
  public static ClassNode readClassNode(@NotNull CharSequence className,
                                        @NotNull InputStream inputStream,
                                        boolean fully) throws InvalidClassFileException, IOException {
    try {
      ClassNode node = new ClassNode();
      int parsingOptions = fully ? 0 : (ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      new ClassReader(inputStream).accept(node, parsingOptions);
      return node;
    } catch (ClosedFileSystemException e) {
      throw e;
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
  public static String readClassName(@NotNull Path classFile) throws InvalidClassFileException, IOException {
    try (InputStream is = Files.newInputStream(classFile)) {
      String className;
      try {
        className = new ClassReader(is).getClassName();
      } catch (RuntimeException e) {
        throw new InvalidClassFileException(FileUtilKt.getNameWithoutExtension(classFile), getAsmErrorMessage(e));
      }
      if (className == null) {
        throw new InvalidClassFileException(FileUtilKt.getNameWithoutExtension(classFile), "class name is not available in byte-code of " + classFile.toAbsolutePath());
      }
      return className;
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className,
                                            @NotNull Path classFile,
                                            boolean fully) throws IOException, InvalidClassFileException {
    try (InputStream is = Files.newInputStream(classFile)) {
      return readClassNode(className, is, fully);
    }
  }

  @NotNull
  public static ClassNode readClassFromFile(@NotNull String className, @NotNull Path classFile) throws IOException, InvalidClassFileException {
    return readClassFromFile(className, classFile, true);
  }
}

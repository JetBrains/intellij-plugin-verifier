package com.intellij.structure.bytecode;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Sergey Patrikeev
 */
public class AsmBytecode {

  @Nullable
  public static ClassNode convertToAsmNode(@Nullable ClassFile classFile) {
    if (classFile != null) {
      byte[] bytes = classFile.getBytes();
      ClassNode node = new ClassNode();
      new ClassReader(bytes).accept(node, 0);
      return node;
    }
    return null;
  }
}

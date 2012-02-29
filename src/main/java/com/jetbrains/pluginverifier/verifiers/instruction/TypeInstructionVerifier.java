package com.jetbrains.pluginverifier.verifiers.instruction;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.*;

/**
 * @author Dennis.Ushakov
 */
public class TypeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final ErrorRegister register) {
    if (!(instr instanceof TypeInsnNode)) return;
    final String className = ((TypeInsnNode)instr).desc;
    if(className == null || VerifierUtil.classExists(resolver, className)) return;
    register.registerError(clazz.name + "." + method.name, "class " + className + " not found");
  }
}

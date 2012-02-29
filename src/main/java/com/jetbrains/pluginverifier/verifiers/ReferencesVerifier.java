package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Dennis.Ushakov
 */
public class ReferencesVerifier implements Verifier {
  private final Resolver myResolver;
  private final VerificationContext myContext;
  private final ErrorRegister myErrors;

  public ReferencesVerifier(VerificationContext context, ErrorRegister errors) {
    myContext = context;
    myErrors = errors;
    myResolver = new Resolver(context.getIdeaClasses().getMoniker(), context.getPluginClasses(), context.getIdeaClasses());
  }

  public void verify() {
    final ClassPool pluginPool = myContext.getPluginClasses();

    final Collection<String> classes = pluginPool.getAllClasses();
    for (String className : classes) {
      final ClassNode node = pluginPool.getClassNode(className);

      if (node == null) {
        myErrors.registerError(className, "broken jar structure");
        return;
      }

      verifyClass(node);
    }
  }

  private void verifyClass(final ClassNode node) {
    for (ClassVerifier verifier : Verifiers.getClassVerifiers()) {
      verifier.verify(node, myResolver, myErrors);
    }

    for (Object o : node.methods) {
      final MethodNode method = (MethodNode) o;
      verifyMethod(node, method);
    }

    for (Object o : node.fields) {
      final FieldNode method = (FieldNode) o;
      verifyField(node, method);
    }
  }

  private void verifyField(final ClassNode node, final FieldNode method) {
    for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
      verifier.verify(node, method, myResolver, myErrors);
    }
  }

  private void verifyMethod(final ClassNode node, final MethodNode method) {
    for (MethodVerifier verifier : Verifiers.getMemberVerifiers()) {
      verifier.verify(node, method, myResolver, myErrors);
    }

    final InsnList instructions = method.instructions;
    for (Iterator i = instructions.iterator(); i.hasNext(); ) {
      AbstractInsnNode instruction = (AbstractInsnNode) i.next();
      verifyInstruction(node, method, instruction);
    }
  }

  private void verifyInstruction(final ClassNode node, final MethodNode method, final AbstractInsnNode instruction) {
    for (InstructionVerifier verifier : Verifiers.getInstructionVerifiers()) {
      verifier.verify(node, method, instruction, myResolver, myErrors);
    }
  }
}

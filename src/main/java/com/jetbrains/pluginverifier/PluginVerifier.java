package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class PluginVerifier implements ErrorRegister, Verifier {
  private final ClassPool myPluginPool;
  private final List<Resolver> myResolvers = new ArrayList<Resolver>();
  private final Map<String, Map<String, String>> myAlreadyFailed = new HashMap<String, Map<String, String>>();

  PluginVerifier(final ClassPool pluginPool,
                 final List<ClassPool> ideaPools) {

    myPluginPool = pluginPool;
    for (ClassPool pool : ideaPools) {
      myResolvers.add(new Resolver(pool.getName(), pluginPool, pool));
      myAlreadyFailed.put(pool.getName(), null);
    }
  }

  public void verify() {
    final Collection<String> classes = myPluginPool.getAllClasses();
    for (String className : classes) {
      final ClassNode node = myPluginPool.getClassNode(className);
      if (node == null) {
        for (Resolver resolver : myResolvers) {
          registerError(resolver.getName(), className, "broken jar structure");
        }
        return;
      }
      verifyClass(node);
    }
  }

  public boolean hasErrors() {
    for (Map.Entry<String, Map<String, String>> entry : myAlreadyFailed.entrySet()) {
      final Map<String, String> errorsMap = entry.getValue();

      if (errorsMap != null && errorsMap.size() > 0)
        return true;
    }

    return false;
  }

  private void verifyClass(final ClassNode node) {
    for (ClassVerifier verifier : Verifiers.getClassVerifiers()) {
      for (Resolver resolver : myResolvers) {
        verifier.verify(node, resolver, this);
      }
    }
    for (Object o : node.methods) {
      final MethodNode method = (MethodNode)o;
      verifyMethod(node, method);
    }
    for (Object o : node.fields) {
      final FieldNode method = (FieldNode)o;
      verifyField(node, method);
    }
  }

  private void verifyField(final ClassNode node, final FieldNode method) {
    for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
      for (Resolver resolver : myResolvers) {
        verifier.verify(node, method, resolver, this);
      }
    }
  }

  private void verifyMethod(final ClassNode node, final MethodNode method) {
    for (MethodVerifier verifier : Verifiers.getMemberVerifiers()) {
      for (Resolver resolver : myResolvers) {
        verifier.verify(node, method, resolver, this);
      }
    }
    final InsnList instructions = method.instructions;
    for (Iterator i = instructions.iterator(); i.hasNext();) {
      AbstractInsnNode instruction = (AbstractInsnNode)i.next();
      verifyInstruction(node, method, instruction);
    }
  }

  private void verifyInstruction(final ClassNode node, final MethodNode method, final AbstractInsnNode instruction) {
    for (InstructionVerifier verifier : Verifiers.getInstructionVerifiers()) {
      for (Resolver resolver : myResolvers) {
        verifier.verify(node, method, instruction, resolver, this);
      }
    }
  }

  public void registerError(final String resolverName, final String occurence, final String error) {
    Map<String, String> resolverMap = myAlreadyFailed.get(resolverName);
    if (resolverMap == null) {
      resolverMap = new HashMap<String, String>();
      myAlreadyFailed.put(resolverName, resolverMap);
    }
    final String oldError = resolverMap.get(occurence);
    resolverMap.put(occurence, oldError != null ? oldError + "; " + error : error);
  }

  public void dumpErrors() {
    System.out.println("Validation of " + myPluginPool.getName());
    for (Map.Entry<String, Map<String, String>> entry : myAlreadyFailed.entrySet()) {
      System.out.println("Validation for: " + entry.getKey());
      final Map<String, String> value = entry.getValue();
      final Set<Map.Entry<String, String>> entries = value != null ? value.entrySet() : null;
      if (entries == null || entries.isEmpty()) {
        System.out.println("PASSED");
        continue;
      }
      for (Map.Entry<String, String> errorEntry : entries) {
        System.out.println("  " + cleanupString(errorEntry.getKey()) + ": " + cleanupString(errorEntry.getValue()));
      }
    }
    System.out.println("=========================================");
  }

  private static String cleanupString(final String errorEntry) {
    return errorEntry.replaceAll("/", ".");
  }
}

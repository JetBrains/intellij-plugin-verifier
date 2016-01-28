package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.MethodSign;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class AbstractMethodVerifier implements ClassVerifier {
  @Override
  public void verify(ClassNode clazz, Resolver resolver, VerificationContext ctx) {
    if ((clazz.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
      return;
    }

    if (clazz.superName == null) return;

    ClassNode superClass = resolver.findClass(clazz.superName);
    if (superClass == null) return;

    if (!VerifierUtil.isAbstract(superClass) && clazz.interfaces.isEmpty()) {
      return; // optimization
    }

    Set<MethodSign> allSign = new HashSet<MethodSign>();

    for (MethodNode methodNode : (List<MethodNode>)clazz.methods) {
      allSign.add(new MethodSign(methodNode));
    }

    ClassNode p = superClass;

    Queue<String> queue = new LinkedList<String>((List<String>)clazz.interfaces);

    while (VerifierUtil.isAbstract(p)) {
      queue.addAll((List<String>)p.interfaces);

      for (MethodNode methodNode : (List<MethodNode>)p.methods) {
        if (allSign.add(new MethodSign(methodNode))) {
          if (VerifierUtil.isAbstract(methodNode)) {
            ctx.registerProblem(new MethodNotImplementedProblem(p.name + '#' + methodNode.name + methodNode.desc),
                                new ProblemLocation(clazz.name));
          }
        }
      }

      if (p.superName == null) {
        p = null;
        break;
      }

      p = resolver.findClass(p.superName);
      if (p == null) {
        return; // RETURN , don't check anymore because unknown class exists.
      }
    }

    Set<String> processedInterfaces = new HashSet<String>();

    while (!queue.isEmpty()) {
      String iName = queue.remove();

      if (!processedInterfaces.add(iName)) continue;

      final ClassNode i = resolver.findClass(iName);
      if (i == null) continue;

      if ((i.access & Opcodes.ACC_INTERFACE) == 0) {
        continue;
      }

      for (String anInterface : (List<String>)i.interfaces) {
        if (!processedInterfaces.contains(anInterface)) {
          queue.add(anInterface);
        }
      }

      for (MethodNode method : (List<MethodNode>)i.methods) {
        MethodSign sign = new MethodSign(method);

        while (!allSign.contains(sign) && p != null) {
          for (MethodNode m : (List<MethodNode>)p.methods) {
            allSign.add(new MethodSign(m));
          }

          if (p.superName == null) {
            p = null;
          }
          else {
            p = resolver.findClass(p.superName);
            if (p == null) {
              return; // RETURN , don't check anymore because unknown class exists.
            }
          }
        }

        if (!allSign.contains(sign)) {
          allSign.add(sign);

          if (VerifierUtil.isAbstract(method)) {
            ctx.registerProblem(new MethodNotImplementedProblem(i.name + '#' + method.name + method.desc),
                new ProblemLocation(clazz.name));
          }
        }
      }
    }
  }

}

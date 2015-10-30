package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.MethodSign;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class AbstractMethodVerifier implements ClassVerifier {
  @SuppressWarnings("unchecked")
  @Override
  public void verify(ClassNode clazz, Resolver resolver, VerificationContext ctx) {
    if (VerifierUtil.isAbstract(clazz) || VerifierUtil.isInterface(clazz)) {
      return;
    }

    //TODO: fix this
    if (clazz.superName == null) return;


    ClassNode superClass = resolver.findClass(clazz.superName);
    if (superClass == null) return;

    if (!VerifierUtil.isAbstract(superClass) && clazz.interfaces.isEmpty()) {
      return; // optimization
    }

    Set<MethodSign> allMethods = new HashSet<MethodSign>();

    List<MethodNode> methods = (List<MethodNode>) clazz.methods;
    for (MethodNode methodNode : methods) {
      allMethods.add(new MethodSign(methodNode));
    }

    ClassNode curNode = superClass;

    Queue<String> parentInterfaces = new LinkedList<String>((List<String>) clazz.interfaces);

    //traverse abstract super-classes and collect all the defined interfaces
    while (VerifierUtil.isAbstract(curNode)) {
      parentInterfaces.addAll((List<String>) curNode.interfaces);

      for (MethodNode methodNode : (List<MethodNode>) curNode.methods) {
        if (allMethods.add(new MethodSign(methodNode))) {
          //if it is an undefined method
          if (VerifierUtil.isAbstract(methodNode)) {
            //and it is abstract => undefined abstract => problem
            ctx.registerProblem(new MethodNotImplementedProblem(curNode.name + '#' + methodNode.name + methodNode.desc),
                new ProblemLocation(clazz.name));
          }
        }
      }

      if (curNode.superName == null) { //java.lang.Object
        curNode = null;
        break;
      }

      curNode = resolver.findClass(curNode.superName);
      if (curNode == null) {
        return; // RETURN , don't check anymore because unknown class exists.
      }
    }

    Set<String> processedInterfaces = new HashSet<String>();

    while (!parentInterfaces.isEmpty()) {
      String iName = parentInterfaces.remove();

      if (!processedInterfaces.add(iName)) continue; //if this interface is already visited

      final ClassNode iNode = resolver.findClass(iName);
      if (iNode == null) continue; //undefined class

      if (!VerifierUtil.isInterface(iNode)) {
        //TODO: it means that the interface is no more "interface", but regular "class"
        continue;
      }

      for (String anInterface : (List<String>) iNode.interfaces) {
        if (!processedInterfaces.contains(anInterface)) {
          parentInterfaces.add(anInterface); //add to the queue
        }
      }

      for (MethodNode methodNode : (List<MethodNode>) iNode.methods) {
        MethodSign method = new MethodSign(methodNode);

        while (!allMethods.contains(method) && curNode != null) {
          for (MethodNode m : (List<MethodNode>) curNode.methods) {
            allMethods.add(new MethodSign(m));
          }

          if (curNode.superName == null) {
            curNode = null;
          } else {
            curNode = resolver.findClass(curNode.superName);
            if (curNode == null) {
              return; // RETURN , don't check anymore because unknown class exists.
            }
          }
        }

        if (!allMethods.contains(method)) {
          allMethods.add(method);

          ctx.registerProblem(new MethodNotImplementedProblem(iNode.name + '#' + methodNode.name + methodNode.desc), new ProblemLocation(clazz.name));
        }
      }
    }
  }

}

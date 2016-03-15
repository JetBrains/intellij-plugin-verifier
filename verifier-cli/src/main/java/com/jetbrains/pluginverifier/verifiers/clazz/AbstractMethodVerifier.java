package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.utils.LocationUtils;
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
  public void verify(ClassNode clazz, Resolver resolver, VerificationContext ctx) throws VerificationError {
    if (VerifierUtil.isAbstract(clazz) || VerifierUtil.isInterface(clazz)) {
      return;
    }

    String superName = clazz.superName == null ? "java/lang/Object" : clazz.superName;

    ClassNode superClass = VerifierUtil.findClass(resolver, superName);
    if (superClass == null) {
      return; //unknown class
    }

    if (!VerifierUtil.isAbstract(superClass) && clazz.interfaces.isEmpty()) {
      return; // optimization
    }

    //all non-static and non-private methods which may be invoked on an instance of this class
    Set<MethodSign> allMethods = new HashSet<MethodSign>();

    List<MethodNode> methods = (List<MethodNode>) clazz.methods;
    for (MethodNode method : methods) {
      if (!VerifierUtil.isStatic(method) && !VerifierUtil.isPrivate(method)) {
        allMethods.add(new MethodSign(method));
      }
    }

    ClassNode curNode = superClass;

    Queue<String> definedInterfaces = new LinkedList<String>((List<String>) clazz.interfaces);

    //traverse abstract super-classes and collect all the defined interfaces
    while (VerifierUtil.isAbstract(curNode)) {
      definedInterfaces.addAll((List<String>) curNode.interfaces);

      for (MethodNode method : (List<MethodNode>) curNode.methods) {

        if (allMethods.add(new MethodSign(method))) {
          if (VerifierUtil.isAbstract(method)) { //if method is abstract => it is neither static nor private
            //undefined abstract => problem
            ctx.registerProblem(new MethodNotImplementedProblem(LocationUtils.getMethodLocation(curNode, method)), ProblemLocation.fromClass(clazz.name));

          }
        }
      }

      if (curNode.superName == null) { //java.lang.Object
        curNode = null;
        break;
      }

      curNode = VerifierUtil.findClass(resolver, superName);
      if (curNode == null) {
        //TODO: don't return silently
        return; // RETURN , don't check anymore because unknown class exists.
      }
    }

    Set<String> processedInterfaces = new HashSet<String>();

    while (!definedInterfaces.isEmpty()) {
      String iface = definedInterfaces.remove();

      if (!processedInterfaces.add(iface)) continue; //if this interface is already visited

      ClassNode iNode = VerifierUtil.findClass(resolver, iface);
      if (iNode == null) continue; //undefined class

      if (!VerifierUtil.isInterface(iNode)) {
        //TODO: it means that the interface is no more "interface", but regular "class"
        continue;
      }

      for (String anInterface : (List<String>) iNode.interfaces) {
        if (!processedInterfaces.contains(anInterface)) { //if some transitive interface is not visited yet
          definedInterfaces.add(anInterface); //add to the queue
        }
      }

      for (MethodNode methodNode : (List<MethodNode>) iNode.methods) {
        if (!VerifierUtil.isAbstract(methodNode)) {
          //method could be default interface method (Java 8)
          continue;
        }

        MethodSign method = new MethodSign(methodNode);

        //try to find this method in some ancestor class
        while (!allMethods.contains(method) && curNode != null) {
          for (MethodNode m : (List<MethodNode>) curNode.methods) {
            allMethods.add(new MethodSign(m));
          }

          if (curNode.superName == null) {
            curNode = null;
          } else {
            curNode = VerifierUtil.findClass(resolver, curNode.superName);

            if (curNode == null) {
              //TODO: don't return silently
              return; // RETURN , don't check anymore because unknown class exists.
            }
          }
        }

        if (!allMethods.contains(method)) {
          //failed to find such a method in any ancestor => method is not implemented
          allMethods.add(method);

          ctx.registerProblem(new MethodNotImplementedProblem(LocationUtils.getMethodLocation(iNode, methodNode)), ProblemLocation.fromClass(clazz.name));
        }
      }
    }
  }

}

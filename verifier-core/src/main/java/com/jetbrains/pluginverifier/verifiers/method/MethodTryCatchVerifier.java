package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import com.jetbrains.pluginverifier.verifiers.VContext;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class MethodTryCatchVerifier implements MethodVerifier {
  @SuppressWarnings("unchecked")
  @Override
  public void verify(ClassNode clazz, MethodNode method, Resolver resolver, VContext ctx) {
    List<TryCatchBlockNode> blocks = (List<TryCatchBlockNode>) method.tryCatchBlocks;
    for (TryCatchBlockNode block : blocks) {
      String catchException = block.type;
      if (catchException == null) continue;
      String descr = VerifierUtil.extractClassNameFromDescr(catchException);
      if (descr == null) continue;
      if (!VerifierUtil.classExistsOrExternal(ctx, resolver, descr)) {
        ctx.registerProblem(new ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method));
      }
    }
  }
}

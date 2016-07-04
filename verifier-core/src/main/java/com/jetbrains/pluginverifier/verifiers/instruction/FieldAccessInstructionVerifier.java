package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.AccessType;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.FieldNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IllegalFieldAccessProblem;
import com.jetbrains.pluginverifier.problems.fields.ChangeFinalFieldProblem;
import com.jetbrains.pluginverifier.problems.statics.InstanceAccessOfStaticFieldProblem;
import com.jetbrains.pluginverifier.problems.statics.StaticAccessOfInstanceFieldProblem;
import com.jetbrains.pluginverifier.utils.LocationUtils;
import com.jetbrains.pluginverifier.utils.ResolverUtil;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import com.jetbrains.pluginverifier.verifiers.VContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author Sergey Patrikeev
 */
public class FieldAccessInstructionVerifier implements InstructionVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VContext ctx) {
    if (!(instr instanceof FieldInsnNode)) return;
    FieldInsnNode node = (FieldInsnNode) instr;

    String fieldOwner = node.owner;
    if (fieldOwner.startsWith("[")) {
      //this is an array field => assume it does exist
      return;
    }
    fieldOwner = VerifierUtil.extractClassNameFromDescr(fieldOwner);
    if (fieldOwner == null) {
      return;
    }

    if (ctx.getVerifierOptions().isExternalClass(fieldOwner)) {
      //assume the external class contains the field
      return;
    }
    ClassNode ownerNode = VerifierUtil.findClass(resolver, clazz, fieldOwner, ctx);
    if (ownerNode == null) {
      ctx.registerProblem(new ClassNotFoundProblem(fieldOwner), ProblemLocation.fromMethod(clazz.name, method));
      return;
    }

    ResolverUtil.FieldLocation actualLocation = ResolverUtil.findField(resolver, ownerNode, node.name, node.desc, ctx);
    if (actualLocation == null) {

      if (VerifierUtil.hasUnresolvedParentClass(fieldOwner, resolver, ctx)) {
        //field owner has some unresolved class => most likely that this class contains(-ed) the sought-for field
        return;
      }

      String fieldLocation = LocationUtils.INSTANCE.getFieldLocation(ownerNode.name, node.name, node.desc);
      ctx.registerProblem(new FieldNotFoundProblem(fieldLocation), ProblemLocation.fromMethod(clazz.name, method));
      return;
    }

    //check that access permission exists
    checkAccess(actualLocation, ctx, resolver, clazz, method);


    int opcode = node.getOpcode();

    final String field = LocationUtils.INSTANCE.getFieldLocation(actualLocation.getClassNode().name, actualLocation.getFieldNode());

    if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
      if (!VerifierUtil.isStatic(actualLocation.getFieldNode())) { //TODO: "if the resolved field is not a static field or an interface field, getstatic throws an IncompatibleClassChangeError"
        ctx.registerProblem(new StaticAccessOfInstanceFieldProblem(field), ProblemLocation.fromMethod(clazz.name, method));
      }
    } else if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
      if (VerifierUtil.isStatic(actualLocation.getFieldNode())) {
        ctx.registerProblem(new InstanceAccessOfStaticFieldProblem(field), ProblemLocation.fromMethod(clazz.name, method));
      }
    }

    checkFinalModifier(opcode, actualLocation, ctx, clazz, method);

  }

  private void checkFinalModifier(int opcode, ResolverUtil.FieldLocation location, VContext ctx, ClassNode verifiedClass, MethodNode verifierMethod) {
    final String field = LocationUtils.INSTANCE.getFieldLocation(location.getClassNode().name, location.getFieldNode());

    if (VerifierUtil.isFinal(location.getFieldNode())) {
      if (opcode == Opcodes.PUTFIELD) {
        /*
        TODO: this check is according to the JVM 8 spec, but Kotlin and others violate it (Java 8 doesn't complain too)
        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<init>".equals(verifierMethod.name))) {
       */
        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name))) {
          ctx.registerProblem(new ChangeFinalFieldProblem(field), ProblemLocation.fromMethod(verifiedClass.name, verifierMethod));
        }
      }

      if (opcode == Opcodes.PUTSTATIC) {
//        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<clinit>".equals(verifierMethod.name))) {
        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name))) {
          ctx.registerProblem(new ChangeFinalFieldProblem(field), ProblemLocation.fromMethod(verifiedClass.name, verifierMethod));
        }
      }
    }

  }

  private void checkAccess(ResolverUtil.FieldLocation location, VContext ctx, Resolver resolver, ClassNode verifiedClass, MethodNode verifiedMethod) {
    ClassNode actualOwner = location.getClassNode();
    FieldNode actualField = location.getFieldNode();

    AccessType accessProblem = null;

    if (VerifierUtil.isPrivate(actualField)) {
      if (!StringUtil.equals(verifiedClass.name, actualOwner.name)) {
        //accessing to the private field of the other class
        accessProblem = AccessType.PRIVATE;
      }
    } else if (VerifierUtil.isProtected(actualField)) {
      if (!VerifierUtil.isAncestor(verifiedClass, actualOwner, resolver, ctx) && !VerifierUtil.haveTheSamePackage(verifiedClass, actualOwner)) {
        accessProblem = AccessType.PROTECTED;
      }
    } else if (VerifierUtil.isDefaultAccess(actualField)) {
      if (!VerifierUtil.haveTheSamePackage(verifiedClass, actualOwner)) {
        accessProblem = AccessType.PACKAGE_PRIVATE;
      }
    }

    if (accessProblem != null) {
      IllegalFieldAccessProblem problem = new IllegalFieldAccessProblem(LocationUtils.INSTANCE.getFieldLocation(actualOwner.name, actualField), accessProblem);
      ctx.registerProblem(problem, ProblemLocation.fromMethod(verifiedClass.name, verifiedMethod));
    }


  }
}

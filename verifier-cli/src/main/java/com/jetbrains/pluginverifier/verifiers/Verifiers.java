package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Plugin;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.verifiers.clazz.AbstractMethodVerifier;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier;
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.*;
import com.jetbrains.pluginverifier.verifiers.method.*;

/**
 * @author Dennis.Ushakov
 */
public class Verifiers {
  public static final Verifier[] PLUGIN_VERIFIERS = new Verifier[]{new ReferencesVerifier()};
  // TODO: add dynamic loading ??
  private static final FieldVerifier[] FIELD_VERIFIERS = new FieldVerifier[]{new FieldTypeVerifier()};

  private static final ClassVerifier[] CLASS_VERIFIERS = new ClassVerifier[]{
      new SuperClassVerifier(),
      new InterfacesVerifier(),
      new AbstractMethodVerifier()
  };
  private static final MethodVerifier[] METHOD_VERIFIERS = new MethodVerifier[]{
      new OverrideNonFinalVerifier(),
      new MethodReturnTypeVerifier(),
      new MethodArgumentTypesVerifier(),
      new MethodLocalVarsVerifier(),
      new MethodThrowsVerifier(),
      new MethodTryCatchVerifier()
  };

  private static final InstructionVerifier[] INSTRUCTION_VERIFIERS = new InstructionVerifier[]{
      new InvokeInstructionVerifier(),
      new TypeInstructionVerifier(),
      new LdcInstructionVerifier(),
      new MultiANewArrayInstructionVerifier(),
      new FieldAccessInstructionVerifier()
  };

  public static ClassVerifier[] getClassVerifiers() {
    return CLASS_VERIFIERS;
  }

  public static MethodVerifier[] getMemberVerifiers() {
    return METHOD_VERIFIERS;
  }

  public static FieldVerifier[] getFieldVerifiers() {
    return FIELD_VERIFIERS;
  }

  public static InstructionVerifier[] getInstructionVerifiers() {
    return INSTRUCTION_VERIFIERS;
  }

  public static void processAllVerifiers(Plugin plugin, VerificationContext ctx) throws VerificationError {
    for (Verifier verifier : PLUGIN_VERIFIERS) {
      verifier.verify(plugin, ctx);
    }
  }
}

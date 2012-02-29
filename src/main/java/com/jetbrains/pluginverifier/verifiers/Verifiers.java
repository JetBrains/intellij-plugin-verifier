package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.clazz.InterfacesVerifier;
import com.jetbrains.pluginverifier.verifiers.clazz.SuperClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldTypeVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InvokeInstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.TypeInstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import com.jetbrains.pluginverifier.verifiers.method.OverrideNonFinalVerifier;

/**
 * @author Dennis.Ushakov
 */
public class Verifiers {
  // TODO: add dynamic loading
  private static final ClassVerifier[] CLASS_VERIFIERS = new ClassVerifier[] {new SuperClassVerifier(), new InterfacesVerifier()};
  private static final MethodVerifier[] METHOD_VERIFIERS = new MethodVerifier[] {new OverrideNonFinalVerifier()};
  private static final FieldVerifier[] FIELD_VERIFIERS = new FieldVerifier[] {new FieldTypeVerifier()};
  private static final InstructionVerifier[] INSTRUCTION_VERIFIERS = new InstructionVerifier[] {new InvokeInstructionVerifier(), new TypeInstructionVerifier()};

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
}

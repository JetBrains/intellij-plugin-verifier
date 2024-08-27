package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * Provides a debug string representation of a type instruction.
 *
 * It is used mainly in IDE debugging to better describe ASM nodes.
 */
@Suppress("unused")
fun TypeInsnNode.toDebugString(): String = when (opcode) {
  NEW -> "NEW $desc"
  ANEWARRAY -> "ANEWARRAY $desc"
  CHECKCAST -> "CHECKLIST $desc"
  INSTANCEOF -> "INSTANCEOF $desc"
  else -> toString()
}

/**
 * Provides a debug string representation of a zero-operand instruction.
 *
 * It is used mainly in IDE debugging to better describe ASM nodes.
 */
@Suppress("unused")
fun InsnNode.toDebugString(): String = insnOpCodes[opcode] ?: toString()

private val insnOpCodes = mapOf(
  NOP to "NOP",
  ACONST_NULL to "ACONST_NULL",
  ICONST_M1 to "ICONST_M1",
  ICONST_0 to "ICONST_0",
  ICONST_1 to "ICONST_1",
  ICONST_2 to "ICONST_2",
  ICONST_3 to "ICONST_3",
  ICONST_4 to "ICONST_4",
  ICONST_5 to "ICONST_5",
  LCONST_0 to "LCONST_0",
  LCONST_1 to "LCONST_1",
  FCONST_0 to "FCONST_0",
  FCONST_1 to "FCONST_1",
  FCONST_2 to "FCONST_2",
  DCONST_0 to "DCONST_0",
  DCONST_1 to "DCONST_1",
  IALOAD to "IALOAD",
  LALOAD to "LALOAD",
  FALOAD to "FALOAD",
  DALOAD to "DALOAD",
  AALOAD to "AALOAD",
  BALOAD to "BALOAD",
  CALOAD to "CALOAD",
  SALOAD to "SALOAD",
  IASTORE to "IASTORE",
  LASTORE to "LASTORE",
  FASTORE to "FASTORE",
  DASTORE to "DASTORE",
  AASTORE to "AASTORE",
  BASTORE to "BASTORE",
  CASTORE to "CASTORE",
  SASTORE to "SASTORE",
  POP to "POP",
  POP2 to "POP2",
  DUP to "DUP",
  DUP_X1 to "DUP_X1",
  DUP_X2 to "DUP_X2",
  DUP2 to "DUP2",
  DUP2_X1 to "DUP2_X1",
  DUP2_X2 to "DUP2_X2",
  SWAP to "SWAP",
  IADD to "IADD",
  LADD to "LADD",
  FADD to "FADD",
  DADD to "DADD",
  ISUB to "ISUB",
  LSUB to "LSUB",
  FSUB to "FSUB",
  DSUB to "DSUB",
  IMUL to "IMUL",
  LMUL to "LMUL",
  FMUL to "FMUL",
  DMUL to "DMUL",
  IDIV to "IDIV",
  LDIV to "LDIV",
  FDIV to "FDIV",
  DDIV to "DDIV",
  IREM to "IREM",
  LREM to "LREM",
  FREM to "FREM",
  DREM to "DREM",
  INEG to "INEG",
  LNEG to "LNEG",
  FNEG to "FNEG",
  DNEG to "DNEG",
  ISHL to "ISHL",
  LSHL to "LSHL",
  ISHR to "ISHR",
  LSHR to "LSHR",
  IUSHR to "IUSHR",
  LUSHR to "LUSHR",
  IAND to "IAND",
  LAND to "LAND",
  IOR to "IOR",
  LOR to "LOR",
  IXOR to "IXOR",
  LXOR to "LXOR",
  I2L to "I2L",
  I2F to "I2F",
  I2D to "I2D",
  L2I to "L2I",
  L2F to "L2F",
  L2D to "L2D",
  F2I to "F2I",
  F2L to "F2L",
  F2D to "F2D",
  D2I to "D2I",
  D2L to "D2L",
  D2F to "D2F",
  I2B to "I2B",
  I2C to "I2C",
  I2S to "I2S",
  LCMP to "LCMP",
  FCMPL to "FCMPL",
  FCMPG to "FCMPG",
  DCMPL to "DCMPL",
  DCMPG to "DCMPG",
  IRETURN to "IRETURN",
  LRETURN to "LRETURN",
  FRETURN to "FRETURN",
  DRETURN to "DRETURN",
  ARETURN to "ARETURN",
  RETURN to "RETURN",
  ARRAYLENGTH to "ARRAYLENGTH",
  ATHROW to "ATHROW",
  MONITORENTER to "MONITORENTER",
  MONITOREXIT to "MONITOREXIT"
)
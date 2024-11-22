/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks.asm

import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode

/**
 * Creates an empty class with the following features:
 *
 * * inherits from `java.lang.Object`,
 * * has a single no-argument constructor (default constructor) with an empty body.
 */
fun publicClass(className: BinaryClassName): ClassNode {
  return ClassNode(ASM9).apply {
    name = className
    superName = "java/lang/Object"
    access = ACC_PUBLIC or ACC_SUPER
    methods.add(constructorPublicNoArg())
  }
}
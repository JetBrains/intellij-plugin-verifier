/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base

/**
 * Binary Class or Interface name.
 *
 * Examples:
 *
 * * `java/lang/String` is a binary name for `java.lang.String
 * * `java/util/Map$Entry` is a binary name for nested class `java.util.Map.Entry`
 *
 * See [Java Virtual Machine Specification §4.2.1](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.2.1)
 */
typealias BinaryClassName = CharSequence
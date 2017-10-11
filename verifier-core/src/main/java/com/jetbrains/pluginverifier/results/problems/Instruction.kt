package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.Presentable

enum class Instruction(private val type: String) : Presentable {
  GET_STATIC("getstatic"),
  PUT_STATIC("putstatic"),
  PUT_FIELD("putfield"),
  GET_FIELD("getfield"),
  INVOKE_VIRTUAL("invokevirtual"),
  INVOKE_INTERFACE("invokeinterface"),
  INVOKE_STATIC("invokestatic"),
  INVOKE_SPECIAL("invokespecial");

  override fun toString(): String = type

  override val shortPresentation: String = type

  override val fullPresentation: String = type
}
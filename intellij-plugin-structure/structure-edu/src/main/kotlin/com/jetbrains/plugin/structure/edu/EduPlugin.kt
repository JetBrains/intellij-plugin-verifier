/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor


data class EduPlugin(
  override val pluginName: String? = null,
  override val description: String? = null,
  override var vendor: String? = null,
  override var vendorEmail: String? = null,
  override var vendorUrl: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val pluginId: String? = null,
  override val pluginVersion: String? = null,
  val language: String? = null,
  val programmingLanguage: String? = null,
  val eduPluginVersion: String?,
  val eduStat: EduStat? = null

) : Plugin {

  override val url: String = ""
  override val changeNotes: String? = null

  val parsedEduVersion = EduFullPluginVersion.fromString(eduPluginVersion)
}

data class EduStat(
  val sections: List<String>,
  val lessons: List<String>,
  val tasks: Map<String, Int>
) {

  fun countInteractiveChallenges(): Int {
    val ideTasks = tasks[TaskType.IDE.id] ?: 0
    val outputTasks = tasks[TaskType.OUTPUT.id] ?: 0
    val eduTasks = tasks[TaskType.EDU.id] ?: 0
    return ideTasks + outputTasks + eduTasks
  }

  fun countQuizzes(): Int {
    return tasks[TaskType.CHOICE.id] ?: 0
  }

  fun countTheoryTasks(): Int {
    return tasks[TaskType.THEORY.id] ?: 0
  }
  
  companion object {
    fun fromDescriptor(descriptor: EduPluginDescriptor): EduStat {
      val allItems = descriptor.items.flatMap { it.items } + descriptor.items

      val sections = allItems.filter { it.type == ItemType.SECTION.id }.map { it.title }
      val lessons = allItems.filter { it.type == ItemType.LESSON.id || it.type.isBlank() }.map { it.title }
      val tasks = allItems.flatMap { it.taskList }.groupingBy { it.taskType }.eachCount()

      return EduStat(sections, lessons, tasks)
    }
  }
}

enum class TaskType(val id: String) {
  EDU("edu"),
  OUTPUT("output"),
  IDE("ide"),
  THEORY("theory"),
  CHOICE("choice")
}

enum class ItemType(val id: String) {
  LESSON("lesson"),
  SECTION("section")
}

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.FullyQualifiedClassName
import net.bytebuddy.ByteBuddy

internal fun ideaPlugin(pluginId: String = "someid",
                       pluginName: String = "someName",
                       vendor: String = "vendor",
                       sinceBuild: String = "131.1",
                       untilBuild: String = "231.1",
                       description: String = "this description is looooooooooong enough",
                       additionalDepends: String = "") = """
    <id>$pluginId</id>
    <name>$pluginName</name>
    <version>someVersion</version>
    ""<vendor email="vendor.com" url="url">$vendor</vendor>""
    <description>$description</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="$sinceBuild" until-build="$untilBuild"/>
    <depends>com.intellij.modules.platform</depends>
    $additionalDepends
  """

internal fun ContentBuilder.descriptor(header: String) {
  dir("META-INF") {
    file("plugin.xml") {
      """
          <idea-plugin>
            $header
          </idea-plugin>
        """
    }
  }
}

internal fun ContentBuilder.classBytes(className: FullyQualifiedClassName, byteBuddy: ByteBuddy) {
  val dynamicType = byteBuddy
    .subclass(Object::class.java)
    .name(className)
    .make()
  val simpleName = dynamicType.typeDescription.simpleName
  file("$simpleName.class", dynamicType.bytes)
}
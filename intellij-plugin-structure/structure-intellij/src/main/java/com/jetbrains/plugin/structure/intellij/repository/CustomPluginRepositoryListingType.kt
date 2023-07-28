package com.jetbrains.plugin.structure.intellij.repository

/**
 * Specifies format of xml file containing list of plugins in a [custom plugin repository] [CustomPluginRepository]
 */
enum class CustomPluginRepositoryListingType {
  /**
   * Format recommended for [custom plugin repositories](https://plugins.jetbrains.com/docs/intellij/custom-plugin-repository.html)
   * (except that <idea-version> tag is optional), for example:
   * ```
   * <plugins>
   *   <plugin id="fully.qualified.id.of.this.plugin" url="https://www.mycompany.com/my_repository/mypluginname.jar" version="major.minor.update">
   *     <idea-version since-build="181.3" until-build="191.*" />
   *   </plugin>
   *   <plugin id="id.of.different.plugin" url="https://www.otherserver.com/other_repository/differentplugin.jar" version="major.minor"/>
   * </plugins>
   * ```
   */
  SIMPLE,

  /**
   * Full-blown listing of a plugin repository, for example:
   *
   * ```
   * <plugin-repository>
   *  <category name="Category name">
   *    <idea-plugin size="42">
   *      <name>Plugin Name</name>
   *      <id>com.some.id</id>
   *      <version>1.0</version>
   *      <idea-version since-build="193.3210"/>
   *      <vendor>VendorName</vendor>
   *      <download-url>file.zip</download-url>
   *      <description>
   *        <![CDATA[
   *        Some description here.
   *        ]]>
   *      </description>
   *    </idea-plugin>
   *  </category>
   * </plugin-repository>
   * ```
   */
  PLUGIN_REPOSITORY
}

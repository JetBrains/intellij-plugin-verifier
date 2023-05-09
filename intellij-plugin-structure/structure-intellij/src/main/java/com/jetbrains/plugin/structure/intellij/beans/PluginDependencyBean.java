/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Plugin dependency mapped to {@code <depends>} element in <code>plugin.xml</code>.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html#3-dependency-declaration-in-pluginxml">Dependency Declaration in plugin.xml</a>
 */
public class PluginDependencyBean {
  @XmlAttribute(name = "optional") public Boolean optional;
  @XmlAttribute(name = "config-file") public String configFile;
  @XmlValue public String dependencyId;
}
/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import jakarta.xml.bind.annotation.XmlAttribute;


public class IdeaVersionBean {
  public static final String SINCE_BUILD_ATTRIBUTE_NAME = "since-build";
  public static final String UNTIL_BUILD_ATTRIBUTE_NAME = "until-build";

  @XmlAttribute(name = SINCE_BUILD_ATTRIBUTE_NAME) public String sinceBuild;
  @XmlAttribute(name = UNTIL_BUILD_ATTRIBUTE_NAME) public String untilBuild;
}
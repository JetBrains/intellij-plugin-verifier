package com.jetbrains.plugin.structure.impl.beans;

import javax.xml.bind.annotation.XmlAttribute;

public class IdeaVersionBean {
  @XmlAttribute(name = "since-build") public String sinceBuild;
  @XmlAttribute(name = "until-build") public String untilBuild;
}
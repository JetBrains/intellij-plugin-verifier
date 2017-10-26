package com.jetbrains.pluginverifier.repository.local.meta;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "plugin-repository")
class LocalRepositoryMetadataBean {
  @XmlElement(name = "category")
  List<CategoryBean> categories = new ArrayList<>();
}

class CategoryBean {
  @XmlElement(name = "idea-plugin")
  List<IdeaPluginBean> plugins = new ArrayList<>();
}

class IdeaPluginBean {
  @XmlElement(name = "name") public String name;
  @XmlElement(name = "id") public String id;
  @XmlElement(name = "version") public String version;
  @XmlElement(name = "vendor") public PluginVendorBean vendor;
  @XmlElement(name = "idea-version") public IdeaVersionBean ideaVersion;
  @XmlElement(name = "download-url") public String downloadUrl;
}

class PluginVendorBean {
  @XmlValue public String name;
}

class IdeaVersionBean {
  @XmlAttribute(name = "since-build") public String sinceBuild;
  @XmlAttribute(name = "until-build") public String untilBuild;
}

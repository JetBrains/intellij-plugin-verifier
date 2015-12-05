package com.jetbrains.pluginverifier.results;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement(name = "plugin-check-result")
public class PluginCheckResult {

  private Map<String, ProblemSet> ide;


  public PluginCheckResult() {
  }


}

package com.jetbrains.pluginverifier.results.plugin;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement(name = "ide-problems")
public class IdeProblemsDescriptor {

  private String myIde;

  private List<ProblemDescriptor> myProblems = new ArrayList<ProblemDescriptor>();

  public IdeProblemsDescriptor() {
  }

  public IdeProblemsDescriptor(String ide, List<ProblemDescriptor> problems) {
    myIde = ide;
    myProblems = problems;
  }

  @XmlAttribute
  public String getIde() {
    return myIde;
  }

  public void setIde(String ide) {
    myIde = ide;
  }

  @XmlElementRef
  public List<ProblemDescriptor> getProblems() {
    return myProblems;
  }

  public void setProblems(List<ProblemDescriptor> problems) {
    myProblems = problems;
  }
}

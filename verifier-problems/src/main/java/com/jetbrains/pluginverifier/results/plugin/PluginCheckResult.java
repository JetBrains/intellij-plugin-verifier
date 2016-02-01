package com.jetbrains.pluginverifier.results.plugin;

import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.*;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement(name = "plugin-check-result")
public class PluginCheckResult {

  private UpdateInfo myUpdateInfo;

  @XmlTransient
  private Map<String, ProblemSet> myIdeToProblems = new HashMap<String, ProblemSet>();

  public PluginCheckResult() {
  }

  public PluginCheckResult(UpdateInfo updateInfo, Map<String, ProblemSet> ideToProblems) {
    myUpdateInfo = updateInfo;
    myIdeToProblems = ideToProblems;
  }

  public Map<String, ProblemSet> getIdeToProblems() {
    return myIdeToProblems;
  }

  @XmlElement(name = "checked-plugin")
  public UpdateInfo getUpdateInfo() {
    return myUpdateInfo;
  }

  public void setUpdateInfo(UpdateInfo updateInfo) {
    myUpdateInfo = updateInfo;
  }

  @XmlElementRef
  public List<IdeProblemsDescriptor> getCheckedIdeasResults() {
    List<IdeProblemsDescriptor> descriptors = new ArrayList<IdeProblemsDescriptor>();

    for (Map.Entry<String, ProblemSet> entry : myIdeToProblems.entrySet()) {
      String ide = entry.getKey();
      Map<Problem, Set<ProblemLocation>> problemsMap = entry.getValue().asMap();

      List<ProblemDescriptor> problemDescriptors = new ArrayList<ProblemDescriptor>();
      for (Map.Entry<Problem, Set<ProblemLocation>> setEntry : problemsMap.entrySet()) {
        problemDescriptors.add(new ProblemDescriptor(setEntry.getKey(), new ArrayList<ProblemLocation>(setEntry.getValue())));
      }

      descriptors.add(new IdeProblemsDescriptor(ide, problemDescriptors));
    }

    return descriptors;
  }

  public void setCheckedIdeasResults(List<IdeProblemsDescriptor> ideasResults) {
    Map<String, ProblemSet> ideToProblems = new HashMap<String, ProblemSet>();

    for (IdeProblemsDescriptor ideasResult : ideasResults) {
      String ide = ideasResult.getIde();
      List<ProblemDescriptor> descriptors = ideasResult.getProblems();

      Map<Problem, Set<ProblemLocation>> problems = new HashMap<Problem, Set<ProblemLocation>>();
      for (ProblemDescriptor descriptor : descriptors) {
        problems.put(descriptor.getProblem(), new HashSet<ProblemLocation>(descriptor.getProblemLocations()));
      }

      ideToProblems.put(ide, new ProblemSet(problems));

    }

    myIdeToProblems = ideToProblems;
  }


}

package com.jetbrains.pluginverifier.results.plugin;

import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Sergey Patrikeev
 */
@XmlRootElement(name = "problem")
public class ProblemDescriptor {

  private Problem myProblem;

  private List<ProblemLocation> myProblemLocations;

  public ProblemDescriptor() {
  }

  public ProblemDescriptor(Problem problem, List<ProblemLocation> problemLocations) {
    myProblem = problem;
    myProblemLocations = problemLocations;
  }

  @XmlElementRef
  public Problem getProblem() {
    return myProblem;
  }

  public void setProblem(Problem problem) {
    myProblem = problem;
  }

  @XmlElementRef
  public List<ProblemLocation> getProblemLocations() {
    return myProblemLocations;
  }

  public void setProblemLocations(List<ProblemLocation> problemLocations) {
    myProblemLocations = problemLocations;
  }
}

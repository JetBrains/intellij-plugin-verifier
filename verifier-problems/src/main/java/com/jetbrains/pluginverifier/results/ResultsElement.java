package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "results")
public class ResultsElement {

  private String ide;

  private List<Problem> problems = new ArrayList<Problem>();

  private List<UpdateInfo> updates = new ArrayList<UpdateInfo>();

  private String map = "";

  @XmlAttribute
  public String getIde() {
    return ide;
  }

  public void setIde(String ide) {
    this.ide = ide;
  }

  @XmlElementRef
  public List<Problem> getProblems() {
    return problems;
  }

  public void setProblems(List<Problem> problems) {
    this.problems = problems;
  }

  @XmlElementRef
  public List<UpdateInfo> getUpdates() {
    return updates;
  }

  public void setUpdates(List<UpdateInfo> updates) {
    this.updates = updates;
  }

  public String getMap() {
    return map;
  }

  public void setMap(String map) {
    this.map = map;
  }

  public Map<UpdateInfo, Collection<Problem>> asMap() {
    Map<UpdateInfo, Collection<Problem>> res = new LinkedHashMap<UpdateInfo, Collection<Problem>>();

    Scanner sc = new Scanner(map);

    for (UpdateInfo update : updates) {
      Collection<Problem> problems;

      int problemsCount = sc.nextInt();
      if (problemsCount == 0) {
        problems = Collections.emptyList();
      }
      else {
        problems = new ArrayList<Problem>(problemsCount);
        for (int i = 0; i < problemsCount; i++) {
          problems.add(this.problems.get(sc.nextInt()));
        }
      }

      res.put(update, problems);
    }

    return res;
  }

  public void initFromMap(Map<UpdateInfo, Collection<Problem>> map) {
    problems.clear();
    updates.clear();

    StringWriter s = new StringWriter();

    //for each problem its unique id
    LinkedHashMap<Problem, Integer> problemIndexMap = new LinkedHashMap<Problem, Integer>();

    int idx = 0;

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
      UpdateInfo update = entry.getKey();
      Collection<Problem> problemSet = entry.getValue();

      //number of problems for this plugin
      s.append(String.valueOf(problemSet.size()));

      for (Problem problem : problemSet) {
        Integer problemIndex = problemIndexMap.get(problem);

        if (problemIndex == null) {
          problemIndex = idx++;
          problemIndexMap.put(problem, problemIndex);
        }

        //set of numbers of problems for this plugin
        s.append(' ').append(String.valueOf(problemIndex));
      }

      s.append('\n');

      updates.add(update);
    }

    problems.addAll(problemIndexMap.keySet());
    this.map = s.toString();
  }
}

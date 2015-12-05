package com.jetbrains.pluginverifier.commands;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Sergey Patrikeev
 */
public class TempMain {
  public static void main(String[] args) throws IOException, JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(PluginCheckResult.class);
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    PluginCheckResult checkResult = new PluginCheckResult();
    PrSet prSet = new PrSet();
    prSet.problems.put(new Problem("Not such method"), Collections.singleton(new PrLoc("Problem-location")));


    Map<String, PrSet> map = new HashMap<String, PrSet>();
    map.put("IDEA-IU-143", prSet);

    PrSet prSet2 = new PrSet();
    Map<Problem, Set<PrLoc>> lalalala = new HashMap<Problem, Set<PrLoc>>();
    lalalala.put(new Problem("No such No Such problem"), Collections.singleton(new PrLoc("Lalalala")));
    lalalala.put(new Problem("No such No such problem"), Collections.singleton(new PrLoc("Lalalala2222")));
    prSet2.setProblems(lalalala);

    map.put("IDEA-IU-155", prSet2);


    checkResult.setMap(map);

    marshaller.marshal(checkResult, new File("hello.xml"));

  }

  @XmlRootElement(name = "plugin-check-results")
  private static class PluginCheckResult {

    private Map<String, PrSet> map = new HashMap<String, PrSet>();

    public PluginCheckResult() {
    }

    @XmlElementWrapper(name = "ide-check")
    public Map<String, PrSet> getMap() {
      return map;
    }

    public void setMap(Map<String, PrSet> map) {
      this.map = map;
    }
  }

  @XmlRootElement(name = "problem-type")
  private static class MyProblemsType {
    public List<MyProblemsEntry> problemsa = new ArrayList<MyProblemsEntry>();
  }

  @XmlRootElement
  private static class MyProblemsEntry {

    @XmlElement
    public Problem myProblem;

    @XmlElement
    public List<PrLoc> list;
  }

  @XmlRootElement(name = "b_class")
  @XmlAccessorType(XmlAccessType.FIELD)
  private static class PrSet {

    @XmlJavaTypeAdapter(Adapter.class)
    private Map<Problem, Set<PrLoc>> problems = new HashMap<Problem, Set<PrLoc>>();

    public Map<Problem, Set<PrLoc>> getProblems() {
      return problems;
    }

    public void setProblems(Map<Problem, Set<PrLoc>> problems) {
      this.problems = problems;
    }
  }


  private static class Adapter extends XmlAdapter<MyProblemsType, Map<Problem, Set<PrLoc>>> {

    @Override
    public MyProblemsType marshal(Map<Problem, Set<PrLoc>> v) throws Exception {
      MyProblemsType problemsType = new MyProblemsType();
      List<MyProblemsEntry> entries = new ArrayList<MyProblemsEntry>();
      for (Map.Entry<Problem, Set<PrLoc>> entry : v.entrySet()) {
        MyProblemsEntry e = new MyProblemsEntry();
        e.myProblem = entry.getKey();
        e.list = new ArrayList<PrLoc>(entry.getValue());
        entries.add(e);
      }
      problemsType.problemsa = entries;
      return problemsType;
    }

    @Override
    public Map<Problem, Set<PrLoc>> unmarshal(MyProblemsType v) throws Exception {
      Map<Problem, Set<PrLoc>> map = new HashMap<Problem, Set<PrLoc>>();
      for (MyProblemsEntry problemsEntry : v.problemsa) {
        Problem problem = problemsEntry.myProblem;
        List<PrLoc> list = problemsEntry.list;

        map.put(problem, new HashSet<PrLoc>(list));
      }
      return map;
    }
  }

  @XmlRootElement
  private static class Problem {

    public String problem;

    public Problem(String problem) {
      this.problem = problem;
    }

    public Problem() {
    }
  }

  @XmlRootElement
  private static class PrLoc {

    public String location;

    public PrLoc(String location) {
      this.location = location;
    }

    public PrLoc() {
    }
  }
}

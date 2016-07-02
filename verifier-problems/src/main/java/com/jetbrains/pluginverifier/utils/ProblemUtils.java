package com.jetbrains.pluginverifier.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.CodeLocation;
import com.jetbrains.pluginverifier.location.PluginLocation;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.problems.fields.ChangeFinalFieldProblem;
import com.jetbrains.pluginverifier.problems.statics.*;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.results.ResultsElement;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ProblemUtils {

  public static final Comparator<UpdateInfo> UPDATE_INFO_VERSIONS_COMPARATOR = new Comparator<UpdateInfo>() {
    @Override
    public int compare(UpdateInfo o1, UpdateInfo o2) {
      String p1 = o1.getPluginId() != null ? o1.getPluginId() : "#" + o1.getUpdateId();
      String p2 = o2.getPluginId() != null ? o2.getPluginId() : "#" + o2.getUpdateId();
      if (!p1.equals(p2)) {
        return p1.compareTo(p2); //compare lexicographically
      }
      return VersionComparatorUtil.compare(o2.getVersion(), o1.getVersion());
    }
  };
  private static final JAXBContext JAXB_CONTEXT;

  static {
    try {
      //if necessary add problem here (and add default constructor for it)
      JAXB_CONTEXT = JAXBContext.newInstance(
          //--------PROBLEMS--------
          Problem.class,
          ClassNotFoundProblem.class,
          CyclicDependenciesProblem.class,
          FailedToReadClassProblem.class,
          IllegalMethodAccessProblem.class,
          IllegalFieldAccessProblem.class,
          IncompatibleClassChangeProblem.class,
          MethodNotFoundProblem.class,
          FieldNotFoundProblem.class,
          MethodNotImplementedProblem.class,
          OverridingFinalMethodProblem.class,
          MissingDependencyProblem.class,
          ChangeFinalFieldProblem.class,
          InheritFromFinalClassProblem.class,

          InstanceAccessOfStaticFieldProblem.class,
          StaticAccessOfInstanceFieldProblem.class,

          AbstractClassInstantiationProblem.class,
          InterfaceInstantiationProblem.class,

          InvokeInterfaceOnPrivateMethodProblem.class,
          InvokeInterfaceOnStaticMethodProblem.class,
          InvokeSpecialOnStaticMethodProblem.class,
          InvokeStaticOnInstanceMethodProblem.class,
          InvokeVirtualOnStaticMethodProblem.class,

          ProblemLocation.class,
          CodeLocation.class,
          PluginLocation.class,

          //--------RESULT-ELEMENTS--------

          ResultsElement.class,
          UpdateInfo.class,
          ProblemSet.class
      );
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private static Marshaller createMarshaller() {
    try {
      return JAXB_CONTEXT.createMarshaller();
    } catch (JAXBException e) {
      throw new RuntimeException("Failed to create marshaller");
    }
  }

  private static Unmarshaller createUnmarshaller() {
    try {
      return JAXB_CONTEXT.createUnmarshaller();
    } catch (JAXBException e) {
      throw new RuntimeException("Failed to create unmarshaller");
    }
  }

  @NotNull
  private static String problemToString(@NotNull Problem problem, boolean format) {
    try {
      Marshaller marshaller = JAXB_CONTEXT.createMarshaller();

      if (format) {
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      }

      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

      StringWriter writer = new StringWriter();

      marshaller.marshal(problem, writer);

      return writer.toString();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  public static ResultsElement loadProblems(@NotNull File xml) throws IOException {
    return (ResultsElement) loadFromFile(xml);
  }

  @NotNull
  public static ResultsElement loadProblems(@NotNull InputStream inputStream) throws IOException {
    return (ResultsElement) loadFromStream(inputStream);
  }

  @NotNull
  public static Object loadFromStream(@NotNull InputStream inputStream) throws IOException {
    try {
      return loadObject(inputStream);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  @NotNull
  private static Object loadFromFile(@NotNull File xml) throws IOException {
    InputStream inputStream = new BufferedInputStream(new FileInputStream(xml));
    return loadFromStream(inputStream);
  }

  public static void saveProblems(@NotNull File output,
                                  @NotNull IdeVersion ideVersion,
                                  @NotNull Map<UpdateInfo, Collection<Problem>> problems)
      throws IOException {
    ResultsElement resultsElement = new ResultsElement();

    resultsElement.setIde(ideVersion.asString());
    resultsElement.initFromMap(problems);

    marshallObject(output, resultsElement);
  }

  private static void marshallObject(@NotNull File output, @NotNull Object o)
      throws IOException {
    Files.createParentDirs(output);

    Marshaller marshaller = createMarshaller();

    try {
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      marshaller.marshal(o, output);
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  @NotNull
  private static Object loadObject(@NotNull InputStream inputStream) throws IOException {
    Unmarshaller unmarshaller = createUnmarshaller();

    try {
      return unmarshaller.unmarshal(inputStream);
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }


  @NotNull
  public static List<Problem> sortProblems(@NotNull Collection<Problem> problems) {
    List<Problem> res = new ArrayList<Problem>(problems);
    Collections.sort(res, new ToStringProblemComparator());
    return res;
  }

  @NotNull
  public static String hash(@NotNull Problem problem) {
    String s = problemToString(problem, false);
    return Hashing.md5().hashString(s, Charset.defaultCharset()).toString();
  }

  /**
   * In DESCENDING order of versions
   */
  public static List<UpdateInfo> sortUpdatesWithDescendingVersionsOrder(@NotNull Collection<UpdateInfo> updateInfos) {
    List<UpdateInfo> sorted = new ArrayList<UpdateInfo>(updateInfos);
    Collections.sort(sorted, UPDATE_INFO_VERSIONS_COMPARATOR);
    return sorted;
  }

  /**
   * Transforms {@literal Map<Update -> [Problems]>  TO Multimap<Problem -> [Updates]>}
   */
  @NotNull
  public static Multimap<Problem, UpdateInfo> flipProblemsMap(@NotNull Map<UpdateInfo, Collection<Problem>> currentProblemsMap) {
    Multimap<Problem, UpdateInfo> currentProblemsToUpdates = ArrayListMultimap.create();

    //rearrange existing map: Map<Problem -> [plugin ids]>
    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : currentProblemsMap.entrySet()) {
      for (Problem problem : entry.getValue()) {
        currentProblemsToUpdates.put(problem, entry.getKey());
      }
    }
    return currentProblemsToUpdates;
  }
}

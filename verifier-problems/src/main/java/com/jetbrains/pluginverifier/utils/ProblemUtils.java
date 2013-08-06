package com.jetbrains.pluginverifier.utils;

import com.jetbrains.pluginverifier.problems.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ProblemUtils {

  private static final JAXBContext JAXB_CONTEXT;

  static {
    try {
      JAXB_CONTEXT = JAXBContext.newInstance(MethodNotFoundProblem.class,
                                             ClassNotFoundProblem.class,
                                             MethodNotImplementedProblem.class,
                                             OverridingFinalMethodProblem.class,
                                             SuperClassNotFoundProblem.class,
                                             DuplicateClassProblem.class,
                                             ResultsElement.class,
                                             UpdateElement.class,
                                             FailedToReadClassProblem.class);
    }
    catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public static Marshaller createMarshaller() {
    try {
      return JAXB_CONTEXT.createMarshaller();
    }
    catch (JAXBException e) {
      throw new RuntimeException("Failed to create marshaller");
    }
  }

  public static Unmarshaller createUnmarshaller() {
    try {
      return JAXB_CONTEXT.createUnmarshaller();
    }
    catch (JAXBException e) {
      throw new RuntimeException("Failed to create unmarshaller");
    }
  }

  public static String problemToString(@NotNull Problem problem, boolean format) {
    try {
      Marshaller marshaller = JAXB_CONTEXT.createMarshaller();

      if (format) {
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      }

      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

      StringWriter writer = new StringWriter();

      marshaller.marshal(problem, writer);

      return writer.toString();
    }
    catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<Integer, Set<Problem>> loadProblems(File xml) throws IOException {
    InputStream inputStream = new BufferedInputStream(new FileInputStream(xml));

    try {
      return loadProblems(inputStream, null);
    }
    finally {
      inputStream.close();
    }
  }

  public static void saveProblems(@NotNull File output, @NotNull String ide, @NotNull Map<Integer, List<Problem>> problems)
    throws IOException {
    ResultsElement resultsElement = new ResultsElement();
    resultsElement.setIde(ide);

    for (Map.Entry<Integer, List<Problem>> entry : problems.entrySet()) {
      UpdateElement updateElement = new UpdateElement();
      updateElement.setId(entry.getKey());
      updateElement.setProblem(new ArrayList<Problem>(entry.getValue()));

      resultsElement.getUpdate().add(updateElement);
    }

    Marshaller marshaller = createMarshaller();

    try {
      marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      marshaller.marshal(resultsElement, output);
    }
    catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  public static @NotNull Map<Integer, Set<Problem>> loadProblems(InputStream inputStream, @Nullable AtomicReference<String> ideIdRef) throws IOException {
    ResultsElement resultsElement = loadProblems(inputStream);

    if (ideIdRef != null) {
      ideIdRef.set(resultsElement.getIde());
    }

    Map<Integer, Set<Problem>> res = new HashMap<Integer, Set<Problem>>();

    for (UpdateElement updateElement : resultsElement.getUpdate()) {
      Set<Problem> set = updateElement.getProblem() == null || updateElement.getProblem().isEmpty() ? Collections.<Problem>emptySet() : new HashSet<Problem>(updateElement.getProblem());
      res.put(updateElement.getId(), set);
    }

    return res;
  }

  public static ResultsElement loadProblems(InputStream inputStream) throws IOException {
    Unmarshaller unmarshaller = createUnmarshaller();

    try {
      return (ResultsElement)unmarshaller.unmarshal(inputStream);

    }
    catch (JAXBException e) {
      throw new IOException(e);
    }
  }

}

package com.jetbrains.pluginverifier.utils;

import com.google.common.hash.Hashing;
import com.jetbrains.pluginverifier.problems.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ProblemUtils {

  private static final JAXBContext JAXB_CONTEXT;

  static {
    try {
      //TODO: if necessary add problem here
      JAXB_CONTEXT = JAXBContext.newInstance(MethodNotFoundProblem.class,
                                             ClassNotFoundProblem.class,
                                             MethodNotImplementedProblem.class,
                                             OverridingFinalMethodProblem.class,
                                             DuplicateClassProblem.class,
                                             ResultsElement.class,
                                             UpdateInfo.class,
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

  public static ResultsElement loadProblems(File xml) throws IOException {
    InputStream inputStream = new BufferedInputStream(new FileInputStream(xml));

    try {
      return loadProblems(inputStream);
    }
    finally {
      inputStream.close();
    }
  }

  public static void saveProblems(@NotNull File output, @NotNull String ide, @NotNull Map<UpdateInfo, Collection<Problem>> problems)
    throws IOException {
    ResultsElement resultsElement = new ResultsElement();

    resultsElement.setIde(ide);
    resultsElement.initFromMap(problems);

    saveProblems(output, resultsElement);
  }

  public static void saveProblems(@NotNull File output, ResultsElement resultsElement)
    throws IOException {
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

  public static ResultsElement loadProblems(InputStream inputStream) throws IOException {
    Unmarshaller unmarshaller = createUnmarshaller();

    try {
      return (ResultsElement)unmarshaller.unmarshal(inputStream);
    }
    catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  public static List<Problem> sort(Collection<Problem> problems) {
    List<Problem> res = new ArrayList<Problem>(problems);
    Collections.sort(res, new ToStringProblemComparator());
    return res;
  }

  public static String hash(Problem problem) {
    String s = problemToString(problem, false);
    return Hashing.md5().hashString(s, Charset.defaultCharset()).toString();
  }
}

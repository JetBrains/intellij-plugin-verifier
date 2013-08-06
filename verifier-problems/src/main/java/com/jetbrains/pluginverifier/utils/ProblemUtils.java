package com.jetbrains.pluginverifier.utils;

import com.jetbrains.pluginverifier.problems.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

  public static Map<Integer, Set<Problem>> loadProblems(InputStream inputStream, @Nullable AtomicReference<String> ideIdRef) throws IOException {
    Document document;

    try {
      document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
    }
    catch (SAXException e) {
      throw new IOException("Failed to parse problem list", e);
    }
    catch (ParserConfigurationException e) {
      throw new IOException("Failed to parse problem list", e);
    }

    Element root = document.getDocumentElement();
    if (!"results".equals(root.getTagName())) {
      throw new IOException("Invalid results file: root element must be 'results'");
    }

    String ideId = root.getAttribute("ide");
    if (ideId == null || ideId.isEmpty()) {
      throw new IOException("Invalid result file: 'ide' attribute is missing");
    }

    if (ideIdRef != null) {
      ideIdRef.set(ideId);
    }

    NodeList pluginBuilds = root.getElementsByTagName("update");

    Unmarshaller unmarshaller = createUnmarshaller();

    Map<Integer, Set<Problem>> res = new HashMap<Integer, Set<Problem>>();

    for (int i = 0; i < pluginBuilds.getLength(); i++) {
      Element e = (Element)pluginBuilds.item(i);

      String idAttribute = e.getAttribute("id");
      if (idAttribute == null || idAttribute.isEmpty()) {
        throw new IOException("Invalid result file: <build> element without 'id' attribute");
      }

      Integer buildId;
      try {
        buildId = Integer.parseInt(idAttribute);
      }
      catch (NumberFormatException e1) {
        throw new IOException("Invalid result file: <build> element with invalid 'id' attribute: " + idAttribute);
      }

      Set<Problem> problemSet = new HashSet<Problem>();

      NodeList problemNodes = e.getChildNodes();
      for (int k = 0; k < problemNodes.getLength(); k++) {
        Node problemNode = problemNodes.item(k);
        if (problemNode instanceof Element) {
          try {
            problemSet.add((Problem)unmarshaller.unmarshal(problemNode));
          }
          catch (JAXBException e1) {
            throw new IOException("Invalid result file: <build> element without 'id' attribute");
          }
        }
      }

      res.put(buildId, problemSet);
    }

    return res;
  }

}

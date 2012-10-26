package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.util.*;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Sergey Evdokimov
 */
public class IdeVerifier {

  public static void verifyIde(@NotNull Idea ide, @NotNull PluginVerifierOptions options) throws IOException {
    System.out.print("Loading compatible plugins list... ");

    String build = FileUtils.readFileToString(new File(ide.getIdeaDir(), "build.txt")).trim();
    if (build.length() == 0) {
      Util.fail("failed to read IDE version ($IDE_HOME/build.txt)");
    }

    List<Integer> pluginIds = new ArrayList<Integer>();

    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/allCompatibleUpdateIds/?build=" + build);
    String text = IOUtils.toString(url);

    for (String id : text.split("[,;\\s]+")) {
      pluginIds.add(Integer.parseInt(id));
    }

    System.out.println(pluginIds.size() + " compatible plugins found");

    Map<Integer, List<Problem>> results = new TreeMap<Integer, List<Problem>>();

    long time = System.currentTimeMillis();

    for (Integer id : pluginIds) {
      File update;
      try {
        update = DownloadUtils.getUpdate(id);
      }
      catch (IOException e) {
        System.out.println("failed to download: " + e.getMessage());
        continue;
      }

      IdeaPlugin plugin;
      try {
        plugin = IdeaPlugin.createFromZip(ide, update);
      }
      catch (Exception e) {
        System.out.println("Plugin is broken: #" + id);
        e.printStackTrace();
        continue;
      }

      System.out.print("testing plugin " + plugin.getId() + " (#" + id + ")... ");

      ProblemsCollector problemRegister = new ProblemsCollector();
      Verifiers.processAllVerifiers(plugin, options, problemRegister);

      results.put(id, problemRegister.getProblems());

      if (problemRegister.getProblems().isEmpty()) {
        System.out.println("ok");
      }
      else {
        System.out.println("has " + problemRegister.getProblems().size() + " errors");
        for (Problem problem : problemRegister.getProblems()) {
          System.out.print("    ");
          System.out.println(problem.getDescription());
        }
      }
    }

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + "s)");

    try {
      storeResults(new File(Util.getValidatorHome(), build + ".xml"), build, results);
    }
    catch (JAXBException e) {
      e.printStackTrace();
    }
  }

  private static void storeResults(File file, String build, Map<Integer, List<Problem>> results) throws IOException, JAXBException {
    System.out.print("Saving results to " + file);

    Writer writer = new BufferedWriter(new FileWriter(file), 1024*1024) ;

    Marshaller marshaller = ProblemUtils.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    try {
      writer.append("<results ide=\"").append(build).append("\">\n");

      for (Map.Entry<Integer, List<Problem>> entry : results.entrySet()) {
        List<Problem> problemList = entry.getValue();
        if (problemList.isEmpty()) {
          writer.append("<update id=\"").append(String.valueOf(entry.getKey())).append("\" />\n");
        }
        else {
          writer.append("<update id=\"").append(String.valueOf(entry.getKey())).append("\">\n");

          for (Problem problem : problemList) {
            marshaller.marshal(problem, writer);
            writer.append('\n');
          }

          writer.append("</update>\n");
        }
      }

      writer.append("</results>");
    }
    finally {
      writer.close();
    }

    System.out.println("  done");
  }

}

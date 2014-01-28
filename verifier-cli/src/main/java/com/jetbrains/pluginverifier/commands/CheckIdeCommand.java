package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.util.Configuration;
import com.jetbrains.pluginverifier.util.DownloadUtils;
import com.jetbrains.pluginverifier.util.UpdateJson;
import com.jetbrains.pluginverifier.util.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CheckIdeCommand extends VerifierCommand {

  private static final Type updateListType = new TypeToken<List<UpdateJson>>() {}.getType();

  public CheckIdeCommand() {
    super("check-ide");
  }

  private static List<String> extractPluginList(@NotNull CommandLine commandLine) {
    String[] pluginIds = commandLine.getOptionValues("pl");
    if (pluginIds == null) return Collections.emptyList();

    List<String> res = new ArrayList<String>();

    for (String pluginId : pluginIds) {
      for (StringTokenizer st = new StringTokenizer(pluginId, ",; "); st.hasMoreTokens(); ) {
        String token = st.nextToken();
        if (!token.isEmpty()) {
          res.add(token);
        }
      }
    }

    return res;
  }

  private static void removeExcludedPlugins(Collection<UpdateJson> updates, CommandLine commandLine) throws IOException {
    String epf = commandLine.getOptionValue("epf");
    if (epf != null) {
      String excludedBuildListStr = Files.toString(new File(epf), Charset.defaultCharset());
      List<UpdateJson> excludedBuildList = new Gson().fromJson(excludedBuildListStr, updateListType);
      for (UpdateJson excludedBuild : excludedBuildList) {
        for (Iterator<UpdateJson> itr = updates.iterator(); itr.hasNext(); ) {
          UpdateJson u = itr.next();
          if (excludedBuild.equalsByIdOrVersion(u)) {
            itr.remove();
          }
        }
      }
    }
  }

  @NotNull
  private String getIdeVersion(@NotNull Idea ide, @NotNull CommandLine commandLine) throws IOException {
    String build = commandLine.getOptionValue("iv");
    if (build == null || build.isEmpty()) {
      build = Files.toString(new File(ide.getIdeaDir(), "build.txt"), Charset.defaultCharset()).trim();
      if (build.length() == 0) {
        throw Util.fail("failed to read IDE version (" + ide.getIdeaDir() + "/build.txt)");
      }
    }

    return build;
  }

  private List<UpdateJson> getUpdateIds(@NotNull String ideVersion, @NotNull CommandLine commandLine) throws IOException {
    List<String> pluginIds = extractPluginList(commandLine);

    if (pluginIds.isEmpty()) {
      throw Util.fail("You have to specify list of plugins to check using -pl option");
    }

    System.out.println("Loading compatible plugins list... ");

    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/originalCompatibleUpdatesByPluginIds/?build=" +
                      ideVersion + "&pluginIds=" + Joiner
      .on("&pluginIds=").join(pluginIds));
    String text = IOUtils.toString(url);

    return new Gson().fromJson(text, updateListType);
  }

  @Override
  public void execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw Util.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"");
    }

    File ideToCheck = new File(freeArgs.get(0));
    if (!ideToCheck.isDirectory()) {
      throw Util.fail("IDE home is not a directory: " + ideToCheck);
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    ClassPool externalClassPath = getExternalClassPath(commandLine);

    Idea ide = new Idea(ideToCheck, jdk, externalClassPath);

    String ideVersion = getIdeVersion(ide, commandLine);
    List<UpdateJson> updateIds = getUpdateIds(ideVersion, commandLine);

    String dumpBrokenPluginsFile = commandLine.getOptionValue("d");
    String reportFile = commandLine.getOptionValue("report");

    boolean checkExcludedBuilds = dumpBrokenPluginsFile != null || reportFile != null;

    if (checkExcludedBuilds) {
      removeExcludedPlugins(updateIds, commandLine);
    }

    Map<UpdateJson, ProblemSet> results = new HashMap<UpdateJson, ProblemSet>();

    long time = System.currentTimeMillis();

    for (UpdateJson updateJson : updateIds) {
      File update;
      try {
        update = DownloadUtils.getUpdate(updateJson.getUpdateId());
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
        System.out.println("Plugin is broken: " + updateJson);
        e.printStackTrace();
        continue;
      }

      System.out.print("testing " + updateJson + "... ");

      VerificationContextImpl ctx = new VerificationContextImpl(options);
      Verifiers.processAllVerifiers(plugin, ctx);

      results.put(updateJson, ctx.getProblems());

      if (ctx.getProblems().isEmpty()) {
        System.out.println("ok");
      }
      else {
        System.out.println(" has " + ctx.getProblems().count() + " errors");

        ctx.getProblems().printProblems(System.out, "    ");
      }
    }

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + "s)");

    if (checkExcludedBuilds) {

      if (dumpBrokenPluginsFile != null) {
        System.out.println("Dumping list of broken plugins to " + dumpBrokenPluginsFile);

        List<UpdateJson> res = new ArrayList<UpdateJson>();

        for (UpdateJson updateJson : updateIds) {
          if (!results.get(updateJson).isEmpty()) {
            res.add(updateJson);
          }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(res, updateListType);
        FileUtils.writeStringToFile(new File(dumpBrokenPluginsFile), json);
      }

      if (reportFile != null) {
        System.out.println("Saving report to " + new File(reportFile).getAbsolutePath());
        CheckIdeHtmlReportBuilder.build(new File(reportFile), ideVersion, results);
      }
    }

    removeExcludedPlugins(results.keySet(), commandLine);
    for (ProblemSet problems : results.values()) {
      if (!problems.isEmpty()) {
        System.exit(2);
      }
    }
  }
}

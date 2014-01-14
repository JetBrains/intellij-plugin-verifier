package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.util.Configuration;
import com.jetbrains.pluginverifier.util.DownloadUtils;
import com.jetbrains.pluginverifier.util.UpdateJson;
import com.jetbrains.pluginverifier.util.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
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

  private List<UpdateJson> getUpdateIds(Idea ide, @NotNull CommandLine commandLine) throws IOException {
    String build = commandLine.getOptionValue("iv");
    if (build == null || build.isEmpty()) {
      build = Files.toString(new File(ide.getIdeaDir(), "build.txt"), Charset.defaultCharset()).trim();
      if (build.length() == 0) {
        throw Util.fail("failed to read IDE version (" + ide.getIdeaDir() + "/build.txt)");
      }
    }

    List<UpdateJson> res;

    List<String> pluginIds = extractPluginList(commandLine);

    if (pluginIds.isEmpty()) {
      throw Util.fail("You have to specify list of plugins to check using -pl option");
    }

    System.out.println("Loading compatible plugins list... ");

    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/originalCompatibleUpdatesByPluginIds/?build=" + build + "&pluginIds=" + Joiner
      .on("&pluginIds=").join(pluginIds));
    String text = IOUtils.toString(url);

    res = new Gson().fromJson(text, updateListType);

    String ebf = commandLine.getOptionValue("ebf");
    if (ebf != null) {
      String excludedBuildListStr = Files.toString(new File(ebf), Charset.defaultCharset());
      List<UpdateJson> excludedBuildList = new Gson().fromJson(excludedBuildListStr, updateListType);
      for (UpdateJson excludedBuild : excludedBuildList) {
        for (Iterator<UpdateJson> itr = res.iterator(); itr.hasNext(); ) {
          UpdateJson u = itr.next();
          if (excludedBuild.equalsByIdOrVersion(u)) {
            itr.remove();
          }
        }
      }

    }

    return res;
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

    List<UpdateJson> updateIds = getUpdateIds(ide, commandLine);

    Map<Integer, Collection<Problem>> results = new TreeMap<Integer, Collection<Problem>>();

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

      System.out.print("testing plugin " + plugin.getId() + " " + updateJson + "... ");

      VerificationContextImpl ctx = new VerificationContextImpl(options);
      Verifiers.processAllVerifiers(plugin, ctx);

      if (ctx.getProblems().isEmpty()) {
        System.out.println("ok");
      }
      else {
        System.out.println("error");

        results.put(updateJson.getUpdateId(), ctx.getProblems());

        System.err.println("Plugin " + updateJson + " has " + ctx.getProblems().size() + " errors");
        for (Problem problem : ctx.getProblems()) {
          System.err.print("    ");
          System.err.println(problem.getDescription());
        }
      }
    }

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + "s)");
    if (!results.isEmpty()) {
      System.exit(2);
    }
  }
}

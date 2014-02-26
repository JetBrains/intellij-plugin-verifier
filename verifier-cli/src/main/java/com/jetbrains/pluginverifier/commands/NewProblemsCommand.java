package com.jetbrains.pluginverifier.commands;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Sergey Evdokimov
 */
public class NewProblemsCommand extends VerifierCommand {

  public NewProblemsCommand() {
    super("new-problems");
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw Util.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar new-problems ~/EAPs/idea-IU-133.439\"");
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

    List<String> resultsList = PRUtil.loadAvailableCheckResultsList();

    Pair<String, Integer> checkedIde = parseBuildNumber(ideVersion);

    TreeMap<Integer, String> previousBuilds = new TreeMap<Integer, String>();

    for (String build : resultsList) {
      Pair<String, Integer> pair = parseBuildNumber(build);

      if (checkedIde.first.equals(pair.first) && checkedIde.second > pair.second) {
        previousBuilds.put(pair.second, build);
      }
    }

    if (previousBuilds.isEmpty()) {
      System.out.println("Plugin repository does not contain check result to compare.");
    }

    List<UpdateInfo> updates = PRUtil.getAllCompatibleUpdates(ideVersion);

    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    for (UpdateInfo updateInfo : updates) {
      File update;
      try {
        update = DownloadUtils.getUpdate(updateInfo.getUpdateId());
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
        System.out.println("Plugin is broken: " + updateInfo);
        e.printStackTrace();
        continue;
      }

      System.out.print("testing " + updateInfo + "... ");

      VerificationContextImpl ctx = new VerificationContextImpl(options);
      Verifiers.processAllVerifiers(plugin, ctx);

      for (Problem problem : ctx.getProblems().getAllProblems()) {
        problems.put(problem, updateInfo);
      }

      if (ctx.getProblems().isEmpty()) {
        System.out.println("ok");
      }
      else {
        System.out.println(" has " + ctx.getProblems().count() + " problems");

        ctx.getProblems().printProblems(System.out, "    ");
      }
    }



    return 0;
  }

  private static Pair<String, Integer> parseBuildNumber(String buildNumber) {
    int idx = buildNumber.lastIndexOf('-');

    return Pair.create(buildNumber.substring(0, idx), Integer.parseInt(buildNumber.substring(idx + 1)));
  }
}

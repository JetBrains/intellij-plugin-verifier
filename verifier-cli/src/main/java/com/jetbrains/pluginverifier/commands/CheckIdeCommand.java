package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.JDK;
import com.intellij.structure.pool.ClassPool;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CheckIdeCommand extends VerifierCommand {

  public CheckIdeCommand() {
    super("check-ide");
  }

  private static Pair<List<String>, List<String>> extractPluginList(@NotNull CommandLine commandLine) {
    List<String> pluginsCheckAllBuilds = new ArrayList<String>();
    List<String> pluginsCheckLastBuilds = new ArrayList<String>();

    String[] pluginIdsCheckAllBuilds = commandLine.getOptionValues('p');
    if (pluginIdsCheckAllBuilds != null) {
      pluginsCheckAllBuilds.addAll(Arrays.asList(pluginIdsCheckAllBuilds));
    }

    String[] pluginIdsCheckLastBuilds = commandLine.getOptionValues('u');
    if (pluginIdsCheckLastBuilds != null) {
      pluginsCheckLastBuilds.addAll(Arrays.asList(pluginIdsCheckLastBuilds));
    }

    String pluginsFile = commandLine.getOptionValue("pluginsFile");
    if (pluginsFile != null) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(pluginsFile));
        String s;
        while ((s = reader.readLine()) != null) {
          s = s.trim();
          if (s.isEmpty() || s.startsWith("//")) continue;

          boolean checkAllBuilds = true;
          if (s.endsWith("$")) {
            s = s.substring(0, s.length() - 1).trim();
            checkAllBuilds = false;
          }
          if (s.startsWith("$")) {
            s = s.substring(1).trim();
            checkAllBuilds = false;
          }

          if (checkAllBuilds) {
            pluginsCheckAllBuilds.add(s);
          }
          else {
            if (s.isEmpty()) continue;

            pluginsCheckLastBuilds.add(s);
          }
        }
      }
      catch (IOException e) {
        throw com.intellij.structure.utils.Util.fail("Failed to read plugins file " + pluginsFile + ": " + e.getLocalizedMessage());
      }
    }

    System.out.println("List of plugins to check: " + Joiner.on(", ").join(Iterables.concat(pluginsCheckAllBuilds, pluginsCheckLastBuilds)));

    return Pair.create(pluginsCheckAllBuilds, pluginsCheckLastBuilds);
  }

  private static Predicate<UpdateInfo> getExcludedPluginsPredicate(@NotNull CommandLine commandLine) throws IOException {
    String epf = commandLine.getOptionValue("epf");
    if (epf == null) {
      return Predicates.alwaysTrue();
    }

    BufferedReader br = new BufferedReader(new FileReader(new File(epf)));
    try {
      final SetMultimap<String, String> m = HashMultimap.create();

      String s;
      while ((s = br.readLine()) != null) {
        s = s.trim();
        if (s.startsWith("//")) continue;

        List<String> tokens = ParametersListUtil.parse(s);
        if (tokens.isEmpty()) continue;

        if (tokens.size() == 1) {
          throw new IOException(epf + " is broken. The line contains plugin name, but does not contains version: " + s);
        }

        String pluginId = tokens.get(0);

        m.putAll(pluginId, tokens.subList(1, tokens.size()));
      }

      return new Predicate<UpdateInfo>() {
        @Override
        public boolean apply(UpdateInfo json) {
          return !m.containsEntry(json.getPluginId(), json.getVersion());
        }
      };
    }
    finally {
      br.close();
    }
  }

  private static void printTeamCityProblems(TeamCityLog log, Map<UpdateInfo, ProblemSet> results, Predicate<UpdateInfo> updateFilter) {
    if (log == TeamCityLog.NULL_LOG) return;

    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      if (!updateFilter.apply(entry.getKey())) continue;

      for (Problem problem : entry.getValue().getAllProblems()) {
        problems.put(problem, entry.getKey());
      }
    }

    TeamCityUtil.printTeamCityProblems(log, problems);
  }

  private static void dumbBrokenPluginsList(@NotNull String dumpBrokenPluginsFile, Collection<UpdateInfo> brokenUpdates)
      throws IOException {
    Multimap<String, String> m = TreeMultimap.create(Ordering.natural(), Ordering.natural().reverse());

    for (UpdateInfo update : brokenUpdates) {
      m.put(update.getPluginId(), update.getVersion());
    }

    PrintWriter out = new PrintWriter(dumpBrokenPluginsFile);
    try {
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n");

      for (Map.Entry<String, Collection<String>> entry : m.asMap().entrySet()) {

        out.print(ParametersListUtil.join(Collections.singletonList(entry.getKey())));
        out.print("    ");
        out.println(ParametersListUtil.join(new ArrayList<String>(entry.getValue())));
      }
    } finally {
      out.close();
    }
  }

  private static void saveResultsToXml(@NotNull String xmlFile, @NotNull String ideVersion, @NotNull Map<UpdateInfo, ProblemSet> results)
      throws IOException {
    Map<UpdateInfo, Collection<Problem>> problems = new LinkedHashMap<UpdateInfo, Collection<Problem>>();

    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      problems.put(entry.getKey(), entry.getValue().getAllProblems());
    }

    ProblemUtils.saveProblems(new File(xmlFile), ideVersion, problems);
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw com.intellij.structure.utils.Util.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"");
    }

    File ideToCheck = new File(freeArgs.get(0));
    if (!ideToCheck.isDirectory()) {
      throw com.intellij.structure.utils.Util.fail("IDE home is not a directory: " + ideToCheck);
    }

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    ClassPool externalClassPath = getExternalClassPath(commandLine);

    Idea ide = new Idea(ideToCheck, jdk, externalClassPath);
    updateIdeVersionFromCmd(ide, commandLine);

    Pair<List<String>, List<String>> pluginIds = extractPluginList(commandLine);
    List<String> pluginsCheckAllBuilds = pluginIds.first;
    List<String> pluginsCheckLastBuilds = pluginIds.second;

    Collection<UpdateInfo> updates;
    if (pluginsCheckAllBuilds.isEmpty() && pluginsCheckLastBuilds.isEmpty()) {
      updates = RepositoryManager.getInstance().getAllCompatibleUpdates(ide.getVersion());
    }
    else {
      updates = new ArrayList<UpdateInfo>();

      if (pluginsCheckAllBuilds.size() > 0) {
        updates.addAll(RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ide.getVersion(), pluginsCheckAllBuilds));
      }

      if (pluginsCheckLastBuilds.size() > 0) {
        Map<String, UpdateInfo> lastBuilds = new HashMap<String, UpdateInfo>();

        for (UpdateInfo info : RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ide.getVersion(), pluginsCheckLastBuilds)) {
          UpdateInfo existsBuild = lastBuilds.get(info.getPluginId());
          if (existsBuild == null || existsBuild.getUpdateId() < info.getUpdateId()) {
            lastBuilds.put(info.getPluginId(), info);
          }
        }

        updates.addAll(lastBuilds.values());
      }
    }

    String dumpBrokenPluginsFile = commandLine.getOptionValue("d");
    String reportFile = commandLine.getOptionValue("report");

    boolean checkExcludedBuilds = dumpBrokenPluginsFile != null || reportFile != null;

    Predicate<UpdateInfo> updateFilter = getExcludedPluginsPredicate(commandLine);

    if (!checkExcludedBuilds) {
      updates = Collections2.filter(updates, updateFilter);
    }

    final Map<UpdateInfo, ProblemSet> results = new HashMap<UpdateInfo, ProblemSet>();

    long time = System.currentTimeMillis();

    for (UpdateInfo updateJson : updates) {
      TeamCityLog.Block block = tc.blockOpen(updateJson.toString());

      try {
        File update;
        try {
          update = RepositoryManager.getInstance().getOrLoadUpdate(updateJson);
        }
        catch (IOException e) {
          System.out.println("failed to download: " + e.getMessage());
          continue;
        }

        IdeaPlugin plugin;
        try {
          plugin = IdeaPlugin.createFromZip(update);
        }
        catch (Exception e) {
          System.out.println("Plugin is broken: " + updateJson);
          tc.messageWarn("Failed to read plugin: " + e.getLocalizedMessage());
          e.printStackTrace();
          continue;
        }

        System.out.print("testing " + updateJson + "... ");

        VerificationContextImpl ctx = new VerificationContextImpl(options, ide);
        Verifiers.processAllVerifiers(plugin, ctx);

        results.put(updateJson, ctx.getProblems());

        if (ctx.getProblems().isEmpty()) {
          System.out.println("ok");
          tc.message(updateJson + " ok");
        }
        else {
          System.out.println(" has " + ctx.getProblems().count() + " problems");

          if (updateFilter.apply(updateJson)) {
            tc.messageError(updateJson + " has problems");
          }
          else {
            tc.message(updateJson + " has problems, but is excluded in brokenPlugins.json");
          }

          ctx.getProblems().printProblems(System.out, "    ");
        }
      }
      finally {
        block.close();
      }
    }

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + "s)");

    printTeamCityProblems(tc, results, updateFilter);

    if (checkExcludedBuilds) {

      if (dumpBrokenPluginsFile != null) {
        System.out.println("Dumping list of broken plugins to " + dumpBrokenPluginsFile);

        dumbBrokenPluginsList(dumpBrokenPluginsFile, Collections2.filter(updates, new Predicate<UpdateInfo>() {
          @Override
          public boolean apply(UpdateInfo update) {
            return results.get(update) != null && !results.get(update).isEmpty();
          }
        }));
      }

      if (reportFile != null) {
        System.out.println("Saving report to " + new File(reportFile).getAbsolutePath());
        CheckIdeHtmlReportBuilder.build(new File(reportFile), ide.getVersion(), Util.concat(pluginIds.first, pluginIds.second), updateFilter, results);
      }
    }

    if (commandLine.hasOption("xr")) {
      saveResultsToXml(commandLine.getOptionValue("xr"), ide.getVersion(), results);
    }

    Set<Problem> allProblems = new HashSet<Problem>();

    for (ProblemSet problemSet : Maps.filterKeys(results, updateFilter).values()) {
      allProblems.addAll(problemSet.getAllProblems());
    }

    if (allProblems.size() > 0) {
      tc.buildStatus(allProblems.size() + (allProblems.size() == 1 ? " problem" : " problems") );
      System.out.printf("IDE has %d problems", allProblems.size());
      return 2;
    }

    return 0;
  }
}

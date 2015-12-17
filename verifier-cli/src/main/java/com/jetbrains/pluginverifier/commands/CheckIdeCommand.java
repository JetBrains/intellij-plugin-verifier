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
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.VerificationProblem;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CheckIdeCommand extends VerifierCommand {

  /**
   * List of IntelliJ plugins which has defined module inside
   * (e.g. plugin "org.jetbrains.plugins.ruby" has a module "com.intellij.modules.ruby" inside)
   */
  private static final ImmutableList<String> INTELLIJ_MODULES_PLUGIN_IDS =
      ImmutableList.of("org.jetbrains.plugins.ruby", "com.jetbrains.php", "org.jetbrains.android", "Pythonid");

  public CheckIdeCommand() {
    super("check-ide");
  }


  @NotNull
  private static Pair<List<String>, List<String>> extractPluginToCheckList(@NotNull CommandLine commandLine) {
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
          } else {
            if (s.isEmpty()) continue;

            pluginsCheckLastBuilds.add(s);
          }
        }
      } catch (IOException e) {
        throw FailUtil.fail("Failed to read plugins file " + pluginsFile + ": " + e.getLocalizedMessage(), e);
      }
    }

    System.out.println("List of plugins to check: " + Joiner.on(", ").join(Iterables.concat(pluginsCheckAllBuilds, pluginsCheckLastBuilds)));

    return Pair.create(pluginsCheckAllBuilds, pluginsCheckLastBuilds);
  }

  @NotNull
  private static Predicate<UpdateInfo> getExcludedPluginsPredicate(@NotNull CommandLine commandLine) throws IOException {
    String epf = commandLine.getOptionValue("epf");
    if (epf == null) {
      //no predicate specified
      return Predicates.alwaysTrue();
    }

    //file containing list of broken plugins (e.g. IDEA-*/lib/resources.jar!/brokenPlugins.txt)
    BufferedReader br = new BufferedReader(new FileReader(new File(epf)));
    try {
      final SetMultimap<String, String> m = HashMultimap.create();

      String s;
      while ((s = br.readLine()) != null) {
        s = s.trim();
        if (s.startsWith("//")) continue; //it is a comment

        List<String> tokens = ParametersListUtil.parse(s);
        if (tokens.isEmpty()) continue;

        if (tokens.size() == 1) {
          throw new IOException(epf + " is broken. The line contains plugin name, but does not contain version: " + s);
        }

        String pluginId = tokens.get(0);

        m.putAll(pluginId, tokens.subList(1, tokens.size())); //"plugin id" -> [all its builds]
      }

      return new Predicate<UpdateInfo>() {
        @Override
        public boolean apply(UpdateInfo json) {
          return !m.containsEntry(json.getPluginId(), json.getVersion());
        }
      };
    } finally {
      IOUtils.closeQuietly(br);
    }
  }


  private static void dumbBrokenPluginsList(@NotNull String dumpBrokenPluginsFile, Collection<UpdateInfo> brokenUpdates)
      throws IOException {
    //pluginId -> [list of its builds in DESC order]
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

  private static void saveResultsToXml(@NotNull String xmlFile,
                                       @NotNull String ideVersion,
                                       @NotNull Map<UpdateInfo, ProblemSet> results) throws IOException {
    Map<UpdateInfo, Collection<Problem>> problems = new LinkedHashMap<UpdateInfo, Collection<Problem>>();

    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      problems.put(entry.getKey(), entry.getValue().getAllProblems());
    }

    ProblemUtils.saveProblems(new File(xmlFile), ideVersion, problems);
  }

  /**
   * Prints Build Problems in the Overview page or as tests
   */
  private static void printTeamCityProblems(@NotNull TeamCityLog log,
                                            @NotNull Map<UpdateInfo, ProblemSet> results,
                                            @NotNull Predicate<UpdateInfo> updateFilter,
                                            @NotNull TeamCityUtil.ReportGrouping reportGrouping) {
    if (log == TeamCityLog.NULL_LOG) return;

    //list of problems without their exact problem location (only affected plugin)
    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    //fill problems map
    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      if (!updateFilter.apply(entry.getKey())) {
        continue; //this is excluded plugin
      }

      for (Problem problem : entry.getValue().getAllProblems()) {
        problems.put(problem, entry.getKey());
      }
    }

    TeamCityUtil.printReport(log, problems, reportGrouping);
  }

  /**
   * Checks if for all the specified plugins to be checked there is
   * a build compatible with a specified IDE in the Plugin Repository
   *
   * @return number of "missing update" and "broken plugin" problems
   */
  private static int printMissingAndIncorrectPlugins(@NotNull TeamCityLog tc,
                                                     @NotNull String version,
                                                     @NotNull List<String> toBeCheckedPluginIds,
                                                     @NotNull List<UpdateInfo> compatibleUpdates,
                                                     @NotNull List<Trinity<UpdateInfo, String, ? extends Exception>> incorrectPlugins) {
    if (tc == TeamCityLog.NULL_LOG) return 0;

    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    //fill verification problems
    for (Trinity<UpdateInfo, String, ? extends Exception> trinity : incorrectPlugins) {
      problems.put(new VerificationProblem(trinity.getSecond(), Util.getStackTrace(trinity.getThird())), trinity.getFirst());
    }

    //fill missing plugin problems
    Set<String> missingPluginIds = new HashSet<String>();
    for (String pluginId : new HashSet<String>(toBeCheckedPluginIds)) {
      boolean hasCompatibleUpdate = false;
      for (UpdateInfo update : compatibleUpdates) {
        if (StringUtil.equals(pluginId, update.getPluginId())) {
          hasCompatibleUpdate = true;
          break;
        }
      }
      if (!hasCompatibleUpdate) {
        missingPluginIds.add(pluginId);
      }
    }

    for (String missingPluginId : missingPluginIds) {
      problems.put(new NoCompatibleUpdatesProblem(missingPluginId, version), new UpdateInfo(missingPluginId, missingPluginId, "no compatible update"));
    }


    TeamCityUtil.printReport(tc, problems, TeamCityUtil.ReportGrouping.PLUGIN);
    return problems.size();
  }

  @NotNull
  private Collection<UpdateInfo> prepareUpdates(@NotNull Collection<UpdateInfo> updates) {
    Collection<UpdateInfo> important = new ArrayList<UpdateInfo>();
    Collection<UpdateInfo> notImportant = new ArrayList<UpdateInfo>();
    for (UpdateInfo update : updates) {
      String pluginId = update.getPluginId();
      if (INTELLIJ_MODULES_PLUGIN_IDS.contains(pluginId)) {
        important.add(update);
      } else {
        notImportant.add(update);
      }
    }
    important.addAll(notImportant);
    return important;
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw FailUtil.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"");
    }

    File ideToCheck = new File(freeArgs.get(0));
    if (!ideToCheck.isDirectory()) {
      throw FailUtil.fail("IDE home is not a directory: " + ideToCheck);
    }

    TeamCityUtil.ReportGrouping reportGrouping = TeamCityUtil.ReportGrouping.parseGrouping(commandLine);

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    ClassPool externalClassPath = getExternalClassPath(commandLine);

    Idea ide = new Idea(ideToCheck, jdk, externalClassPath);
    updateIdeVersionFromCmd(ide, commandLine);

    Pair<List<String>, List<String>> pluginsIds = extractPluginToCheckList(commandLine);
    List<String> pluginsCheckAllBuilds = pluginsIds.first;
    List<String> pluginsCheckLastBuilds = pluginsIds.second;

    Collection<UpdateInfo> updates;
    if (pluginsCheckAllBuilds.isEmpty() && pluginsCheckLastBuilds.isEmpty()) {
      updates = RepositoryManager.getInstance().getAllCompatibleUpdates(ide.getVersion());
    } else {
      updates = new ArrayList<UpdateInfo>();

      if (pluginsCheckAllBuilds.size() > 0) {
        updates.addAll(RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ide.getVersion(), pluginsCheckAllBuilds));
      }

      if (pluginsCheckLastBuilds.size() > 0) {
        Map<String, UpdateInfo> lastBuilds = new HashMap<String, UpdateInfo>();

        for (UpdateInfo info : RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ide.getVersion(), pluginsCheckLastBuilds)) {
          UpdateInfo existsBuild = lastBuilds.get(info.getPluginId());

          //choose last build
          if (existsBuild == null || existsBuild.getUpdateId() < info.getUpdateId()) {
            lastBuilds.put(info.getPluginId(), info);
          }
        }

        updates.addAll(lastBuilds.values());
      }
    }

    //preserve initial lists of plugins
    List<String> toBeCheckedPluginIds = Util.concat(pluginsCheckAllBuilds, pluginsCheckLastBuilds);
    List<UpdateInfo> compatibleToBeCheckedUpdates = new ArrayList<UpdateInfo>(updates);

    String dumpBrokenPluginsFile = commandLine.getOptionValue("d");
    String reportFile = commandLine.getOptionValue("report");

    //whether to check excluded builds or not
    boolean checkExcludedBuilds = dumpBrokenPluginsFile != null || reportFile != null;

    Predicate<UpdateInfo> updateFilter = getExcludedPluginsPredicate(commandLine);

    if (!checkExcludedBuilds) {
      //drop out excluded plugins and don't check them
      updates = Collections2.filter(updates, updateFilter);
    }

    final Map<UpdateInfo, ProblemSet> results = new HashMap<UpdateInfo, ProblemSet>();

    long time = System.currentTimeMillis();

    //move important IntelliJ plugins to the beginning of check-list
    //(those plugins which contain defined IntelliJ module inside)
    updates = prepareUpdates(updates);

    Set<UpdateInfo> importantUpdates = prepareImportantUpdates(updates);

    //list of plugins which were not checked due to some error (first = plugin; second = error message; third = caused exception)
    final List<Trinity<UpdateInfo, String, ? extends Exception>> incorrectPlugins = new ArrayList<Trinity<UpdateInfo, String, ? extends Exception>>();

    //-----------------------------VERIFICATION---------------------------------------

    int updatesProceed = 0;

    for (UpdateInfo updateJson : updates) {
      TeamCityLog.Block block = tc.blockOpen(updateJson.toString());

      try {
        File updateFile = RepositoryManager.getInstance().getOrLoadUpdate(updateJson);

        IdeaPlugin plugin = IdeaPlugin.createFromZip(updateFile);

        System.out.println(String.format("Verifying plugin %s (#%d out of %d)...", updateJson, (++updatesProceed), updates.size()));

        VerificationContextImpl ctx = new VerificationContextImpl(options, ide);
        Verifiers.processAllVerifiers(plugin, ctx);

        results.put(updateJson, ctx.getProblems());

        if (ctx.getProblems().isEmpty()) {
          System.out.println("plugin " + updateJson + " is OK");
          tc.message(updateJson + " is OK");
        } else {
          int count = ctx.getProblems().count();
          System.out.println("has " + count + " problems");

          if (updateFilter.apply(updateJson)) {
            tc.messageError(updateJson + " has " + count + " problems");
          } else {
            tc.message(updateJson + " has problems, but is excluded in brokenPlugins.json");
          }

          ctx.getProblems().printProblems(System.out, "    ");
        }


        if (importantUpdates.contains(updateJson)) {
          //add a plugin with defined IntelliJ module to IDEA
          //it gives us a chance to refer to such plugins by their defined module-name
          ide.addCustomPlugin(plugin);
        }

      } catch (Exception e) {
        final String message;
        if (e instanceof IOException) {
          message = "Failed to read/download plugin " + updateJson + " because " + e.getLocalizedMessage();
        } else {
          message = "Failed to verify plugin " + updateJson + " because " + e.getLocalizedMessage();
        }
        incorrectPlugins.add(Trinity.create(updateJson, message, e));

        System.err.println(message);
        e.printStackTrace();
        tc.messageError(message, Util.getStackTrace(e));
      } finally {
        block.close();
      }

    }

    //-----------------------------PRINT RESULTS----------------------------------------

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + " seconds)");

    //Save results to XML if necessary
    if (commandLine.hasOption("xr")) {
      saveResultsToXml(commandLine.getOptionValue("xr"), ide.getVersion(), results);
    }


    //Is it a full check? (i.e. all the specified plugin builds were checked) => save report.html
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
        File file = new File(reportFile);
        System.out.println("Saving report to " + file.getAbsolutePath());

        CheckIdeHtmlReportBuilder.build(file, ide.getVersion(), toBeCheckedPluginIds, updateFilter, results);
      }
    }

    printTeamCityProblems(tc, results, updateFilter, reportGrouping);

    int totalProblemsCnt = printMissingAndIncorrectPlugins(tc, ide.getVersion(), toBeCheckedPluginIds, compatibleToBeCheckedUpdates, incorrectPlugins);

    Set<Problem> allProblems = new HashSet<Problem>();

    for (ProblemSet problemSet : Maps.filterKeys(results, updateFilter).values()) {
      allProblems.addAll(problemSet.getAllProblems());
    }


    totalProblemsCnt += allProblems.size();

    //-----------------------------PRINT CHECK STATUS----------------------------------------

    if (totalProblemsCnt > 0) {
      tc.buildStatusFailure("IDE " + ide.getVersion() + " has " + totalProblemsCnt + StringUtil.pluralize(" problem", totalProblemsCnt));
      System.out.printf("IDE %s has %d problems", ide.getVersion(), totalProblemsCnt);
      return 2;
    } else {
      tc.buildStatusSuccess("IDE " + ide.getVersion() + " has no broken API problems!");
    }

    return 0;
  }

  /**
   * Drops out non-last builds of IntelliJ plugins
   * IntelliJ plugin is a plugin which defines intellij-module in its plugin.xml
   */
  @NotNull
  private Set<UpdateInfo> prepareImportantUpdates(@NotNull Collection<UpdateInfo> updates) {
    Map<String, Integer> lastBuilds = new HashMap<String, Integer>();
    for (UpdateInfo update : updates) {
      String pluginId = update.getPluginId();
      if (INTELLIJ_MODULES_PLUGIN_IDS.contains(pluginId)) {
        Integer existingBuild = lastBuilds.get(pluginId);
        Integer curBuild = update.getUpdateId();

        if (existingBuild == null || existingBuild < curBuild) {
          lastBuilds.put(pluginId, curBuild);
        }
      }
    }

    Set<UpdateInfo> result = new HashSet<UpdateInfo>();
    for (UpdateInfo update : updates) {
      String pluginId = update.getPluginId();
      Integer updateId = update.getUpdateId();

      if (lastBuilds.containsKey(pluginId) && lastBuilds.get(pluginId).equals(updateId)) {
        result.add(update);
      }
    }

    return result;
  }
}

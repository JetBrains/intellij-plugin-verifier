package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.intellij.structure.domain.*;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.BrokenPluginProblem;
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
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CheckIdeCommand extends VerifierCommand {

  /**
   * List of IntelliJ plugins which has defined module inside (e.g. plugin "org.jetbrains.plugins.ruby" has a module
   * "com.intellij.modules.ruby" inside)
   */
  private static final ImmutableList<String> INTELLIJ_MODULES_PLUGIN_IDS =
      ImmutableList.of("org.jetbrains.plugins.ruby", "com.jetbrains.php", "org.jetbrains.android", "Pythonid", "PythonCore");
  private TeamCityUtil.ReportGrouping myGrouping;
  private TeamCityLog myTc;
  private Jdk myJdk;
  private PluginVerifierOptions myVerifierOptions;
  private Resolver myExternalClassPath;
  private Ide myIde;
  private Collection<UpdateInfo> myUpdatesToCheck;
  private List<String> myCheckedIds;
  private String myDumpBrokenPluginsFile;
  private String myReportFile;
  private boolean myCheckExcludedBuilds;
  private Predicate<UpdateInfo> myExcludedUpdatesFilter;
  private Map<UpdateInfo, ProblemSet> myResults;
  private Set<UpdateInfo> myImportantUpdates;
  private Multimap<String, String> myExcludedPlugins;
  private Multimap<Problem, UpdateInfo> myBrokenPluginsProblems = ArrayListMultimap.create();
  private Multimap<Problem, UpdateInfo> myMissingUpdatesProblems = ArrayListMultimap.create();

  public CheckIdeCommand() {
    super("check-ide");
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
   * Checks if for all the specified plugins to be checked there is a build compatible with a specified IDE in the
   * Plugin Repository
   *
   * @return number of "missing update" and "broken plugin" problems
   */
  private int printMissingAndIncorrectPlugins() {
    if (myTc == TeamCityLog.NULL_LOG) return 0;
    TeamCityUtil.printReport(myTc, myBrokenPluginsProblems, TeamCityUtil.ReportGrouping.PLUGIN);
    TeamCityUtil.printReport(myTc, myMissingUpdatesProblems, TeamCityUtil.ReportGrouping.PLUGIN);
    return myBrokenPluginsProblems.size() + myMissingUpdatesProblems.size();
  }

  private void fillMissingPluginProblems() {
    for (String pluginId : new HashSet<String>(myCheckedIds)) { //plugins from checkedPlugins.txt. for them check that compatible version is present
      boolean hasCompatibleUpdate = false;
      for (UpdateInfo update : myUpdatesToCheck) {
        if (myExcludedUpdatesFilter.apply(update)) {
          if (StringUtil.equals(pluginId, update.getPluginId())) {
            hasCompatibleUpdate = true;
            break;
          }
        }
      }

      if (!hasCompatibleUpdate) {
        //try to find this update in the compatible updates of IDEA Community

        UpdateInfo missingUpdate = new UpdateInfo(pluginId, pluginId, "no compatible update");

        UpdateInfo buildForCommunity = getUpdateCompatibleWithCommunityEdition(pluginId);
        if (buildForCommunity != null) {
          String details = "\nNote: there is an update (#" + buildForCommunity.getUpdateId() + ") compatible with IDEA Community Edition, " +
              "\nbut the Plugin repository does not offer to install it if you run the IDEA Ultimate" +
              "\nThis update has been checked on API compatibility problems too. The present problems (if any) are about it.";
          myMissingUpdatesProblems.put(new NoCompatibleUpdatesProblem(pluginId, myIde.getVersion().asString(), details), missingUpdate);

          //check the community build instead
          myUpdatesToCheck.add(buildForCommunity);

        } else {
          myMissingUpdatesProblems.put(new NoCompatibleUpdatesProblem(pluginId, myIde.getVersion().asString(), ""), missingUpdate);
        }

      }
    }
  }

  @Nullable
  private UpdateInfo getUpdateCompatibleWithCommunityEdition(@NotNull String pluginId) {
    String ideVersion = myIde.getVersion().asString();
    if (ideVersion.startsWith("IU-")) {
      final String communityVersion = "IC-" + StringUtil.trimStart(ideVersion, "IU-");
      try {
        return RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(IdeVersion.createIdeVersion(communityVersion), pluginId);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private void fillArguments(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws IOException, JDOMException {
    if (freeArgs.isEmpty()) {
      throw FailUtil.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"");
    }

    File ideToCheck = new File(freeArgs.get(0));
    if (!ideToCheck.isDirectory()) {
      throw FailUtil.fail("IDE home is not a directory: " + ideToCheck);
    }

    myGrouping = TeamCityUtil.ReportGrouping.parseGrouping(commandLine);

    myTc = TeamCityLog.getInstance(commandLine);

    myJdk = createJdk(commandLine);

    myVerifierOptions = PluginVerifierOptions.parseOpts(commandLine);

    myExternalClassPath = getExternalClassPath(commandLine);

    myIde = createIde(ideToCheck, commandLine);

    Pair<List<String>, List<String>> pluginsIds = Util.extractPluginToCheckList(commandLine);
    List<String> checkAllBuilds = pluginsIds.first;
    List<String> checkLastBuilds = pluginsIds.second;

    if (checkAllBuilds.isEmpty() && checkLastBuilds.isEmpty()) {
      myUpdatesToCheck = RepositoryManager.getInstance().getLastCompatibleUpdates(myIde.getVersion());
    } else {
      myUpdatesToCheck = new ArrayList<UpdateInfo>();

      if (checkAllBuilds.size() > 0) {
        for (String build : checkAllBuilds) {
          myUpdatesToCheck.addAll(RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(myIde.getVersion(), build));
        }
      }

      if (checkLastBuilds.size() > 0) {
        Map<String, UpdateInfo> lastBuilds = new HashMap<String, UpdateInfo>();

        List<UpdateInfo> list = new ArrayList<UpdateInfo>();
        for (String build : checkLastBuilds) {
          list.addAll(RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(myIde.getVersion(), build));
        }

        for (UpdateInfo info : list) {
          UpdateInfo currentBuild = lastBuilds.get(info.getPluginId());

          //choose last build
          if (currentBuild == null || (currentBuild.getUpdateId() != null && info.getUpdateId() != null && currentBuild.getUpdateId() < info.getUpdateId())) {
            lastBuilds.put(info.getPluginId(), info);
          }
        }

        myUpdatesToCheck.addAll(lastBuilds.values());
      }
    }

    //preserve initial lists of plugins
    myCheckedIds = Util.concat(checkAllBuilds, checkLastBuilds);

    myDumpBrokenPluginsFile = commandLine.getOptionValue("d");
    myReportFile = commandLine.getOptionValue("report");

    myExcludedPlugins = Util.getExcludedPlugins(commandLine);

    myExcludedUpdatesFilter = new Predicate<UpdateInfo>() {
      @Override
      public boolean apply(UpdateInfo input) {
        return !myExcludedPlugins.containsEntry(input.getPluginId(), input.getVersion());
      }
    };

    fillMissingPluginProblems();

    //whether to check excluded builds or not
    myCheckExcludedBuilds = myDumpBrokenPluginsFile != null || myReportFile != null;


    if (!myCheckExcludedBuilds || commandLine.hasOption("dce")) {
      //drop out excluded plugins and don't check them
      myUpdatesToCheck = Collections2.filter(myUpdatesToCheck, myExcludedUpdatesFilter);
    }

    dumpUpdatesToCheck(myUpdatesToCheck);

    //move important IntelliJ plugins to the beginning of check-list
    //(those plugins which contain defined IntelliJ module inside)
    myUpdatesToCheck = prepareUpdates(myUpdatesToCheck);

    myImportantUpdates = prepareImportantUpdates(myUpdatesToCheck);

    myResults = new HashMap<UpdateInfo, ProblemSet>();
  }

  @NotNull
  private Ide createIde(@NotNull File ideToCheck, @NotNull CommandLine commandLine) throws IOException {
    return IdeManager.getInstance().createIde(ideToCheck, takeVersionFromCmd(commandLine));
  }

  private void dumpUpdatesToCheck(Collection<UpdateInfo> updatesToCheck) {
    System.out.println("The following updates will be checked: ");
    System.out.println(Joiner.on(", ").join(updatesToCheck));
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
    fillArguments(commandLine, freeArgs);


    //-----------------------------VERIFICATION---------------------------------------
    long time = System.currentTimeMillis();

    int updatesProceed = 0;

    for (UpdateInfo updateJson : myUpdatesToCheck) {
      TeamCityLog.Block block = myTc.blockOpen(updateJson.toString());

      try {
        File updateFile;
        try {
          updateFile = RepositoryManager.getInstance().getPluginFile(updateJson);
        } catch (IOException e) {
          System.err.println("Failed to download plugin " + updateJson);
          e.printStackTrace();
          continue;
        }

        if (updateFile == null) {
          throw FailUtil.fail("Plugin " + updateJson + " is not found in the repository");
        }

        Plugin plugin;
        try {
          plugin = PluginManager.getInstance().createPlugin(updateFile);
        } catch (IncorrectPluginException e) {
          myBrokenPluginsProblems.put(new BrokenPluginProblem(e.getLocalizedMessage()), updateJson);
          System.err.println("Broken plugin " + updateJson);
          e.printStackTrace();
          continue;
        }

        System.out.println(String.format("Verifying plugin %s (#%d out of %d)...", updateJson, (++updatesProceed), myUpdatesToCheck.size()));

        VerificationContextImpl ctx = new VerificationContextImpl(plugin, myIde, myJdk, myExternalClassPath, myVerifierOptions);
        Verifiers.processAllVerifiers(ctx);

        myResults.put(updateJson, ctx.getProblemSet());

        if (ctx.getProblemSet().isEmpty()) {
          System.out.println("plugin " + updateJson + " is OK");
          myTc.message(updateJson + " is OK");
        } else {
          int count = ctx.getProblemSet().count();
          System.out.println("has " + count + " problems");

          if (myExcludedUpdatesFilter.apply(updateJson)) {
            myTc.messageError(updateJson + " has " + count + " problems");
          } else {
            myTc.message(updateJson + " has problems, but is excluded in brokenPlugins.json");
          }

          ctx.getProblemSet().printProblems(System.out, "    ");
        }


        if (myImportantUpdates.contains(updateJson)) {
          //add a plugin with defined IntelliJ module to IDEA
          //it gives us a chance to refer to such plugins by their defined module-name
          myIde = myIde.getExpandedIde(plugin);
        }

      } catch (Exception e) {
        String details = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getName();
        if (myExcludedUpdatesFilter.apply(updateJson)) {
          myBrokenPluginsProblems.put(new VerificationProblem(details, updateJson.toString()), updateJson);
        }

        System.err.println("Failed to verify plugin " + updateJson + " because " + details);
        e.printStackTrace();
        myTc.messageError("Failed to verify plugin " + updateJson + " because " + details, Util.getStackTrace(e));
      } finally {
        block.close();
      }

    }

    //-----------------------------PRINT RESULTS----------------------------------------

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + " seconds)");

    //Save results to XML if necessary
    if (commandLine.hasOption("xr")) {
      saveResultsToXml(commandLine.getOptionValue("xr"), myIde.getVersion().toString(), myResults);
    }


    //if all the builds were checked (including excluded builds from brokenPlugins.txt)
    if (myCheckExcludedBuilds) {

      if (myDumpBrokenPluginsFile != null) {
        System.out.println("Dumping list of broken plugins to " + myDumpBrokenPluginsFile);

        Util.dumbBrokenPluginsList(myDumpBrokenPluginsFile, Collections2.filter(myUpdatesToCheck, new Predicate<UpdateInfo>() {
          @Override
          public boolean apply(UpdateInfo update) {
            return myResults.get(update) != null && !myResults.get(update).isEmpty(); //update to check contains some problem
          }
        }));
      }

      if (myReportFile != null) {
        File file = new File(myReportFile);
        System.out.println("Saving report to " + file.getAbsolutePath());

        CheckIdeHtmlReportBuilder.build(file, myIde.getVersion().asString(), myCheckedIds, myExcludedUpdatesFilter, myResults);
      }
    }

    TeamCityUtil.printTeamCityProblems(myTc, myResults, myExcludedUpdatesFilter, myGrouping);

    int totalProblemsCnt = printMissingAndIncorrectPlugins();

    Set<Problem> allProblems = new HashSet<Problem>();

    for (ProblemSet problemSet : Maps.filterKeys(myResults, myExcludedUpdatesFilter).values()) {
      allProblems.addAll(problemSet.getAllProblems());
    }


    totalProblemsCnt += allProblems.size();

    //-----------------------------PRINT CHECK STATUS----------------------------------------

    if (totalProblemsCnt > 0) {
      myTc.buildStatusFailure("IDE " + myIde.getVersion() + " has " + totalProblemsCnt + StringUtil.pluralize(" problem", totalProblemsCnt));
      System.out.printf("IDE %s has %d problems", myIde.getVersion(), totalProblemsCnt);
      return 2;
    } else {
      myTc.buildStatusSuccess("IDE " + myIde.getVersion() + " has no broken API problems!");
    }

    return 0;
  }

  /**
   * Drops out non-last builds of IntelliJ plugins IntelliJ plugin is a plugin which defines intellij-module in its
   * plugin.xml
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

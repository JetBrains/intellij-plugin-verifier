package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.*;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.PluginCache;
import com.jetbrains.pluginverifier.problems.BrokenPluginProblem;
import com.jetbrains.pluginverifier.problems.NoCompatibleUpdatesProblem;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityVPrinter;
import kotlin.Pair;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

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
  private TeamCityLog myTc;
  private File myJdkDir;
  private VOptions myVerifierOptions;
  private Resolver myExternalClassPath;
  private Ide myIde;
  private Resolver myIdeResolver;
  private Collection<UpdateInfo> myUpdatesToCheck;
  private List<String> myCheckedIds;
  private String myDumpBrokenPluginsFile;
  private String myHtmlReportFile;
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

  /**
   * Checks if for all the specified plugins to be checked there is a build compatible with a specified IDE in the
   * Plugin Repository
   *
   * @return number of "missing update" and "broken plugin" problems
   */
  private int printMissingAndIncorrectPlugins() {
    if (myTc == TeamCityLog.Companion.getNULL_LOG()) return 0;
    TeamCityVPrinter printer = new TeamCityVPrinter(myTc, TeamCityVPrinter.GroupBy.BY_PLUGIN);

    IdeDescriptor.ByVersion ideDescriptor = new IdeDescriptor.ByVersion(myIde.getVersion());

    List<VResult> broken = new ArrayList<>();
    myBrokenPluginsProblems.entries().forEach(it ->
        broken.add(new VResult.BadPlugin(new PluginDescriptor.ByUpdateInfo(it.getValue()), ideDescriptor, it.getKey().getDescription()))
    );
    VResults brokens = new VResults(broken);

    List<VResult> missing = new ArrayList<>();
    myMissingUpdatesProblems.entries().forEach(it ->
        missing.add(new VResult.BadPlugin(new PluginDescriptor.ByUpdateInfo(it.getValue()), ideDescriptor, it.getKey().getDescription()))
    );
    VResults missings = new VResults(missing);

    printer.printResults(brokens);
    printer.printResults(missings);
    return myBrokenPluginsProblems.keySet().size() + myMissingUpdatesProblems.keySet().size();
  }

  private void fillMissingPluginProblems() {
    for (String pluginId : new HashSet<>(myCheckedIds)) { //plugins from checkedPlugins.txt. for them check that compatible version is present
      boolean hasCompatibleUpdate = false;
      for (UpdateInfo update : myUpdatesToCheck) {
        if (myExcludedUpdatesFilter.test(update)) {
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

  private void fillArguments(@NotNull Opts opts, @NotNull List<String> freeArgs) throws IOException, JDOMException {
    if (freeArgs.isEmpty()) {
      throw new RuntimeException("You have to specify IDE to check. For example: \"java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439\"");
    }

    File ideToCheck = new File(freeArgs.get(0));
    if (!ideToCheck.isDirectory()) {
      throw new RuntimeException("IDE home is not a directory: " + ideToCheck);
    }

    myTc = TeamCityLog.Companion.getInstance(opts.getNeedTeamCityLog());

    myJdkDir = Util.INSTANCE.getJdkDir(opts);

    myVerifierOptions = VOptionsUtil.parseOpts(opts);

    myExternalClassPath = Util.INSTANCE.getExternalClassPath(opts);

    myIde = Util.INSTANCE.createIde(ideToCheck, opts);
    myIdeResolver = Resolver.createIdeResolver(myIde);

    Pair<List<String>, List<String>> pluginsIds = Util.INSTANCE.extractPluginToCheckList(opts);
    List<String> checkAllBuilds = pluginsIds.getFirst();
    List<String> checkLastBuilds = pluginsIds.getSecond();

    if (checkAllBuilds.isEmpty() && checkLastBuilds.isEmpty()) {
      myUpdatesToCheck = RepositoryManager.getInstance().getLastCompatibleUpdates(myIde.getVersion());
    } else {
      myUpdatesToCheck = new ArrayList<>();

      if (checkAllBuilds.size() > 0) {
        for (String build : checkAllBuilds) {
          myUpdatesToCheck.addAll(RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(myIde.getVersion(), build));
        }
      }

      if (checkLastBuilds.size() > 0) {
        Map<String, UpdateInfo> lastBuilds = new HashMap<>();

        List<UpdateInfo> list = new ArrayList<>();
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
    myCheckedIds = Util.INSTANCE.concat(checkAllBuilds, checkLastBuilds);

    myDumpBrokenPluginsFile = opts.getDumpBrokenPluginsFile();
    myHtmlReportFile = opts.getHtmlReportFile();

    myExcludedPlugins = Util.INSTANCE.getExcludedPlugins(opts);

    myExcludedUpdatesFilter = input -> !myExcludedPlugins.containsEntry(input.getPluginId(), input.getVersion());

    fillMissingPluginProblems();

    //whether to check excluded builds or not
    myCheckExcludedBuilds = myDumpBrokenPluginsFile != null || myHtmlReportFile != null;


    if (!myCheckExcludedBuilds) {
      //drop out excluded plugins and don't check them
      myUpdatesToCheck = Collections2.filter(myUpdatesToCheck, myExcludedUpdatesFilter::test);
    }

    dumpUpdatesToCheck(myUpdatesToCheck);

    //move important IntelliJ plugins to the beginning of check-list
    //(those plugins which contain defined IntelliJ module inside)
    myUpdatesToCheck = prepareUpdates(myUpdatesToCheck);

    myImportantUpdates = prepareImportantUpdates(myUpdatesToCheck);

    myResults = new HashMap<>();
  }

  private void dumpUpdatesToCheck(Collection<UpdateInfo> updatesToCheck) {
    System.out.println("The following updates will be checked: ");
    System.out.println(Joiner.on(", ").join(updatesToCheck));
  }

  @NotNull
  private Collection<UpdateInfo> prepareUpdates(@NotNull Collection<UpdateInfo> updates) {
    Collection<UpdateInfo> important = new ArrayList<>();
    Collection<UpdateInfo> notImportant = new ArrayList<>();
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
  public int execute(@NotNull Opts opts, @NotNull List<String> freeArgs) throws Exception {
    fillArguments(opts, freeArgs);


    //-----------------------------VERIFICATION---------------------------------------
    long time = System.currentTimeMillis();

    int updatesProceed = 0;

    for (UpdateInfo updateJson : myUpdatesToCheck) {

      try (TeamCityLog.Block block = myTc.blockOpen(updateJson.toString())) {
        File updateFile;
        try {
          updateFile = RepositoryManager.getInstance().getPluginFile(updateJson);
        } catch (IOException e) {
          System.err.println("Failed to download plugin " + updateJson);
          e.printStackTrace();
          continue;
        }

        if (updateFile == null) {
          throw new RuntimeException("Plugin " + updateJson + " is not found in the repository");
        }

        Plugin plugin;
        try {
          plugin = PluginCache.getInstance().createPlugin(updateFile);
        } catch (IncorrectPluginException e) {
          myBrokenPluginsProblems.put(new BrokenPluginProblem(e.getLocalizedMessage()), updateJson);
          System.err.println("Broken plugin " + updateJson);
          e.printStackTrace();
          continue;
        }

        System.out.println(String.format("Verifying plugin %s (#%d out of %d)...", updateJson, (++updatesProceed), myUpdatesToCheck.size()));

        ProblemSet problemSet = Util.INSTANCE.verify(plugin, myIde, myIdeResolver, myJdkDir, myExternalClassPath, myVerifierOptions);

        myResults.put(updateJson, problemSet);

        if (problemSet.isEmpty()) {
          System.out.println("plugin " + updateJson + " is OK");
          myTc.message(updateJson + " is OK");
        } else {
          System.out.println("has " + problemSet.count() + " problems");

          if (myExcludedUpdatesFilter.test(updateJson)) {
            myTc.messageError(updateJson + " has " + problemSet.count() + " problems");
          } else {
            myTc.message(updateJson + " has problems, but is excluded in brokenPlugins.json");
          }

          problemSet.printProblems(System.out, "    ");
        }


        if (myImportantUpdates.contains(updateJson)) {
          //add a plugin with defined IntelliJ module to IDEA
          //it gives us a chance to refer to such plugins by their defined module-name
          myIde = myIde.getExpandedIde(plugin);
        }

      } catch (Exception e) {
        String details = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getName();
        if (myExcludedUpdatesFilter.test(updateJson)) {
          myBrokenPluginsProblems.put(new VerificationProblem(details, updateJson.toString()), updateJson);
        }

        System.err.println("Failed to verify plugin " + updateJson + " because " + details);
        e.printStackTrace();
        myTc.messageError("Failed to verify plugin " + updateJson + " because " + details, Util.INSTANCE.getStackTrace(e));
      }

    }

    tearDown();

    //-----------------------------PRINT RESULTS----------------------------------------

    System.out.println("Verification completed (" + ((System.currentTimeMillis() - time) / 1000) + " seconds)");

    //Save results to XML if necessary
    if (opts.getResultFile() != null) {
      Util.INSTANCE.saveResultsToXml(new File(opts.getResultFile()), myIde.getVersion(), myResults);
    }


    //if all the builds were checked (including excluded builds from brokenPlugins.txt)
    if (myCheckExcludedBuilds) {

      if (myDumpBrokenPluginsFile != null) {
        System.out.println("Dumping list of broken plugins to " + myDumpBrokenPluginsFile);

        Util.INSTANCE.dumbBrokenPluginsList(myDumpBrokenPluginsFile, new ArrayList<>(Collections2.filter(myUpdatesToCheck, update -> {
          return myResults.get(update) != null && !myResults.get(update).isEmpty(); //update to check contains some problem
        })));
      }

      if (myHtmlReportFile != null) {
        File file = new File(myHtmlReportFile);
        System.out.println("Saving report to " + file.getAbsolutePath());

        HtmlReportBuilder.INSTANCE.build(file, myIde.getVersion(), myExcludedUpdatesFilter, myResults);
      }
    }

    myResults = Maps.filterKeys(myResults, x -> myExcludedUpdatesFilter.test(x));

    VResults vResults = TeamCityUtil.INSTANCE.convertOldResultsToNewResults(myResults, myIde.getVersion());
    new TeamCityVPrinter(myTc, TeamCityVPrinter.GroupBy.parse(opts)).printResults(vResults);

    int totalProblemsCnt = printMissingAndIncorrectPlugins();

    Set<Problem> allProblems = new HashSet<>();

    for (ProblemSet problemSet : Maps.filterKeys(myResults, myExcludedUpdatesFilter::test).values()) {
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

  private void tearDown() {
    myIdeResolver.close();
  }

  /**
   * Drops out non-last builds of IntelliJ plugins IntelliJ plugin is a plugin which defines intellij-module in its
   * plugin.xml
   */
  @NotNull
  private Set<UpdateInfo> prepareImportantUpdates(@NotNull Collection<UpdateInfo> updates) {
    Map<String, Integer> lastBuilds = new HashMap<>();
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

    Set<UpdateInfo> result = new HashSet<>();
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

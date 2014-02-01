package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.utils.*;
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

  private static final Type updateListType = new TypeToken<List<Update>>() {}.getType();

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

  private static Predicate<Update> getExcludedPluginsPredicate(@NotNull CommandLine commandLine) throws IOException {
    String epf = commandLine.getOptionValue("epf");
    if (epf == null) {
      return Predicates.alwaysTrue();
    }

    String excludedBuildListStr = Files.toString(new File(epf), Charset.defaultCharset());
    List<Update> excludedBuildList = new Gson().fromJson(excludedBuildListStr, updateListType);

    final Set<Object> excludedUpdate = new HashSet<Object>();

    for (Update update : excludedBuildList) {
      if (update.getUpdateId() != null) {
        excludedUpdate.add(update.getUpdateId());
      }

      if (update.getPluginId() != null && update.getVersion() != null) {
        excludedUpdate.add(Pair.create(update.getPluginId(), update.getVersion()));
      }
    }

    return new Predicate<Update>() {
      @Override
      public boolean apply(Update json) {
        if (excludedUpdate.contains(json.getUpdateId())) {
          return false;
        }

        Pair<String,String> pair = Pair.create(json.getPluginId(), json.getVersion());
        return !excludedUpdate.contains(pair);
      }
    };
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

  private List<Update> getUpdateIds(@NotNull String ideVersion, @NotNull CommandLine commandLine) throws IOException {
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

    TeamCityLog tc = commandLine.hasOption("tc") ? new TeamCityLog(System.out) : TeamCityLog.NULL_LOG;

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    ClassPool externalClassPath = getExternalClassPath(commandLine);

    Idea ide = new Idea(ideToCheck, jdk, externalClassPath);

    String ideVersion = getIdeVersion(ide, commandLine);
    Collection<Update> updateIds = getUpdateIds(ideVersion, commandLine);

    String dumpBrokenPluginsFile = commandLine.getOptionValue("d");
    String reportFile = commandLine.getOptionValue("report");

    boolean checkExcludedBuilds = dumpBrokenPluginsFile != null || reportFile != null;

    Predicate<Update> updateFilter = getExcludedPluginsPredicate(commandLine);

    if (!checkExcludedBuilds) {
      updateIds = Collections2.filter(updateIds, updateFilter);
    }

    Map<Update, ProblemSet> results = new HashMap<Update, ProblemSet>();

    long time = System.currentTimeMillis();

    for (Update updateJson : updateIds) {
      TeamCityLog.Block block = tc.blockOpen(updateJson.toString());

      try {
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
          tc.messageWarn("Failed to read plugin: " + e.getLocalizedMessage());
          e.printStackTrace();
          continue;
        }

        System.out.print("testing " + updateJson + "... ");

        VerificationContextImpl ctx = new VerificationContextImpl(options);
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

        List<Update> res = new ArrayList<Update>();

        for (Update update : updateIds) {
          if (!results.get(update).isEmpty()) {
            res.add(update);
          }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(res, updateListType);
        FileUtils.writeStringToFile(new File(dumpBrokenPluginsFile), json);
      }

      if (reportFile != null) {
        System.out.println("Saving report to " + new File(reportFile).getAbsolutePath());
        CheckIdeHtmlReportBuilder.build(new File(reportFile), ideVersion, updateFilter, results);
      }
    }

    Set<Problem> allProblems = new HashSet<Problem>();

    for (ProblemSet problemSet : Maps.filterKeys(results, updateFilter).values()) {
      allProblems.addAll(problemSet.getAllProblems());
    }

    if (allProblems.size() > 0) {
      tc.buildStatus(allProblems.size() + (allProblems.size() == 1 ? " problem" : " problems") );
      System.out.printf("IDE has %d problems", allProblems.size());
      System.exit(2);
    }
  }

  private static void printTeamCityProblems(TeamCityLog log, Map<Update, ProblemSet> results, Predicate<Update> updateFilter) {
    if (log == TeamCityLog.NULL_LOG) return;

    Multimap<Problem, Update> problems = ArrayListMultimap.create();

    for (Map.Entry<Update, ProblemSet> entry : results.entrySet()) {
      if (!updateFilter.apply(entry.getKey())) continue;

      for (Problem problem : entry.getValue().getAllProblems()) {
        problems.put(problem, entry.getKey());
      }
    }

    if (problems.isEmpty()) return;

    List<Problem> p = new ArrayList<Problem>(problems.keySet());
    Collections.sort(p, new ToStringProblemComparator());

    for (Problem problem : p) {
      List<Update> updates = new ArrayList<Update>(problems.get(problem));
      Collections.sort(updates, new ToStringCachedComparator<Update>());

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }
}

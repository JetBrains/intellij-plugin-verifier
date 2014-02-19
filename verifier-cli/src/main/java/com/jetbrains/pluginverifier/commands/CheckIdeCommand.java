package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
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
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CheckIdeCommand extends VerifierCommand {

  private static final Type updateListType = new TypeToken<List<UpdateInfo>>() {}.getType();

  public CheckIdeCommand() {
    super("check-ide");
  }

  private static List<String> extractPluginList(@NotNull CommandLine commandLine) {
    String[] pluginIds = commandLine.getOptionValues("pl");
    if (pluginIds == null) return Collections.emptyList();

    List<String> res = new ArrayList<String>();

    for (String pluginId : pluginIds) {
      for (StringTokenizer st = new StringTokenizer(pluginId, ","); st.hasMoreTokens(); ) {
        String token = st.nextToken();
        if (!token.isEmpty()) {
          res.add(token);
        }
      }
    }

    return res;
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

  private List<UpdateInfo> getUpdateIds(@NotNull String ideVersion, @NotNull List<String> pluginIds) throws IOException {
    if (!pluginIds.isEmpty()) {
      System.out.println("Loading compatible plugins list... ");

      URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/originalCompatibleUpdatesByPluginIds/?build=" +
                        ideVersion + "&pluginIds=" + Joiner
        .on("&pluginIds=").join(pluginIds));
      String text = IOUtils.toString(url);

      return new Gson().fromJson(text, updateListType);
    }

    System.out.println("Loading compatible plugins list... ");

    URL url = new URL(Configuration.getInstance().getPluginRepositoryUrl() + "/manager/allCompatibleUpdateIds/?build=" + ideVersion);
    String text = IOUtils.toString(url);

    List<UpdateInfo> res = new ArrayList<UpdateInfo>();
    for (StringTokenizer st = new StringTokenizer(text, ", \n"); st.hasMoreTokens(); ) {
      String s = st.nextToken();
      if (!s.isEmpty()) {
        UpdateInfo update = new UpdateInfo();
        update.setUpdateId(Integer.parseInt(s));
        res.add(update);
      }
    }

    Collections.sort(res, Ordering.natural().onResultOf(new Function<UpdateInfo, Comparable>() {
      @Override
      public Comparable apply(UpdateInfo update) {
        return update.getUpdateId();
      }
    }));

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

    TeamCityLog tc = commandLine.hasOption("tc") ? new TeamCityLog(System.out) : TeamCityLog.NULL_LOG;

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    ClassPool externalClassPath = getExternalClassPath(commandLine);

    Idea ide = new Idea(ideToCheck, jdk, externalClassPath);

    String ideVersion = getIdeVersion(ide, commandLine);

    List<String> pluginIds = extractPluginList(commandLine);

    Collection<UpdateInfo> updates = getUpdateIds(ideVersion, pluginIds);

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

        dumbBrokenPluginsList(dumpBrokenPluginsFile, Collections2.filter(updates, new Predicate<UpdateInfo>() {
          @Override
          public boolean apply(UpdateInfo update) {
            return results.get(update) != null && !results.get(update).isEmpty();
          }
        }));
      }

      if (reportFile != null) {
        System.out.println("Saving report to " + new File(reportFile).getAbsolutePath());
        CheckIdeHtmlReportBuilder.build(new File(reportFile), ideVersion, pluginIds, updateFilter, results);
      }
    }

    if (commandLine.hasOption("xr")) {
      saveResultsToXml(commandLine.getOptionValue("xr"), ideVersion, results);
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

  private static void printTeamCityProblems(TeamCityLog log, Map<UpdateInfo, ProblemSet> results, Predicate<UpdateInfo> updateFilter) {
    if (log == TeamCityLog.NULL_LOG) return;

    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      if (!updateFilter.apply(entry.getKey())) continue;

      for (Problem problem : entry.getValue().getAllProblems()) {
        problems.put(problem, entry.getKey());
      }
    }

    if (problems.isEmpty()) return;

    List<Problem> p = ProblemUtils.sort(problems.keySet());

    for (Problem problem : p) {
      List<UpdateInfo> updates = new ArrayList<UpdateInfo>(problems.get(problem));
      Collections.sort(updates, new ToStringCachedComparator<UpdateInfo>());

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
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
    }
    finally {
      out.close();
    }
  }

  private static void saveResultsToXml(@NotNull String xmlFile, String ideVersion, Map<UpdateInfo, ProblemSet> results)
    throws IOException {
    Map<UpdateInfo, Collection<Problem>> problems = new LinkedHashMap<UpdateInfo, Collection<Problem>>();

    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      problems.put(entry.getKey(), entry.getValue().getAllProblems());
    }

    ProblemUtils.saveProblems(new File(xmlFile), ideVersion, problems);
  }
}

package com.jetbrains.pluginverifier.utils;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Util {

  public static final Options CMD_OPTIONS = new Options()
      .addOption("h", "help", false, "Show help")
      .addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)")
      .addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'")
      .addOption("e", "external-classes", true, "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
      .addOption("all", "check-all-plugins-with-ide", false, "Check IDE build with all compatible plugins")
      .addOption("p", "plugin-to-check", true, "A plugin id to check with IDE, plugin verifier will check ALL compatible plugin builds")
      .addOption("u", "update-to-check", true, "A plugin id to check with IDE, plugin verifier will check LAST plugin build only")
      .addOption("iv", "ide-version", true, "Version of IDE that will be tested, e.g. IU-133.439")
      .addOption("epf", "excluded-plugin-file", true, "File with list of excluded plugin builds.")
      .addOption("d", "dump-broken-plugin-list", true, "File to dump broken plugin list.")
      .addOption("report", "make-report", true, "Create a detailed report about broken plugins.")
      .addOption("xr", "save-results-xml", true, "Save results to xml file")
      .addOption("tc", "team-city-output", false, "Print TeamCity compatible output.")
      .addOption("pluginsFile", "plugins-to-check-file", true, "The file that contains list of plugins to check.")
      .addOption("cp", "external-class-path", true, "External class path")
      .addOption("printFile", true, ".xml report file to be printed in TeamCity")
      .addOption("repo", "results-repository", true, "Url of repository which contains check results")
      .addOption("pcr", "plugin-check-result", true, "File to dump result of checking plugin against IDEs")
      .addOption("g", "group", true, "Whether to group problems presentation (possible args are 'plugin' - group by plugin and 'type' - group by error-type)");

  public static void printHelp() {
    new HelpFormatter().printHelp("java -jar verifier.jar <command> [<args>]", CMD_OPTIONS);
  }

  @NotNull
  public static String getStackTrace(@Nullable Throwable t) {
    if (t == null) return "";
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }


  public static <T> List<T> concat(Collection<T> first, Collection<T> second) {
    List<T> res = new ArrayList<T>(first.size() + second.size());
    res.addAll(first);
    res.addAll(second);
    return res;
  }
}

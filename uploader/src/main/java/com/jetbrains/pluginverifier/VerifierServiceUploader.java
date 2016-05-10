package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.service.VerifierServiceApi;
import com.jetbrains.pluginverifier.utils.StringUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class VerifierServiceUploader {
  public static void main(String[] args) throws IOException {
    checkArgs(args);
    String serverUrl = StringUtil.trimEnd(args[0], "/");
    File fileToUpload = new File(args[1]);
    VerifierServiceApi.uploadReportFile(serverUrl, fileToUpload);
  }

  private static void checkArgs(String[] args) {
    if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
      throw new IllegalArgumentException("<server_url> <path_to_file>");
    }
  }
}

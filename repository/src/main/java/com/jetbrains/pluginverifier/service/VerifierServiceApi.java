package com.jetbrains.pluginverifier.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.misc.DownloadManager;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of verifier-service connection
 * (a web service which collects plugin breakages info)
 *
 * @author Sergey Patrikeev
 */
public class VerifierServiceApi {

  public static final String DEFAULT_SERVICE_URL = "http://localhost:7777/";

  private static final String UPLOAD_REPORT_PATH = "/uploadResult";

  private static final String RESULTS_PATH = "/results";
  private static final String RESULTS_LIST_PATH = "/resultsList";

  private static final Gson GSON = new Gson();
  private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {
  }.getType();
  private static final String XML_EXTENSION = ".xml";


  @NotNull
  private static List<String> requestFilesList(@NotNull String filesListUrl) throws IOException {
    HttpGet httpGet = new HttpGet(filesListUrl);
    CloseableHttpClient client = HttpClients.createDefault();

    try {
      CloseableHttpResponse response = client.execute(httpGet);

      try {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          List<String> result = GSON.fromJson(EntityUtils.toString(entity), LIST_STRING_TYPE);
          EntityUtils.consume(entity);
          return result;
        }
      } finally {
        IOUtils.closeQuietly(response);
      }
    } finally {
      IOUtils.closeQuietly(client);
    }

    throw new IOException("No files found on server " + filesListUrl);
  }

  private static void downloadFile(@NotNull String downloadFileUrl,
                                   @NotNull File fileToSaveInto) throws IOException {
    HttpGet httpGet = new HttpGet(downloadFileUrl);

    CloseableHttpClient client = HttpClients.createDefault();

    try {
      CloseableHttpResponse response = client.execute(httpGet);
      try {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          FileUtils.copyInputStreamToFile(entity.getContent(), fileToSaveInto);
          EntityUtils.consume(entity);
        } else {
          throw new IOException("No files found on server " + downloadFileUrl);
        }
      } finally {
        IOUtils.closeQuietly(response);
      }
    } finally {
      IOUtils.closeQuietly(client);
    }

  }


  private static void uploadFile(@NotNull String uploadUrl,
                                 @NotNull File fileToUpload) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    try {

      MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
      entityBuilder.addPart("file", new FileBody(fileToUpload));

      HttpPost httppost = new HttpPost(uploadUrl);

      httppost.setEntity(entityBuilder.build());

      CloseableHttpResponse response = httpclient.execute(httppost);
      try {
        System.out.println("----------------------------------------");
        System.out.println("Executing request: " + httppost.getRequestLine());
        System.out.println(response.getStatusLine());
        EntityUtils.consume(response.getEntity());

        if (response.getStatusLine().getStatusCode() != 200) {
          throw new IOException(String.format("Unable to upload %s, status code is %d, reason %s",
              fileToUpload, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase())
          );
        }
        System.out.println("Successfully uploaded " + fileToUpload);
      } finally {
        IOUtils.closeQuietly(response);
      }
    } finally {
      IOUtils.closeQuietly(httpclient);
    }
  }

  @NotNull
  public static List<IdeVersion> requestAvailableReports(@NotNull String repositoryUrl) throws IOException {
    List<String> files = requestFilesList(repositoryUrl + RESULTS_LIST_PATH);
    List<IdeVersion> builds = new ArrayList<IdeVersion>();
    for (String file : files) {
      builds.add(IdeVersion.createIdeVersion(StringUtil.trimEnd(file, XML_EXTENSION)));
    }
    return builds;
  }

  @NotNull
  public static File requestReportFile(@NotNull String repositoryUrl, @NotNull IdeVersion build) throws IOException {
    File resultFile = DownloadManager.getInstance().createCheckResultFile(build);
    downloadFile(repositoryUrl + RESULTS_PATH + "/" + build.asString() + XML_EXTENSION, resultFile);
    return resultFile;
  }

  public static void uploadReportFile(@NotNull String repositoryUrl,
                                      @NotNull File fileToUpload) throws IOException {
    uploadFile(repositoryUrl + UPLOAD_REPORT_PATH, fileToUpload);
  }
}

package com.jetbrains.pluginverifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.List;

/**
 * This is a dummy implementation of verifier-service (a web service which
 * collects plugin breakages info) connection
 *
 * @author Sergey Patrikeev
 */
public class VerifierServiceApi {

  public static final String DEFAULT_SERVICE_URL = "http://localhost:7777/";
  public static final String UPLOAD_PATH = "/upload";
  public static final String FILES_LIST_PATH = "/filesList";
  public static final String FILES = "/files";
  private static final Gson GSON = new Gson();
  private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {
  }.getType();

  @NotNull
  public static List<String> requestFilesList(@NotNull String verifierServiceUrl) throws IOException {
    HttpGet httpGet = new HttpGet(verifierServiceUrl + FILES_LIST_PATH);
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

    throw new IOException("No files found on server " + verifierServiceUrl);
  }

  public static void downloadFile(@NotNull String verifierServiceUrl,
                                  @NotNull String fileName,
                                  @NotNull File fileToSaveInto) throws IOException {
    HttpGet httpGet = new HttpGet(verifierServiceUrl + FILES + "/" + fileName);

    CloseableHttpClient client = HttpClients.createDefault();

    try {
      CloseableHttpResponse response = client.execute(httpGet);
      try {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          FileUtils.copyInputStreamToFile(entity.getContent(), fileToSaveInto);
          EntityUtils.consume(entity);
        } else {
          throw new IOException("No files found on server " + verifierServiceUrl);
        }
      } finally {
        IOUtils.closeQuietly(response);
      }
    } finally {
      IOUtils.closeQuietly(client);
    }

  }


  public static void uploadFile(@NotNull String verifierServiceUrl,
                                @NotNull File fileToUpload) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    try {

      MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
      entityBuilder.addPart("file", new FileBody(fileToUpload));

      HttpPost httppost = new HttpPost(verifierServiceUrl + UPLOAD_PATH);

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
}

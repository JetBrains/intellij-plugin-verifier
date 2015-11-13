package com.jetbrains.pluginverifier;

import com.google.common.collect.ArrayListMultimap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class Uploader {

  //0 http://plugins.jetbrains.com/problems/uploadResultsAPI</argument>
  //1 checkResults</argument>
  //2 build-report.xml</argument>
  //3 userName</argument>
  //4 pluginrobot</argument>
  //5 password</argument>
  //6 ${pluginrobot.password}</argument>


  //TODO: re-upload the first report with strange problems added
  public static void main(String[] args) throws IOException {
    parseArgsAndUpload(args);
  }

  private static void parseArgsAndUpload(String[] args) throws IOException {
    String postUrl = args[0];
    String filePartName = args[1];
    File fileToUpload = new File(args[2]);
    ArrayListMultimap<String, String> textBodies = ArrayListMultimap.create();
    for (int i = 3; i < args.length; i += 2) {
      textBodies.put(args[i], args[i + 1]);
    }

    uploadReport(postUrl, filePartName, fileToUpload, textBodies);
  }


  private static void uploadReport(@NotNull String postUrl,
                                   @NotNull String filePartName,
                                   @NotNull File fileToUpload,
                                   @NotNull ArrayListMultimap<String, String> textBodies) throws IOException {
    CloseableHttpClient httpclient = HttpClients.createDefault();

    try {

      MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
      entityBuilder.addPart(filePartName, new FileBody(fileToUpload)); //checkResults: build-report.xml


      HttpPost httppost = new HttpPost(postUrl);

      for (Map.Entry<String, String> entry : textBodies.entries()) {
        entityBuilder.addTextBody(entry.getKey(), entry.getValue());
      }

      httppost.setEntity(entityBuilder.build());

      System.out.println("Executing request: " + httppost.getRequestLine());
      CloseableHttpResponse response = httpclient.execute(httppost);
      try {
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        EntityUtils.consume(response.getEntity());

        if (response.getStatusLine().getStatusCode() != 200) {
          throw new RuntimeException("Unable to upload report, status code is " + response.getStatusLine().getStatusCode());
        }
      }
      finally {
        response.close();
      }
    }
    finally {
      httpclient.close();
    }
  }
}

package org.jetbrains.ide.diff.builder.maven

import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeUnit

val METADATA_VERSIONS_REGEX = Regex("<version>(.*)</version>")

private val DOWNLOAD_TIMEOUT = TimeUnit.MINUTES.toMillis(3).toInt()

private fun buildMavenMetadataUrl(
    repoUrl: String,
    groupId: String,
    artifactId: String
) = repoUrl.trimEnd('/') + '/' + groupId.replace('.', '/') + '/' + artifactId + "/maven-metadata.xml"

fun requestMavenAvailableVersions(repoUrl: String, groupId: String, artifactId: String): List<String> {
  val metadataUrl = URL(buildMavenMetadataUrl(repoUrl, groupId, artifactId))

  val timeout = TimeUnit.MINUTES.toMillis(1).toInt()
  val metadataContent = metadataUrl.openConnection().apply {
    connectTimeout = timeout
    readTimeout = timeout
  }.getInputStream().bufferedReader().readText()

  return METADATA_VERSIONS_REGEX.findAll(metadataContent)
      .map { it.groups[1]!!.value }
      .toList()
}

fun downloadArtifactTo(
    repositoryUrl: String,
    groupId: String,
    artifactId: String,
    version: String,
    result: Path,
    classifier: String = "",
    packaging: String = "jar"
) {
  val downloadUrl = URL(buildMavenDownloadUrl(
      repositoryUrl,
      groupId,
      artifactId,
      version,
      classifier,
      packaging
  ))
  FileUtils.copyURLToFile(downloadUrl, result.toFile(), DOWNLOAD_TIMEOUT, DOWNLOAD_TIMEOUT)
}


fun buildMavenDownloadUrl(
    repoUrl: String,
    groupId: String,
    artifactId: String,
    version: String,
    classifier: String = "",
    packaging: String = "jar"
): String = repoUrl.trimEnd('/') + '/' + groupId.replace('.', '/') +
    '/' + artifactId + "/$version/$artifactId-$version" +
    (if (classifier.isNotEmpty()) "-$classifier" else "") + ".$packaging"
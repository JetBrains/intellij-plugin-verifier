package org.jetbrains.ide.diff.builder.maven

import java.net.URL
import java.util.concurrent.TimeUnit

val METADATA_VERSIONS_REGEX = Regex("<version>(.*)</version>")

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
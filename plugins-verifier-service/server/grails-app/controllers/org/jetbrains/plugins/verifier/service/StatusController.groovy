package org.jetbrains.plugins.verifier.service

import com.google.common.collect.ImmutableList
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import kotlin.Pair
import org.jetbrains.plugins.verifier.service.setting.TrunkVersions

class StatusController {

  private static final def SUPPORTED_CLIENTS = ImmutableList.of("1.0")

  def index() {}

  def supportedClients() {
    sendJson(SUPPORTED_CLIENTS)
  }

  def listReleaseVersions() {
    List<Pair<Integer, IdeVersion>> versions = (141..165).collect {
      new Pair<Integer, IdeVersion>(it, TrunkVersions.INSTANCE.getReleaseVersion(it))
    }
    sendJson(versions)
  }

  def setReleaseVersion(int trunkNumber, String releaseVersion) {
    TrunkVersions.INSTANCE.setReleaseVersion(trunkNumber, IdeVersion.createIdeVersion(releaseVersion))
    listReleaseVersions()
  }

  private sendJson(Object obj) {
    String json
    if (obj instanceof String) {
      json = obj as String
    } else {
      json = GsonHolder.GSON.toJson(obj)
    }
    render(contentType: 'text/json', encoding: 'utf-8', text: json)
  }

}

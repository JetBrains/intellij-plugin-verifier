package com.intellij.packaging.artifacts;

public abstract class ArtifactType {
  private final String myId;
  private final String myTitle;

  protected ArtifactType(String id, String title) {
    myId = id;
    myTitle = title;
  }

  public final String getId() {
    return myId;
  }

}

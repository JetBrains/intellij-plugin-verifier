package com.jetbrains.pluginverifier.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.reports.Details;
import com.jetbrains.pluginverifier.reports.DetailsImpl;

import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
class DetailsTypeAdapter extends TypeAdapter<Details> {

  private static final String IDE_FIELD = "ide";
  private static final String PLUGIN_FIELD = "plugin";
  private static final String PROBLEMS_FIELD = "problems";
  private static final String OVERVIEW_FIELD = "overview";

  @Override
  public void write(JsonWriter out, Details value) throws IOException {
    out.name(IDE_FIELD);
    Persistence.GSON.toJson(value.checkedIde(), IdeVersion.class, out);
    out.name(PLUGIN_FIELD);
    Persistence.GSON.toJson(value.checkedPlugin(), UpdateInfo.class, out);
    out.name(PROBLEMS_FIELD);
    Persistence.GSON.toJson(value.problems(), Persistence.MULTIMAP_PROBLEMS_TYPE, out);
    out.name(OVERVIEW_FIELD);
    out.value(value.overview());
  }

  @Override
  public Details read(JsonReader in) throws IOException {
    Preconditions.checkArgument(IDE_FIELD.equals(in.nextName()));
    IdeVersion version = Persistence.GSON.fromJson(in, IdeVersion.class);

    Preconditions.checkArgument(PLUGIN_FIELD.equals(in.nextName()));
    UpdateInfo update = Persistence.GSON.fromJson(in, UpdateInfo.class);

    Preconditions.checkArgument(PROBLEMS_FIELD.equals(in.nextName()));
    Multimap<Problem, ProblemLocation> problems = Persistence.GSON.fromJson(in, Persistence.MULTIMAP_PROBLEMS_TYPE);

    Preconditions.checkArgument(OVERVIEW_FIELD.equals(in.nextName()));
    String overview = in.nextString();
    return new DetailsImpl(version, update, problems, overview);
  }
}

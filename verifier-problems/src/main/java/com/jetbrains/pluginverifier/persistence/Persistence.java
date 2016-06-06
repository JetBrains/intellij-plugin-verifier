package com.jetbrains.pluginverifier.persistence;

import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.reports.Details;
import com.jetbrains.pluginverifier.reports.Report;

import java.lang.reflect.Type;

/**
 * @author Sergey Patrikeev
 */
public class Persistence {

  public static final Gson GSON = new GsonBuilder()
      .serializeNulls() //it is important for UpdateInfo class which could contain null fields
      .registerTypeAdapter(UpdateInfo.class, new UpdateInfoTypeAdapter())
      .registerTypeHierarchyAdapter(IdeVersion.class, new IdeVersionTypeAdapter())
      .registerTypeHierarchyAdapter(Jsonable.class, new JsonableTypeAdapter())
      .registerTypeHierarchyAdapter(Report.class, new ReportTypeAdapter())
      .registerTypeHierarchyAdapter(Details.class, new DetailsTypeAdapter())
      .registerTypeAdapterFactory(new MultimapTypeAdapterFactory())
      .create();


  static final Type MULTIMAP_PROBLEMS_TYPE = new TypeToken<Multimap<Problem, ProblemLocation>>() {
  }.getType();
}

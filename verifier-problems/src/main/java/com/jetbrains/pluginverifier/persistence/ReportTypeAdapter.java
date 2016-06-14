package com.jetbrains.pluginverifier.persistence;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jetbrains.pluginverifier.reports.Details;
import com.jetbrains.pluginverifier.reports.Report;
import com.jetbrains.pluginverifier.reports.ReportImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
//TODO: optimize the size of output
class ReportTypeAdapter extends TypeAdapter<Report> {
  @Override
  public void write(JsonWriter out, Report value) throws IOException {
    out.beginArray();
    for (Details details : value.details()) {
      out.beginObject();
      Persistence.GSON.toJson(details, Details.class, out);
      out.endObject();
    }
    out.endArray();
  }

  @Override
  public Report read(JsonReader in) throws IOException {
    in.beginArray();
    List<Details> details = new ArrayList<Details>();
    while (in.hasNext()) {
      in.beginObject();
      details.add(Persistence.GSON.<Details>fromJson(in, Details.class));
      in.endObject();
    }
    in.endArray();
    return new ReportImpl(details);
  }
}

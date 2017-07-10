package com.jetbrains.structure.plugin;

import com.jetbrains.structure.product.ProductVersion;
import org.jetbrains.annotations.Nullable;

public interface Plugin {
  String getPluginId();

  String getPluginName();

  String getPluginVersion();

  ProductVersion getSinceBuild();

  @Nullable
  ProductVersion getUntilBuild();

  @Nullable
  String getUrl();

  @Nullable
  String getChangeNotes();

  @Nullable
  String getDescription();

  String getVendor();

  @Nullable
  String getVendorEmail();

  @Nullable
  String getVendorUrl();
}

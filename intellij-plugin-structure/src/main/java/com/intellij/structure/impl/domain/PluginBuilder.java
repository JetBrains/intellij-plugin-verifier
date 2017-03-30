package com.intellij.structure.impl.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginBuilder {
    private static final Document EMPTY_DOCUMENT = new Document();

    private Set<String> myDefinedModules = new HashSet<String>();
    private List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
    private List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
    private Map<PluginDependency, String> myOptionalConfigFiles = new HashMap<PluginDependency, String>();
    private Map<String, Plugin> myOptionalDescriptors = new HashMap<String, Plugin>();
    private Set<String> myReferencedClasses = new HashSet<String>();
    private Multimap<String, Element> myExtensions = ArrayListMultimap.create();
    private List<String> myHints = new ArrayList<String>();
    @NotNull private final File myPluginFile;
    @NotNull private Document myUnderlyingDocument = EMPTY_DOCUMENT;
    @NotNull private String myFileName = "(unknown)";
    @Nullable private byte[] myLogoContent;
    @Nullable private String myLogoUrl;
    @Nullable private String myPluginName;
    @Nullable private String myPluginVersion;
    @Nullable private String myPluginId;
    @Nullable private String myPluginVendor;
    @Nullable private String myVendorEmail;
    @Nullable private String myVendorUrl;
    @Nullable private String myDescription;
    @Nullable private String myUrl;
    @Nullable private String myNotes;
    @Nullable private IdeVersion mySinceBuild;
    @Nullable private IdeVersion myUntilBuild;

    public PluginBuilder(@NotNull File myPluginFile) {
        this.myPluginFile = myPluginFile;
    }

    public Map<PluginDependency, String> getOptionalConfigFiles() {
        return Collections.unmodifiableMap(myOptionalConfigFiles);
    }

    @Nullable
    public byte[] getMyLogoContent() {
        return myLogoContent;
    }

    public PluginBuilder addDefinedModule(@NotNull String module) {
        myDefinedModules.add(module);
        return this;
    }

    public PluginBuilder addModuleDependency(@NotNull PluginDependency dependency) {
        myModuleDependencies.add(dependency);
        return this;
    }

    public PluginBuilder addDependency(@NotNull PluginDependency dependency) {
        myDependencies.add(dependency);
        return this;
    }

    public PluginBuilder addOptionalConfigFile(@NotNull PluginDependency dependency, @NotNull String configFile) {
        myOptionalConfigFiles.put(dependency, configFile);
        return this;
    }

    public PluginBuilder addReferencedClass(@NotNull String className) {
        myReferencedClasses.add(className);
        return this;
    }

    public PluginBuilder addExtension(@NotNull String extensionName, Element element) {
        myExtensions.put(extensionName, element);
        return this;
    }

    public PluginBuilder addHint(@NotNull String hint) {
        myHints.add(hint);
        return this;
    }

    public PluginBuilder setOptionalDescriptors(@NotNull Map<String, Plugin> optionalDescriptors) {
        myOptionalDescriptors.clear();
        myOptionalDescriptors.putAll(optionalDescriptors);
        for (Plugin optDescriptor : optionalDescriptors.values()) {
            myExtensions.putAll(optDescriptor.getExtensions());
        }
        return this;
    }

    public PluginBuilder setPluginName(@Nullable String myPluginName) {
        this.myPluginName = myPluginName;
        return this;
    }

    public PluginBuilder setPluginVersion(@Nullable String myPluginVersion) {
        this.myPluginVersion = myPluginVersion;
        return this;
    }

    public PluginBuilder setPluginId(@Nullable String myPluginId) {
        this.myPluginId = myPluginId;
        return this;
    }

    public PluginBuilder setPluginVendor(@Nullable String myPluginVendor) {
        this.myPluginVendor = myPluginVendor;
        return this;
    }

    public PluginBuilder setVendorEmail(@Nullable String myVendorEmail) {
        this.myVendorEmail = myVendorEmail;
        return this;
    }

    public PluginBuilder setVendorUrl(@Nullable String myVendorUrl) {
        this.myVendorUrl = myVendorUrl;
        return this;
    }

    public PluginBuilder setDescription(@Nullable String myDescription) {
        this.myDescription = myDescription;
        return this;
    }

    public PluginBuilder setUrl(@Nullable String myUrl) {
        this.myUrl = myUrl;
        return this;
    }

    public PluginBuilder setNotes(@Nullable String myNotes) {
        this.myNotes = myNotes;
        return this;
    }

    public PluginBuilder setSinceBuild(@Nullable IdeVersion mySinceBuild) {
        this.mySinceBuild = mySinceBuild;
        return this;
    }

    public PluginBuilder setUntilBuild(@Nullable IdeVersion myUntilBuild) {
        this.myUntilBuild = myUntilBuild;
        return this;
    }

    public PluginBuilder setUnderlyingDocument(@NotNull Document myUnderlyingDocument) {
        this.myUnderlyingDocument = myUnderlyingDocument;
        return this;
    }

    public PluginBuilder setFileName(@NotNull String myFileName) {
        this.myFileName = myFileName;
        return this;
    }

    public PluginBuilder setLogoUrl(@Nullable String myLogoUrl) {
        this.myLogoUrl = myLogoUrl;
        return this;
    }

    public PluginBuilder setLogoContent(@Nullable byte[] myLogoContent) {
        this.myLogoContent = myLogoContent;
        return this;
    }

    @NotNull
    public Plugin build() {
        return new PluginImpl(
                myDefinedModules, myDependencies, myModuleDependencies, myOptionalConfigFiles, myOptionalDescriptors,
                myReferencedClasses, myExtensions, myPluginFile, myHints, myUnderlyingDocument, myFileName,
                myLogoContent, myLogoUrl, myPluginName, myPluginVersion, myPluginId, myPluginVendor, myVendorEmail,
                myVendorUrl, myDescription, myUrl, myNotes, mySinceBuild, myUntilBuild);
    }
}

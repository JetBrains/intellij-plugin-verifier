package com.jetbrains.plugin.structure.ktor.bean

// KtorFeature
const val ID = "id"
const val NAME = "name"
const val COPYRIGHT = "copyright"
const val DESCRIPTION = "description"
const val DEPENDENCY = "dependency"
const val VERSION = "version"
const val VENDOR = "vendor"
const val REQUIRED_FEATURES = "required_feature_ids"
const val DOCUMENTATION = "documentation"
const val INSTALL_RECEIPT = "app_install_receipt"
const val TEST_INSTALL_RECEIPT = "test_install_receipt"
const val GRADLE_INSTALL = "gradle_install"
const val MAVEN_INSTALL = "maven_install"

// Vendor
const val VENDOR_NAME = "name"
const val VENDOR_EMAIL = "email"
const val VENDOR_URL = "url"

// Documentation
const val DOCUMENTATION_DESCRIPTION = "text"
const val DOCUMENTATION_USAGE = "usage"
const val DOCUMENTATION_OPTIONS = "options"

// Install
const val INSTALL_IMPORTS = "imports"
const val INSTALL_BLOCK = "installBlock"
const val INSTALL_TEMPLATES = "extraTemplates"

// Maven Install
const val MAVEN_DEPENDENCIES = "dependencies"
const val MAVEN_REPOSITORIES = "repositories"
const val MAVEN_PLUGONS = "plugins"

// Gradle Install
const val GRADLE_DEPENDENCIES = "dependencies"
const val GRADLE_REPOSITORIES = "repositories"
const val GRADLE_PLUGONS = "plugins"

// Dependency
const val DEPENDENCY_GROUP = "group"
const val DEPENDENCY_ARTIFACT = "artifact"
const val DEPENDENCY_VERSION = "version"

// Maven Plugin
const val PLUGIN_GROUP = "group"
const val PLUGIN_ARTIFACT = "artifact"
const val PLUGIN_VERSION = "version"

// Gradle Plugin
const val PLUGIN_ID = "id"

// Maven Repository
const val MAVEN_REP_ID = "id"
const val MAVEN_REP_URL = "url"

// Gradle Repository
const val GRADLE_REP_TYPE = "type"
const val GRADLE_REP_FUNCTION = "function_name"
const val GRADLE_REP_URL = "url"

// Gradle Repository Type
const val GRADLE_REP_TYPE_FUNCTION = "function_call_based"
const val GRADLE_REP_TYPE_URL = "url_based"

// Code Template
const val TEMPLATE_POSITION = "position"
const val TEMPLATE_TEXT = "text"

// Code Position
const val POSITION_INSIDE = "inside_app"
const val POSITION_OUTSIDE = "outside_app"
const val POSITION_FILE = "separate_file"
const val POSITION_TESTFUN = "test_function"
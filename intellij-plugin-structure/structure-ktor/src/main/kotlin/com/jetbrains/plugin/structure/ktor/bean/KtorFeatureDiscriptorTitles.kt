package com.jetbrains.plugin.structure.ktor.bean

// KtorFeature
const val ID = "id"
const val NAME = "name"
const val COPYRIGHT = "copyright"
const val SHORT_DESCRIPTION = "short_description"
const val VERSION = "version"
const val KTOR_VERSION = "ktor_version"
const val VENDOR = "vendor"
const val REQUIRED_FEATURES = "required_feature_ids"
const val DOCUMENTATION = "documentation"
const val INSTALL_RECEIPT = "install_recipe"
const val GRADLE_INSTALL = "gradle_install"
const val MAVEN_INSTALL = "maven_install"

// Vendor
const val VENDOR_NAME = "name"
const val VENDOR_EMAIL = "email"
const val VENDOR_URL = "url"

// Documentation
const val DOCUMENTATION_DESCRIPTION = "description"
const val DOCUMENTATION_USAGE = "usage"
const val DOCUMENTATION_OPTIONS = "options"

// Install
const val INSTALL_BLOCK = "install_block"
const val INSTALL_IMPORTS = "imports"
const val INSTALL_TEST_IMPORTS = "test_imports"
const val INSTALL_TEMPLATES = "templates"

// Maven Install
const val MAVEN_DEPENDENCIES = "dependencies"
const val MAVEN_TEST_DEPENDENCIES = "test_dependencies"
const val MAVEN_REPOSITORIES = "repositories"
const val MAVEN_PLUGINS = "plugins"

// Gradle Install
const val GRADLE_DEPENDENCIES = "dependencies"
const val GRADLE_TEST_DEPENDENCIES = "test_dependencies"
const val GRADLE_REPOSITORIES = "repositories"
const val GRADLE_PLUGINS = "plugins"

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
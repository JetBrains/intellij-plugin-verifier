package com.jetbrains.plugin.structure.youtrack.bean

object YouTrackAppFields {
  object Manifest {
    const val NAME = "name"
    const val TITLE = "title"
    const val VERSION = "version"
    const val DESCRIPTION = "description"
    const val URL = "url"
    const val ICON = "icon"
    const val ICON_DARK = "iconDark"
    const val SINCE = "minYouTrackVersion"
    const val UNTIL = "maxYouTrackVersion"
    const val NOTES = "changeNotes"
    const val VENDOR = "vendor"
    const val WIDGETS = "widgets"
  }

  object Vendor {
    const val NAME = "name"
    const val URL = "url"
    const val EMAIL = "email"
  }

  object Widget {
    const val KEY = "key"
    const val EXTENSION_POINT = "extensionPoint"
    const val INDEX_PATH = "indexPath"
  }
}


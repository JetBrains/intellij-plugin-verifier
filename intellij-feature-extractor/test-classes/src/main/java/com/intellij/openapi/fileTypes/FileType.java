package com.intellij.openapi.fileTypes;

public interface FileType {
  FileType[] EMPTY_ARRAY = new FileType[0];

  /**
   * Returns the name of the file type. The name must be unique among all file types registered in the system.
   *
   * @return The file type name.
   */
  String getName();

  /**
   * Returns the user-readable description of the file type.
   *
   * @return The file type description.
   */

  String getDescription();

  /**
   * Returns the default extension for files of the type.
   *
   * @return The extension, not including the leading '.'.
   */

  String getDefaultExtension();

  /**
   * Returns true if files of the specified type contain binary data. Used for source control, to-do items scanning and
   * other purposes.
   *
   * @return true if the file is binary, false if the file is plain text.
   */
  boolean isBinary();

  /**
   * Returns true if the specified file type is read-only. Read-only file types are not shown in the "File Types"
   * settings dialog, and users cannot change the extensions associated with the file type.
   *
   * @return true if the file type is read-only, false otherwise.
   */

  boolean isReadOnly();

}
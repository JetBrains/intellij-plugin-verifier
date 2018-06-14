package com.jetbrains.pluginverifier.persistence

import com.jetbrains.pluginverifier.persistence.VerificationResultPersistence.readVerificationResult
import com.jetbrains.pluginverifier.persistence.VerificationResultPersistence.saveVerificationResult
import com.jetbrains.pluginverifier.results.VerificationResult
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*

/**
 * Utility class that provides methods to [save] [saveVerificationResult]
 * and [read] [readVerificationResult] [VerificationResult]s from/to files.
 */
object VerificationResultPersistence {
  fun saveVerificationResult(verificationResult: VerificationResult, outputStream: OutputStream) {
    ObjectOutputStream(outputStream).use { it.writeObject(verificationResult) }
  }

  fun saveVerificationResult(verificationResult: VerificationResult, path: Path) {
    Files.newOutputStream(path, CREATE, TRUNCATE_EXISTING, WRITE).use {
      saveVerificationResult(verificationResult, it)
    }
  }

  fun readVerificationResult(inputStream: InputStream) =
      ObjectInputStream(inputStream).use { it.readObject() as VerificationResult }

  fun readVerificationResult(path: Path) =
      Files.newInputStream(path, READ).use { readVerificationResult(it) }
}
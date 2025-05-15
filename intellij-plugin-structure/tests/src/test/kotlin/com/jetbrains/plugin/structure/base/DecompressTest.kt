import com.jetbrains.plugin.structure.base.utils.createMinimalisticTarGz
import com.jetbrains.plugin.structure.base.utils.extractTo
import com.jetbrains.plugin.structure.base.utils.newTemporaryFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DecompressTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `TAR_GZ is decompressed into a directory`() {
    val tarGzPath = temporaryFolder.newTemporaryFile("archives/ide.tar.gz")
    createMinimalisticTarGz(tarGzPath)

    val decompressedIdeFolder = temporaryFolder.newFolder("ide").toPath()
    tarGzPath.extractTo(decompressedIdeFolder)
  }
}

package scalajs.selenium.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

import org.scalajs.jsenv.selenium.SeleniumJSEnv

class SnowpackTestServer(baseDir: File) {
  private val contentDirName = "test-run"
  private val contentDir     = s"$baseDir/target/$contentDirName"
  private val testPort       = 9091
  private val webRoot        = s"http://localhost:$testPort/"

  private val process: AtomicReference[Option[Process]] = new AtomicReference(None)

  private val snowpackTestConfig: String =
    s"""
       |{
       |  "mount": {
       |    "$contentDir" : "/"
       |  },
       |  "devOptions": {
       |    "port": $testPort,
       |    "open": "none",
       |    "hmr": false
       |  }
       |}
       |""".stripMargin

  def start(): Unit = {
    val testConfigPath = Files.createTempFile(null, ".json")
    Files.write(testConfigPath, snowpackTestConfig.getBytes())
    testConfigPath.toFile.deleteOnExit()

    val processBuilder = new ProcessBuilder("npm", "start", "--", "--config", testConfigPath.toString)
      .directory(baseDir)
      .redirectError(Redirect.INHERIT)

    process.set(Some(processBuilder.start()))
  }

  def stop(): Unit = {
    val maybeProcess = process.getAndSet(None)
    maybeProcess.foreach(_.destroy())
  }

  def seleniumConfig: SeleniumJSEnv.Config = {
    SeleniumJSEnv
      .Config()
      .withMaterializeInServer(contentDir, webRoot)
  }
}

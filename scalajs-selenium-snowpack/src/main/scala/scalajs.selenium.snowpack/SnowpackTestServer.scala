package scalajs.selenium.snowpack

import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

import org.scalajs.jsenv.selenium.SeleniumJSEnv

class SnowpackTestServer(baseDir: File, crossTarget: File, testPort: Int, extraArgs: List[String], enableStdout: Boolean) {
  private val contentDirName = "test-run"
  private val contentDir     = s"$crossTarget/$contentDirName"
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

  def start(): String = {
    Files.createDirectories(crossTarget.toPath)
    val testConfigPath = crossTarget.toPath.resolve("snowpack.test.config.json")
    Files.write(testConfigPath, snowpackTestConfig.getBytes())

    val commands           = List("npm", "start", "--", "--config", testConfigPath.toString) ++ extraArgs
    val baseProcessBuilder = new ProcessBuilder(commands: _*).directory(baseDir)

    val processBuilder =
      if (enableStdout) baseProcessBuilder.inheritIO()
      else baseProcessBuilder.redirectError(Redirect.INHERIT)

    process.set(Some(processBuilder.start()))
    commands.mkString(" ")
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

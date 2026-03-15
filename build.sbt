import com.typesafe.sbt.packager.docker.ExecCmd
import org.scalajs.linker.interface.ModuleKind

ThisBuild / scalaVersion  := "3.8.2"
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / organization  := "io.sding"

lazy val scalacFromEnv = Seq.concat(
  if (sys.env.contains("GITHUB_ACTIONS")) Some("-Werror") else None,
  Seq(
    "-feature",
    "-Wunused:all",
    "-deprecation",
    "-Xmax-inlines:128"
  )
)
ThisBuild / scalacOptions ++= scalacFromEnv
ThisBuild / libraryDependencySchemes += "org.typelevel" %% "otel4s-core-trace" % "always"
ThisBuild / libraryDependencySchemes += "io.getquill"   %% "quill-engine"      % "always"

val http4sVersion      = "0.23.33"
val circeVersion       = "0.14.15"
val fs2Version         = "3.13.0"
val otel4sVersion      = "0.17.0"
val scribeVersion      = "3.18.0"
val scalatestVersion   = "3.2.19"
val langgraph4jVersion = "1.8.9"
val langchain4jVersion = "1.12.2"

// ─── shared (cross JVM + JS) ────────────────────────────────────────────────
lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    name := "sding-shared",
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-generic" % circeVersion,
      "io.circe"      %%% "circe-parser"  % circeVersion,
      "org.scalatest" %%% "scalatest"     % scalatestVersion % Test
    )
  )

// ─── server (JVM) ───────────────────────────────────────────────────────────
lazy val server = (project in file("server"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .dependsOn(shared.jvm)
  .settings(
    name                             := "sding",
    description                      := "sding",
    assembly / assemblyOutputPath    := file("target/sding-uber.jar"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case PathList("META-INF", "resources", "webjars", "swagger-ui", _*) =>
        MergeStrategy.singleOrError
      case PathList("module-info.class")                    => MergeStrategy.discard
      case x if x.endsWith("/module-info.class")            => MergeStrategy.discard
      case x if x.endsWith("/io.netty.versions.properties") => MergeStrategy.discard
      case PathList("io.netty.versions.properties")         => MergeStrategy.discard
      case x                                                =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    buildInfoKeys            := Seq[BuildInfoKey](name, version),
    buildInfoPackage         := "sding",
    Test / parallelExecution := true,
    Test / fork              := true,
    Test / javaOptions ++= Seq("-Xms512m", "-Xmx2g", "-XX:+AlwaysPreTouch"),
    Test / envVars := Map("RELEASE_NAME" -> name.value),
    Test / testOptions ++= (if (sys.env.contains("GITHUB_ACTIONS"))
                              Seq(Tests.Argument(TestFrameworks.ScalaTest, "-l", "loadtest"))
                            else Nil),
    dockerBaseImage                      := "eclipse-temurin:21-jre-alpine",
    Docker / daemonUserUid               := None,
    Docker / daemonUser                  := "sding",
    Docker / daemonGroup                 := "sding",
    Docker / defaultLinuxInstallLocation := "/app",
    dockerExposedPorts                   := Seq(8080),
    dockerCmd                            := Seq(s"/app/bin/sding"),
    dockerCommands                       := dockerCommands.value.filterNot {
      case ExecCmd("ENTRYPOINT", _*) => true
      case _                         => false
    },
    libraryDependencies ++= Seq(
      "org.http4s"           %% "http4s-core"                   % http4sVersion,
      "org.http4s"           %% "http4s-client"                 % http4sVersion,
      "org.http4s"           %% "http4s-server"                 % http4sVersion,
      "org.http4s"           %% "http4s-dsl"                    % http4sVersion,
      "org.http4s"           %% "http4s-ember-client"           % http4sVersion,
      "org.http4s"           %% "http4s-ember-server"           % http4sVersion,
      "io.circe"             %% "circe-literal"                 % circeVersion,
      "org.http4s"           %% "http4s-circe"                  % http4sVersion,
      "is.cir"               %% "ciris"                         % "3.12.0",
      "org.typelevel"        %% "cats-effect"                   % "3.7.0",
      "org.scalatest"        %% "scalatest"                     % scalatestVersion % Test,
      "org.typelevel"        %% "cats-effect-testing-scalatest" % "1.8.0"          % Test,
      "org.typelevel"        %% "cats-effect-testkit"           % "3.7.0"          % Test,
      "co.fs2"               %% "fs2-core"                      % fs2Version,
      "co.fs2"               %% "fs2-io"                        % fs2Version,
      "org.typelevel"        %% "otel4s-sdk"                    % otel4sVersion,
      "org.typelevel"        %% "otel4s-sdk-exporter"           % otel4sVersion,
      "com.outr"             %% "scribe"                        % scribeVersion,
      "com.outr"             %% "scribe-slf4j2"                 % scribeVersion,
      "com.outr"             %% "scribe-cats"                   % scribeVersion,
      "com.outr"             %% "scribe-json-circe"             % scribeVersion,
      "org.bsc.langgraph4j"   % "langgraph4j-core"              % langgraph4jVersion,
      "dev.langchain4j"       % "langchain4j"                   % langchain4jVersion,
      "dev.langchain4j"       % "langchain4j-google-ai-gemini"  % langchain4jVersion,
      "dev.langchain4j"       % "langchain4j-open-ai"           % langchain4jVersion,
      "dev.langchain4j"       % "langchain4j-anthropic"         % langchain4jVersion,
      "org.snakeyaml"         % "snakeyaml-engine"              % "3.0.1",
      "io.getquill"          %% "quill-jdbc"                    % "4.8.6",
      "org.flywaydb"          % "flyway-core"                   % "12.1.0",
      "org.flywaydb"          % "flyway-database-postgresql"    % "12.1.0",
      "org.postgresql"        % "postgresql"                    % "42.7.10",
      "com.github.jwt-scala" %% "jwt-circe"                     % "11.0.3",
      "at.favre.lib"          % "bcrypt"                        % "0.10.2"
    )
  )

// ─── client (Scala.js) ─────────────────────────────────────────────────────
lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(shared.js)
  .settings(
    name                            := "sding-client",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= Seq(
      "com.raquo"     %%% "laminar"     % "17.2.1",
      "org.scala-js"  %%% "scalajs-dom" % "2.8.1",
      "org.scalatest" %%% "scalatest"   % scalatestVersion % Test
    )
  )

// ─── frontend bundle task ───────────────────────────────────────────────────
lazy val bundleFrontend = taskKey[Unit]("Build optimized frontend JS and copy to server resources")

// ─── dev: backend + frontend (npm install if needed, server in background, vite dev) ─
lazy val dev = taskKey[Unit]("Run backend and frontend (npm install + server in background + vite dev)")

// ─── root aggregator ────────────────────────────────────────────────────────
lazy val root = (project in file("."))
  .aggregate(shared.jvm, shared.js, server, client)
  .settings(
    name         := "sding-root",
    publish      := {},
    publishLocal := {},
    dev          := {
      val log         = streams.value.log
      val clientDir   = (client / baseDirectory).value
      val nodeModules = clientDir / "node_modules"
      if (!nodeModules.exists()) {
        log.info("Installing node modules...")
        val exit = scala.sys.process.Process("npm" :: "install" :: Nil, clientDir).!
        if (exit != 0) throw new MessageOnlyException("npm install failed")
      }
      (server / Compile / compile).value
      val cp   = (server / Runtime / fullClasspath).value.files.mkString(java.io.File.pathSeparator)
      val main = (server / Compile / run / mainClass).value
        .orElse((server / Compile / discoveredMainClasses).value.headOption)
        .getOrElse("sding.Main")
      log.info("Starting backend...")
      val serverProcess = scala.sys.process.Process("java" :: "-cp" :: cp :: main :: Nil, baseDirectory.value).run()
      sys.addShutdownHook(serverProcess.destroy())
      log.info("Starting frontend (Vite)...")
      val exit = scala.sys.process.Process("npm" :: "run" :: "dev" :: Nil, clientDir).!
      if (exit != 0) throw new MessageOnlyException(s"npm run dev exited with $exit")
    },
    bundleFrontend := {
      val log = streams.value.log
      log.info("Building optimized frontend JS...")
      val linkResult = (client / Compile / fullLinkJS).value
      val jsDir      = linkResult.data.publicModules.head.jsFileName
      val sourceDir  = (client / Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val targetDir  = (server / Compile / resourceDirectory).value / "static"
      IO.createDirectory(targetDir)
      IO.copyDirectory(sourceDir, targetDir)
      val indexHtml = (client / baseDirectory).value / "index.html"
      if (indexHtml.exists()) IO.copyFile(indexHtml, targetDir / "index.html")
      log.info(s"Frontend bundle copied to $targetDir")
    }
  )

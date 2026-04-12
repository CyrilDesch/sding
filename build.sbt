import com.typesafe.sbt.packager.docker.ExecCmd

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
val otel4sVersion      = "0.18.0"
val scribeVersion      = "3.18.0"
val scalatestVersion   = "3.2.19"
val langgraph4jVersion = "1.8.9"
val langchain4jVersion = "1.12.2"

// ─── server (JVM) ───────────────────────────────────────────────────────────
lazy val server = (project in file("server"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
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
    Compile / mainClass      := Some("sding.Main"),
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
      "io.circe"             %% "circe-generic"                 % circeVersion,
      "io.circe"             %% "circe-parser"                  % circeVersion,
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

// ─── root aggregator ────────────────────────────────────────────────────────
lazy val root = (project in file("."))
  .aggregate(server)
  .settings(
    name         := "sding-root",
    publish      := {},
    publishLocal := {}
  )

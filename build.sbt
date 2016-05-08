import org.scalastyle.sbt.ScalastylePlugin._

val Specs2Version = "3.6.5"

val PlayVersion = "2.5.1"

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

lazy val commonSettings = Seq(
  organization := "au.com.agiledigital",
  scalaVersion := "2.11.8",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-encoding", "UTF-8"),
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint", // Enable recommended additional warnings.
    "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
    "-Ywarn-numeric-widen" // Warn when numerics are widened.
  ),
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play" % PlayVersion % Provided,
    "org.jsoup" % "jsoup" % "1.8.3",
    "org.scalaz" %% "scalaz-core" % "7.1.3"
  ),
  // Disable scaladoc generation in dist.
  sources in(Compile, doc) := Seq.empty,
  updateOptions := updateOptions.value.withCachedResolution(true),
  // Restrict resources that will be used.
  concurrentRestrictions in Global := Seq(
    Tags.limitSum(1, Tags.Compile, Tags.Test),
    Tags.limitAll(2)
  ),
  scalastyleFailOnError := true,
  compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
  (test in Test) <<= (test in Test) dependsOn compileScalastyle,
  // To publish, put these credentials in ~/.ivy2/credentials
  //credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", "****", "****"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  wartremoverErrors in (Compile, compile) ++= Seq(
        Wart.FinalCaseClass,
        Wart.Null,
        Wart.TryPartial,
        Wart.Var,
        Wart.OptionPartial,
        Wart.ListOps,
        Wart.EitherProjectionPartial,
        Wart.Any2StringAdd,
        Wart.AsInstanceOf,
        Wart.ExplicitImplicitTypes,
        Wart.MutableDataStructures,
        Wart.Return,
        Wart.AsInstanceOf,
        Wart.IsInstanceOf)
) ++ Formatting.formattingSettings

lazy val root = (project in file(".")).
  settings(commonSettings:_*).
  settings(
    name := "play-rest-support-root",
    publish := {},
    publishArtifact := false
  ).
  aggregate(
    core,
    testkit,
    coreTests
  )

lazy val core = (project in file("core")).
  settings(commonSettings: _*).
  settings(
    name := "play-rest-support"
  )

lazy val coreTests = (project in file("core-tests")).
  settings(commonSettings: _*).
  settings(
    name := "play-rest-support-tests",
    fork in Test := false,
    parallelExecution in Test := false,
    publish := {},
    publishArtifact := false
  ).
  dependsOn(testkit % Test).
  dependsOn(core)

lazy val testkit = (project in file("testkit")).
  settings(commonSettings: _*).
  settings(
    name := "play-rest-support-testkit",
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % Specs2Version,
      "org.specs2" %% "specs2-junit" % Specs2Version,
      "org.specs2" %% "specs2-matcher-extra" % Specs2Version,
      "org.specs2" %% "specs2-mock" % Specs2Version,
      "com.typesafe.play" %% "play-specs2" % PlayVersion
    ),
    coverageExcludedPackages := ".*test.*"
  ).
  dependsOn(core)

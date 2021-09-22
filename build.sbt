ThisBuild / organization := "example"
ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0"
ThisBuild / scalacOptions ++= Seq(
  "-target:11",
  "-feature",
  "-unchecked",
)

lazy val root = (project in file("."))
  .settings(
    name := "custom-slickgen",
  )

ThisBuild / libraryDependencies ++= {
  val slickVersion = "3.3.3"

  Seq(
    "org.liquibase" % "liquibase-core" % "4.4.3",
    "com.h2database" % "h2" % "1.4.200",
    "org.slf4j" % "slf4j-simple" % "1.7.32",

    "com.typesafe.slick" %% "slick"          % slickVersion,
    "com.typesafe.slick" %% "slick-codegen"  % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,

    // for code generation
    "info.picocli" % "picocli" % "4.6.1" % Provided,
    "org.scala-lang" % "scala-library" % scalaVersion.value,
  )
}

// Slick code generation

Compile / sourceGenerators += domainClassGeneration.taskValue

lazy val domainClassGeneration = taskKey[Seq[File]]("Generate the domain classes from the database schema")
domainClassGeneration := {
  val logger = streams.value.log
  val url = "jdbc:h2:./database/data"
  val classpath =
    (baseDirectory.value / "project" / "target" * "scala*" * "sbt*" * "classes").get() ++
      (Compile / dependencyClasspath).value.files

  // Run Liquibase

  val baseDir = (ThisBuild / baseDirectory).value.toPath
  val resourceDir = (Compile / resourceDirectory).value
  val changelog = baseDir.relativize((resourceDir / "liquibase.xml").toPath).toString

  runner.value.run("liquibase.integration.commandline.LiquibaseCommandLine",
    classpath, Seq("update", s"--changelog-file=$changelog", s"--url=$url"), logger
  ).recover {
    case t: Throwable => sys.error(t.getMessage)
  }

  // Run Slick

  val profile   = "slick.jdbc.H2Profile"
  val driver    = "org.h2.Driver"
  val outputDir = (Compile / sourceManaged).value / "slick"
  val pkg       = "example.domain"
  val className = classOf[ExampleCodeGenerator].getName

/*
  slick.codegen.SourceCodeGenerator.run(
    profile    = profile,
    jdbcDriver = driver,
    url        = url,
    outputDir  = outputDir.getPath,
    pkg        = pkg,
    user       = None,
    password   = None,
    ignoreInvalidDefaults = true,
    codeGeneratorClass    = className.some,
    outputToMultipleFiles = true
  )
*/

/*
  slick.codegen.SourceCodeGenerator.main(
    Array(profile, driver, url, outputDir.getPath, pkg, "", "", "true", className, "true")
  )
*/

  runner.value.run("slick.codegen.SourceCodeGenerator",
    classpath, Seq(profile, driver, url, outputDir.getPath, pkg, "", "", "true", className, "true"), logger
  ).recover {
    case t: Throwable => sys.error(t.getMessage)
  }

  ((outputDir / pkg.replace('.', '/')) ** "*.scala").get()
}

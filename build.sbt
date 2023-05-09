name:= "HL7-PET"

githubOwner := "cdcgov"
githubRepository := "HL7-PET"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

organization:= "gov.cdc.hl7"
organizationName:= "CDC"
scmInfo:= Some (
  ScmInfo(
    url("https://github.com/cdcgov/hl7-pet"),
    "scm:git@github.com/cdcgov/hl7-pet.git"
  )
)

developers := List(
  Developer(
    id="mcq1",
    name="Marcelo Caldas",
    email = "mcq1@cdc.com",
    url = url ("https://github.com/cdcgov/hl7-pet")
  )
)

description := "this project is a library to Parse HL7 v2 messages"
licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/cdcent/hl7-pet"))

pomIncludeRepository := { _ => false }

publishTo := Some("GitHub cdcgov Apache Maven Packages" at "https://maven.pkg.github.com/cdcgov/hl7-pet");
  
//credentials += Credentials ("~/.sbt/sonatype_credentials")
//credentials += Credentials ("CDC Nexus Repository Manager", "https://imagehub.cdc.gov/", "mcq1", "")
credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "cdcgov",
  System.getenv("GITHUB_TOKEN")
);

publishMavenStyle := true


//useGpg := true

version := "1.2.7.3"

//scalaVersion := "2.12.17"
scalaVersion:= "2.13.10"

mainClass := Some("gov.cdc.hl7pet.DeIdentifierApp")
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.14"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.14" % Test

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.0"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
 libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-modules-base
 libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.14.0" pomOnly()
// https://mvnrepository.com/artifact/com.google.code.gson/gson
 libraryDependencies += "com.google.code.gson" % "gson" % "2.10"
// https://mvnrepository.com/artifact/org.scalatest/scalatest-flatspec
//libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.14" % Test


//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

//Do not append scala versions to the generated artifact
crossPaths:= true

publishArtifact in (Compile, packageSrc) := true

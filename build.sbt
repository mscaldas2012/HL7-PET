name := "HL7-PET"

organization:= "io.github.mscaldas2012"
organizationName:= "mscaldas2012"
scmInfo:= Some (
  ScmInfo(
    url("https://github.com/mscaldas2012/HL7-PET"),
    "scm:git@github.com:mscaldas2012/HL7-PET.git"
  )
)

developers := List(
  Developer(
    id="mscaldas2012",
    name="Marcelo Caldas",
    email = "mscaldas@gmail.com",
    url = url ("https://github.com/mscaldas2012")
  )
)

description := "this project is a library to Parse HL7 v2 messages"
licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/mscaldas2012/HL7-PET"))

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://imagehub.cdc.gov/repository/maven-ede/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "")
}


//credentials += Credentials ("~/.sbt/sonatype_credentials")
//credentials += Credentials ("CDC Nexus Repository Manager", "https://imagehub.cdc.gov/", "mcq1", "")
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
publishMavenStyle := true


//useGpg := true

version := "1.2.7"

scalaVersion := "2.13.8"

mainClass := Some("open.HL7PET.tools.DeIdentifierApp")
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.4"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
 libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.4"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-modules-base
 libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.13.4" pomOnly()
// https://mvnrepository.com/artifact/com.google.code.gson/gson
 libraryDependencies += "com.google.code.gson" % "gson" % "2.9.0"

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

//Do not append scala versions to the generated artifact
crossPaths:= true

publishArtifact in (Compile, packageSrc) := true

name := "HL7-PET"

organization:= "gov.cdc.hl7"
organizationName:= "CDC"
//scmInfo:= Some (
//  ScmInfo(
//    url("https://github.com/cdcent/hl7-pet"),
//    "scm:git@github.com/cdcent/hl7-pet.git"
//  )
//)

developers := List(
  Developer(
    id="mcq1",
    name="Marcelo Caldas",
    email = "mcq1@cdc.com",
    url = url ("https://github.com/cdcent/hl7-pet")
  )
)

description := "This project is a library to Parse HL7 v2 messages"
licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/cdcent/hl7-pet"))

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://imagehub.cdc.gov/repository/maven-ede/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "/")
}


//credentials += Credentials ("~/.sbt/sonatype_credentials")
//credentials += Credentials ("CDC Nexus Repository Manager", "https://imagehub.cdc.gov/", "mcq1", "")
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
publishMavenStyle := true


//useGpg := true

version := "1.2.10"

scalaVersion := "2.13.10"
//scalaVersion := "2.12.10"

mainClass := Some("gov.cdc.hl7pet.DeIdentifierApp")
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.18"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.0"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
 libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-modules-base
 libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.17.1" pomOnly()
// https://mvnrepository.com/artifact/com.google.code.gson/gson
 libraryDependencies += "com.google.code.gson" % "gson" % "2.10.1"

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

//Do not append scala versions to the generated artifact
crossPaths:= true

publishArtifact in (Compile, packageSrc) := true

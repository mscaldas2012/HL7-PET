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
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true

useGpg := true

version := "1.2.1-SNAPSHOT"

scalaVersion := "2.13.0"

mainClass := Some("hl7.DeIdentifierApp")
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
// https://mvnrepository.com/artifact/org.codehaus.jackson/jackson-core-asl
libraryDependencies += "org.codehaus.jackson" % "jackson-core-asl" % "1.9.13"
// https://mvnrepository.com/artifact/org.codehaus.jackson/jackson-mapper-asl
libraryDependencies += "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13"

//Do not append scala versions to the generated artifact
crossPaths:= false

publishArtifact in (Compile, packageSrc) := true

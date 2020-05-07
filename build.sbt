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

//credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
//credentials += Credentials ("~/.sbt/sonatype_credentials")
credentials += Credentials ("Sonatype Nexus Repository Manager", "oss.sonatype.org", "mscaldas2019", "^fgAKRQ99:K^Vx4aCQGW")
publishMavenStyle := true


useGpg := true

version := "1.2.4"

scalaVersion := "2.13.0"

mainClass := Some("hl7.DeIdentifierApp")
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.1"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
 libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.1"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-modules-base
 libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.10.1" pomOnly()
// https://mvnrepository.com/artifact/com.google.code.gson/gson
 libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"



//Do not append scala versions to the generated artifact
crossPaths:= false

publishArtifact in (Compile, packageSrc) := true

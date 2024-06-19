name:= "HL7-PET"

//githubOwner := "cdc.gov"
//githubRepository := "HL7-PET"
//githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")


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

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
//publishTo := Some("GitHub cdcgov Apache Maven Packages" at "https://maven.pkg.github.com/cdcgov/hl7-pet")
//credentials += Credentials(
//  "GitHub Package Registry",
//  "maven.pkg.github.com",
//  "cdcgov",
//  System.getenv("GITHUB_TOKEN")
//)
>>>>>>> 11b2a2b225555e8a9ef99bc7f4a42303b34960f1
publishTo := {
  val nexus = "https://imagehub.cdc.gov/repository/maven-ede/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "/")
}
<<<<<<< HEAD
=======
//publishTo := {
//  val nexus = "https://imagehub.cdc.gov/repository/maven-ede/"
//  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
//  else Some("releases" at nexus + "/")
//}
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d
=======
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
>>>>>>> 11b2a2b225555e8a9ef99bc7f4a42303b34960f1


//credentials += Credentials ("~/.sbt/sonatype_credentials")
//credentials += Credentials ("CDC Nexus Repository Manager", "https://imagehub.cdc.gov/", "mcq1", "")

<<<<<<< HEAD
<<<<<<< HEAD
>>>>>>> a34297ddf8384bd748876031aec1d62324a17f2c
=======
//publishTo := Some("GitHub cdcgov Apache Maven Packages" at "https://maven.pkg.github.com/cdcgov/hl7-pet")
//credentials += Credentials(
//  "GitHub Package Registry",
//  "maven.pkg.github.com",
//  "cdcgov",
//  System.getenv("GITHUB_TOKEN")
//)
>>>>>>> 11b2a2b225555e8a9ef99bc7f4a42303b34960f1
publishMavenStyle := true

version := "1.2.9"
scalaVersion:= "2.13.10"

<<<<<<< HEAD
//mainClass in assembly := Some("gov.cdc.hl7pet.DeIdentifierApp")
mainClass := Some("gov.cdc.hl7pet.DeIdentifierApp")
Global / excludeLintKeys += mainClass

libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.14"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.14" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.0"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.14.0" pomOnly()
libraryDependencies += "com.google.code.gson" % "gson" % "2.10"
=======
version := "1.2.9.1"
=======
version := "1.2.10"
>>>>>>> 5320168fb9083e67d0257221fc839c243cbc5dab

scalaVersion := "2.13.10"
//scalaVersion := "2.12.10"

mainClass := Some("gov.cdc.hl7pet.DeIdentifierApp")
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.18"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.0"
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d

<<<<<<< HEAD
<<<<<<< HEAD
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
<<<<<<< HEAD
=======
>>>>>>> 11b2a2b225555e8a9ef99bc7f4a42303b34960f1
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.14.0" pomOnly()
libraryDependencies += "com.google.code.gson" % "gson" % "2.10"
//libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.14" % Test

=======
 libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-modules-base
 libraryDependencies += "com.fasterxml.jackson.module" % "jackson-modules-base" % "2.17.0" pomOnly()
// https://mvnrepository.com/artifact/com.google.code.gson/gson
 libraryDependencies += "com.google.code.gson" % "gson" % "2.10.1"
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

crossPaths:= true

publishArtifact in (Compile, packageSrc) := true
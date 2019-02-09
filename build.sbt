name := "hl7_utils"

organization:= "gov.cdc.ncezid.eip"

version := "1.1.0-SNAPSHOT" 

scalaVersion := "2.12.2"

mainClass := Some("hl7.DeIdentifierApp")

libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.1" % "test"
// https://mvnrepository.com/artifact/org.codehaus.jackson/jackson-core-asl
libraryDependencies += "org.codehaus.jackson" % "jackson-core-asl" % "1.9.13"
// https://mvnrepository.com/artifact/org.codehaus.jackson/jackson-mapper-asl
libraryDependencies += "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.13"

credentials += Credentials("Sonatype Nexus Repository Manager", "eip.sandbox.aimsplatform.net" , "admin", "admin123")

publishMavenStyle := true
//Do not append scala versions to the generated artifact
crossPaths:= false

publishArtifact in (Compile, packageSrc) := true
publishMavenStyle := true
publishTo := {
  val nexus = "http://10.32.1.132:8381/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "repository/maven-snapshots")
  else
    Some("releases" at nexus + "repository/maven-releases")
}


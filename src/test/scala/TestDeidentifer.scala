//import open.HL7PET.tools.HL7StaticParser.{NEW_LINE_FEED, PATH_REGEX}

import gov.cdc.hl7.{DeIdentifier, RedactInfo}
import gov.cdc.hl7.HL7StaticParser.NEW_LINE_FEED
import gov.cdc.utils.FileUtils
import org.scalatest.flatspec.AnyFlatSpec

import java.util
import scala.io.Source
//import org.scalatest.FlatSpec

class TestDeidentifer extends AnyFlatSpec {

  "DeIdentifier" should "clean data" in {
    val d = new DeIdentifier()
    d.deIdentifyFile( "src/test/resources/ORU_SampleOne.hl7", "src/main/resources/redaction_rules.txt")
  }

  "Deidentifier" should "generate report" in {
    val d = new DeIdentifier()
    val msg = FileUtils.readFile("src/test/resources/ORU_SampleOne.hl7")
    val rules = FileUtils.readFile("src/main/resources/redaction_rules.txt").split(NEW_LINE_FEED)
    val (redactedMessage, report) = d.deIdentifyMessage(msg, rules)
    println(report)
  }

  "sbustring" should "be found" in {
    val testLine = "PID|1||PID00753334^^^TN.Nashville.SPHL&2.16.840.1.114222.4.1.175791&ISO^PI||~^^^^^^U|||F|||^^^TN^^USA^C^^47055"
    val field = "3"
    var initIndex = 0
    for( a <- 0 to (field.toInt-1)) {
      initIndex = testLine.indexOf("|", initIndex+1)
      println(initIndex)
    }
    print(testLine.substring(0, initIndex+1))
    print("REDACTED")
    println(testLine.substring(testLine.indexOf("|",initIndex+2), testLine.length))

  }

  "hl7-pet" should "redact message" in {
    val d = new DeIdentifier()
    val msg = loadFile("covid19_elr.hl7")
    val rules = loadFile("redaction_rules.txt").split(NEW_LINE_FEED)
    val (redactedMessage, report) = d.deIdentifyMessage(msg, rules)
    printReport(report)
    println(s"redacted message:\n$redactedMessage")
  }

  "hl7-pet" should "redact celr message" in {
    val d = new DeIdentifier()
    val msg = loadFile("HL7_2.5_New HHS Fields1.txt")
    val rules = loadFile("CELR-config.txt").split(NEW_LINE_FEED)
    val (redactedMessage, report) = d.deIdentifyMessage(msg, rules)
    printReport(report)
    println(s"redacted message:\n$redactedMessage")
  }

  private def printReport(report: util.List[RedactInfo]): Unit = {
    report.forEach( i => println(s"${i.lineNumber}) ${i.path}: ${i.rule}"))
  }

  private def loadFile(fileName: String): String = {
    Source.fromResource(fileName).getLines().mkString("\n")
  }

  "array get" should "get values" in {
    val r: Option[Array[Array[Boolean]]] = Some(Array(Array(false, false)))

    val v1 = r.get(0).reduce(_ || _)
//    val v1 = a2.reduce(_ || _)
  println(v1)
  }
}

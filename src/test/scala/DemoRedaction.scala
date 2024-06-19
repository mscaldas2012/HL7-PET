import gov.cdc.hl7.HL7StaticParser.NEW_LINE_FEED
import gov.cdc.hl7.{DeIdentifier, RedactInfo}
import org.scalatest.flatspec.AnyFlatSpec

import java.util
import scala.io.Source

class DemoRedaction extends AnyFlatSpec {

  "hl7-pet" should "redact message" in {
    val d = new DeIdentifier()
    val msg = loadFile("covid19_elr.hl7")
    val rules = loadFile("redaction_rules.txt").split(NEW_LINE_FEED)
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

}

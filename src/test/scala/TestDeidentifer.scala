//import open.HL7PET.tools.HL7StaticParser.{NEW_LINE_FEED, PATH_REGEX}

import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.HL7StaticParser.NEW_LINE_FEED
import gov.cdc.utils.FileUtils
import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.FlatSpec

class TestDeidentifer extends AnyFlatSpec {

  "DeIdentifier" should "clean data" in {
    val d = new DeIdentifier()
    d.deIdentifyFile( "src/test/resources/ORU_SampleOne.hl7", "src/main/resources/deid_rules.txt")
  }

  "Deidentifier" should "generate report" in {
    val d = new DeIdentifier()
    val msg = FileUtils.readFile("src/test/resources/ORU_SampleOne.hl7")
    val rules = FileUtils.readFile("src/main/resources/deid_rules.txt").split(NEW_LINE_FEED)
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
}

import open.HL7PET.tools.HL7StaticParser.{NEW_LINE_FEED, PATH_REGEX}
import open.HL7PET.tools.{DeIdentifier, HL7ParseUtils, HL7StaticParser}
import org.scalatest.FlatSpec
import utils.FileUtils

class TestDeidentifer extends FlatSpec{



  "DeIdentifier" should "clean data" in {
    val d = new DeIdentifier()
    d.deIdentifyFile( "src/test/resources/DHQP_SPM_OTH_SECOND.hl7", "src/main/resources/deid_rules.txt")
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

import gov.cdc.hl7.HL7ParseUtils
<<<<<<< HEAD

import org.scalatest.flatspec.AnyFlatSpec
=======
import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.FlatSpec
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d

import scala.io.Source

class TestBugs extends AnyFlatSpec {

  def readResourceFile(fileName: String): String = {
    val content: String = Source.fromResource(fileName).getLines().mkString("\n")
    content
  }

  "Children SPM" should "bring children when adding [1]" in {
    val testMessage = readResourceFile("ORU_SampleOne.hl7")
    val hl7Util = new HL7ParseUtils(testMessage, null, true)

    val path = "OBR[4]->SPM-4"

    val result = hl7Util.getValue(path)
//    if (result.isDefined && result.get.length > 0) {
//      val r = result.get
//      print(r(0)(0))
//    } else print("no value found")
    printResults(result)
  }

  def printResults(maybeStrings: Option[Array[Array[String]]]) = {
    println(s"results ")
    if (maybeStrings.isDefined) {
      maybeStrings.get foreach {
        v => v.foreach(f => println(s"\t--> $f"))
      }
    }
    println("---")
  }
}




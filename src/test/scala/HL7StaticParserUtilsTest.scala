<<<<<<< HEAD
import gov.cdc.hl7.{HL7ParseError, HL7ParseUtils, HL7StaticParser}
import org.scalatest.flatspec.AnyFlatSpec
=======
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.{BatchValidator, HL7ParseError, HL7ParseUtils, HL7StaticParser}
import gov.cdc.hl7.model.Profile
import org.scalatest.flatspec.AnyFlatSpec

import java.sql.ResultSet
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d
//import org.scalatest.FlatSpec


<<<<<<< HEAD
class HL7StaticParserUtilsTest extends AnyFlatSpec {
  private val testMessage = "MSH|^~\\&|MDSS^2.16.840.1.114222.4.3.2.2.3.161.1.1000.1^ISO|MDCH^2.16.840.1.114222.4.1.3660^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20150632162510||ORU^R01^ORU_R01|5276074519_20150626162510529|P|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Hepatitis_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO\r" +
=======

class HL7StaticParserUtilsTest extends AnyFlatSpec {
  private val testMessage =
    "MSH|^~\\&|MDSS^2.16.840.1.114222.4.3.2.2.3.161.1.1000.1^ISO|MDCH^2.16.840.1.114222.4.1.3660^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20150632162510||ORU^R01^ORU_R01|5276074519_20150626162510529|P|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Hepatitis_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO\r" +
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d
    "PID|1||5276074529^^^MDCH&2.16.840.1.114222.4.1.3660&ISO||~^^^^^^S||19600101|F||2106-3^Caucasian^CDCREC~1002-5^American Indian^CDCREC|^^ANN ARBOR^26^48105^USA^^^26161|||||||||||2135-2^Hispanic or Latino^CDCREC|||||||20141031\r" +
    "OBR|1||5276074519^MDCH^2.16.840.1.114222.4.1.3660^ISO|68991-9^Epidemiologic Information^LN|||20150626162510|||||||||||||||20150626162510|||F||||||10110^Hepatitis A^NND\r" +
    "OBX|1|CWE|NOT116^National Reporting Jurisdiction^PHINQUESTION||26^Michigan^FIPS5_2||||||F\n\r" +
    "OBX|2|CWE|NOT109^Reporting State^PHINQUESTION||26^Michigan^FIPS5_2~13^Georgia^FIPS5_2||||||F\r\n" +
    "OBX|3|CWE|INV163^Case Class Status Code^PHINQUESTION||410605003^Confirmed present (qualifier value)^SCT||||||F\r" +
    "OBX|7|SN|77977-7^Illness Duration^LN||^13|^^ISO|||||F\n" +
    "OBR|5||xyz^MDCH^2.16.840.1.114222.4.1.3660^ISO|777777-9^LAB Information^LN|||20150626162510|||||||||||||||20150626162510|||F||||||12345^Hepatitis A^NND\n\r" +
    "OBX|8|CWE|NOT116^National Reporting Jurisdiction^PHINQUESTION||26^Michigan^FIPS5_2||||||F\n\r" +
    "OBR|3||abc^MDCH^2.16.840.1.114222.4.1.3660^ISO|68991-9^Epidemiologic Information^LN|||20150626162510|||||||||||||||20150626162510|||F||||||10110^Hepatitis A^NND\n\r" +
    "OBX|9|CWE|NOT116^National Reporting Jurisdiction^PHINQUESTION||26^Michigan^FIPS5_2||||||F\n\r"


  //  var batchValidator = new BatchValidator(testMessage, null)

  "New line" should "split on any combination" in {
    val NEW_LINE_FEED = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
    val lines = testMessage.split(NEW_LINE_FEED)
    lines.foreach { it =>
      println(" --> " + it)
    }
    assert(lines.size == 11)
  }

  "Parser" should "split messages" in {
    var (line, msh) = HL7StaticParser.retrieveSegment(testMessage,"MSH")
//    println(line + ":" + msh(0))

    var index = 1
//      stuff.foreach(l =>  l.foreach( i => {
//        println(s"MSH[$index] $i")
//        index = index +1
//      }))

    println("Splitted already: " + msh(8))
    println("Full line: " + msh.mkString("|"))
//
    var field9 = HL7StaticParser.splitFields(msh, 9)
    println("each msh-9: ")
    field9.foreach(println)

    var field12 = HL7StaticParser.splitFields(msh, 12)
    println("version: ")
    field12.foreach(println)

    var (line2, condition) = HL7StaticParser.retrieveFirstSegmentOf(testMessage, "OBR")

    println("Condition: " + condition(31) + " at line " + line2)
    println("Full line: " + condition.mkString("|"))
    var cond = HL7StaticParser.splitFields(condition, 31)
    println("each condition: ")
    cond.foreach(println)
    try {
      var invalid = HL7StaticParser.splitFields(condition, 49)
      println("Should throw error!")
    } catch {
      case c: HL7ParseError => println("Success! Error thrown! " + c)
    }
  }
//
//  "Parser" should "throw error with invalid segment" in {
//    try {
//      var msh = hl7Util.retrieveSegment("ABC")
//      println("Should throw error!")
//    } catch {
//      case c: HL7ParseError => println("Success! error thrown! " + c)
//    }
//  }
//
//  "Parser" should "throw error " in {
//    try {
//      var obx = hl7Util.retrieveSegment("OBX")
//      println("Should throw error!")
//    } catch {
//      case c: HL7ParseError => println("Success! error thrown!! " + c)
//    }
//  }
//
//  "Parser" should "split batched messages  " in {
//    val content = scala.io.Source.fromFile("src/test/resources/Cancer.txt").mkString
//    val batchedMessages = new BatchValidator(content, null)
//    val eachMsg = batchedMessages.debatchMessages()
//    println("found " + eachMsg.size + " Messages")
//    //eachMsg.foreach(l => println("Found msg... "  + l.substring(0, 100)))
//    println("First Message: " )
//    println(eachMsg.head)
//  }
//
//  "Parser" should "split single message" in {
//    val batchedMessages = new HL7ParseUtils(testMessage)
//    val eachMsg = batchValidator.debatchMessages()
//    //eachMsg.foreach(l => println("Found msg... " + l.substring(0, 100)))
//    println("First Message: ")
//    println(eachMsg.head)
//    assert(eachMsg.size == 1)
//  }
//
//  "Parser" should "return empty for non-existing segment" in {
//      var fhs = hl7Util.peek("FHS")
//      assert(fhs == 0)
//  }
//
//  "Parser" should "check no MSH available" in {
//    val emptyMessage = "FHS|asdfasdfafads\n" +
//                        "BHS|adsfasfasfadsf\n" +
//                        "BTS|0\n" +
//                        "FTS|1"
//    val emptyHL7Parser = new HL7ParseUtils(emptyMessage, null, false)
//    val emptyBatchValidator = new BatchValidator(emptyMessage, null)
//    val noMsg = emptyBatchValidator.debatchMessages()
//    assert(noMsg.isEmpty)
//
//  }
//
//  "OBX Lines" should "retrieve multiple lines" in {
//    val obxMap = hl7Util.retrieveMultipleSegments("OBX")
//    for ((line, seg) <- obxMap) println(s"line: $line, se: $seg")
//  }
//
//  "Parser" should "debatch to 4 messages" in {
//    var allLines: String = readFile("oregonMessage.hl7")
//    //val parser: HL7ParseUtils = new HL7ParseUtils(allLines)
//    val validator: BatchValidator = new BatchValidator(allLines, null)
//    val msgs: List[String] = validator.debatchMessages()
//    println(" found " + msgs.length+ "  messages")
//
//  }
//
//  private def readFile(filename: String) = {
//    val source = io.Source.fromResource(filename)
//    var allLines = ""
//    if (source != null) {
//      for (line <- source.getLines) {
//        allLines += line + "\n"
//      }
//    }
//    allLines
//  }
//
//  "Parser" should "GetCondition from first OBR" in {
//    val obxMap = hl7Util.retrieveFirstSegmentOf("OBR")
//    println(s"line: ${obxMap._1}")
//    obxMap._2.foreach(s => println(s))
//  }
//  val PATH_REGEX = "([A-Z]{3})(\\[([0-9]+|\\*|(@[0-9\\.\\='\\- ]*))\\])?(\\-([0-9]+)(\\[([0-9]+|\\*)\\])?((\\.([0-9]+))(\\.([0-9]+))?)?)?".r
//
//  "MHS has no OBR?"  must "flag error" in {
//    var allLines: String = readFile("MSHNoOBR.hl7")
//    val hl7Utils = new HL7ParseUtils(allLines, null,false)
//    try {
//      val obr = hl7Utils.retrieveFirstSegmentOf("OBR")
//      println(s"Line ${obr._1} ")
//      obr._2.foreach(s => println(s))
//      assert(false)
//    } catch {
//      case e:HL7ParseError => assert(true)
//      case _ => assert(false)
//    }
//
//  }
//
//
//
//  def printResults(maybeStrings: Option[Array[Array[String]]]) = {
//    println(s"results ")
//    if (maybeStrings.isDefined) {
//      maybeStrings.get foreach {
//        v => v.foreach(f => println(s"\t--> $f"))
//      }
//    }
//    println("---")
//  }
//  "bug" should "be fixed" in {
//    //printResults(hl7Util.getValue("OBX[@3.1='NOT109']-5.2"))
//    printResults(hl7Util.getValue("OBX[4]-6.3"))
//
//  }
  "Paths" should "be found" in {

    println("Simple Evals...")
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH").get.startsWith("MSH|"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-21[3].1").get.equals("Hepatitis_MMG_V1.0"))
    assert(HL7StaticParser.getFirstValue(testMessage, "OBR[1]-4[1]").get.equals("68991-9^Epidemiologic Information^LN"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH[1]-9[1].3").get.equals("ORU_R01"))
    assert(HL7StaticParser.getFirstValue(testMessage, "PID-3.4.2").get.equals("2.16.840.1.114222.4.1.3660"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-12").get.equals("2.5.1"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-12.1").get.equals("2.5.1"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-12.1.1").get.equals("2.5.1"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH[1]-12.1.1").get.equals("2.5.1"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-12[1].1.1").get.equals("2.5.1"))
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH[1]-12[1].1.1").get.equals("2.5.1"))

    println("\n\nOrder...")
    assert(HL7StaticParser.getFirstValue(testMessage, "OBX[1]").get.startsWith("OBX|1|CWE|NOT116^National Reporting Jurisdiction"))
    assert(HL7StaticParser.getFirstValue(testMessage, "OBX[2]").get.startsWith("OBX|2|CWE|NOT109^Reporting State"))
    assert(HL7StaticParser.getFirstValue(testMessage, "OBX[3]").get.startsWith("OBX|3|CWE|INV163^Case Class Status Code"))


    println("\n\nRepeats...")
    assert(HL7StaticParser.getValue(testMessage, "OBR[*]-4[1].1").get.length == 3)
<<<<<<< HEAD
    assert(HL7StaticParser.getValue(testMessage, "OBR-4[1]").get.length == 2)
=======
    assert(HL7StaticParser.getValue(testMessage, "OBR-4[1]").get.length == 3)
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d
    assert(HL7StaticParser.getValue(testMessage, "PID[1]-5[*]").get.length == 1)



    println("\n\nEmpty or no match evals")
    // All those are beyond possible indexes.. should return None!
    assert(HL7StaticParser.getValue(testMessage, "MSH[2]-9").isEmpty)
    assert(HL7StaticParser.getValue(testMessage, "OBR[1]-4[2]").isEmpty)
    assert(HL7StaticParser.getValue(testMessage, "MSH[1]-9[1].4").isEmpty)
    assert(HL7StaticParser.getValue(testMessage, "PID[1]-3[1].4.4").isEmpty)
    assert(HL7StaticParser.getValue(testMessage, "INV[1]").isEmpty) //Invalid Segment altogether!
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH-99").isEmpty)
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH[2]-9").isEmpty)
    assert(HL7StaticParser.getFirstValue(testMessage, "OBR[1]-4[2]").isEmpty)
    assert(HL7StaticParser.getFirstValue(testMessage, "MSH[1]-9[1].4").isEmpty)
    assert(HL7StaticParser.getFirstValue(testMessage, "PID[1]-3[1].4.4").isEmpty)
    assert(HL7StaticParser.getFirstValue(testMessage, "INV[1]").isEmpty) //Invalid Segment altogether!

    //empty results...
    assert(HL7StaticParser.getValue(testMessage, "OBR[*]-4.1.2").isEmpty)
    assert(HL7StaticParser.getValue(testMessage, "OBR[*].4.1.badpath").isEmpty)

  }

  "Paths with inequalities" should "be found" in {

    println("Simple Evals...")
    val hl7p = new HL7ParseUtils(testMessage, null, true)
    val maybeString = hl7p.getValue("OBR[@1>'2']", true)
    printResults(maybeString)
    //assert(maybeString.get.startsWith("OBR"))
  }

  "$LAST Index" should "return last entry" in {

    val hl7p = new HL7ParseUtils(testMessage, null, true)
    val maybeString = hl7p.getValue("MSH-21[$LAST]", true)
    printResults(maybeString)
    val maybeStringOBR = hl7p.getValue("OBR[$LAST]->OBX", true)
    printResults(maybeStringOBR)

    val maybeStringOBX = hl7p.getValue("OBX[@3.1='NOT109']-5[$LAST]", true)
    printResults(maybeStringOBX)

    //assert(maybeString.get.startsWith("OBR"))
  }

  def printResults(resultSet: Option[Array[Array[String]]]) = {
    println(s"results ")
    if (resultSet.isDefined) {
      resultSet.get foreach {
        v => v.foreach(f => println(s"\t--> $f"))
      }
    }
    println("---")
  }

  def getProfile(fileName: String): Profile = {
    val profileFile = Source.fromResource(fileName).getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(profileFile, classOf[Profile])

  }



}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.{BatchValidator, HL7ParseError, HL7ParseUtils}
import gov.cdc.hl7.model.Profile
import org.scalatest.FlatSpec

import scala.io.Source


class HL7ParserUtilsTest extends FlatSpec {
  private val testMessage = "MSH|^~\\&|MDSS^2.16.840.1.114222.4.3.2.2.3.161.1.1000.1^ISO|MDCH^2.16.840.1.114222.4.1.3660^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20150632162510||ORU^R01^ORU_R01|5276074519_20150626162510529|P|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Hepatitis_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO\r" +
    "PID|1||5276074529^^^MDCH&2.16.840.1.114222.4.1.3660&ISO||~^^^^^^S||19600101|F||2106-3^Caucasian^CDCREC~1002-5^American Indian^CDCREC|^^ANN ARBOR^26^48105^USA^^^26161|||||||||||2135-2^Hispanic or Latino^CDCREC|||||||20141031\r" +
    "OBR|1||5276074519^MDCH^2.16.840.1.114222.4.1.3660^ISO|68991-9^Epidemiologic Information^LN|||20150626162510|||||||||||||||20150626162510|||F||||||10110^Hepatitis A^NND\r" +
    "OBX|1|CWE|NOT116^National Reporting Jurisdiction^PHINQUESTION||26^Michigan^FIPS5_2||||||F\n\r" +
    "OBX|2|CWE|NOT109^Reporting State^PHINQUESTION||26^Michigan^FIPS5_2~13^Georgia^FIPS5_2||||||F\r\n" +
    "OBX|3|CWE|INV163^Case Class Status Code^PHINQUESTION||410605003^Confirmed present (qualifier value)^SCT||||||F\r" +
    "OBX|7|SN|77977-7^Illness Duration^LN||^13|^^ISO|||||F\n" +
    "OBR|2||5276074519^MDCH^2.16.840.1.114222.4.1.3660^ISO|777777-9^LAB Information^LN|||20150626162510|||||||||||||||20150626162510|||F||||||12345^Hepatitis A^NND"

  var batchValidator = new BatchValidator(testMessage, null)


  var hl7Util = new HL7ParseUtils(testMessage, null, false)

  def getParser() = {
     new HL7ParseUtils(testMessage)
  }
  "New line" should "split on any combination" in {
    val NEW_LINE_FEED = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
    var lines = testMessage.split(NEW_LINE_FEED)
    lines.foreach { it =>
      println(" --> " + it)
    }
    assert(lines.size == 8)
  }

  "Parser" should "split messages" in {
    var (line, msh) = hl7Util.retrieveSegment("MSH")

    var index = 1
    //  stuff.foreach(l =>  l.foreach( i => {
    //    println(s"MSH[$index] $i")
    //    index = index +1
    //  }))

    println("Splitted already: " + msh(8))
    println("Full line: " + msh.head.mkString("|"))

    var field9 = hl7Util.splitFields(msh, 9)
    println("each msh-9: ")
    field9.foreach(println)

    var field12 = hl7Util.splitFields(msh, 12)
    println("version: ")
    field12.foreach(println)

    var (line2, condition) = hl7Util.retrieveFirstSegmentOf("OBR")

    println("Condition: " + condition(31) + " at line " + line2)
    println("Full line: " + condition.mkString("|"))
    var cond = hl7Util.splitFields(condition, 31)
    println("each condition: ")
    cond.foreach(println)
    try {
      var invalid = hl7Util.splitFields(condition, 49)
      println("Should throw error!")
    } catch {
      case c: HL7ParseError => println("Success! Error thrown! " + c)
    }
  }

  "Parser" should "throw error with invalid segment" in {
    try {
      var msh = hl7Util.retrieveSegment("ABC")
      println("Should throw error!")
    } catch {
      case c: HL7ParseError => println("Success! error thrown! " + c)
    }
  }

  "Parser" should "throw error " in {
    try {
      var obx = hl7Util.retrieveSegment("OBX")
      println("Should throw error!")
    } catch {
      case c: HL7ParseError => println("Success! error thrown!! " + c)
    }
  }

  "Parser" should "split batched messages  " in {
    val content = scala.io.Source.fromFile("src/test/resources/Cancer.txt").mkString
    val batchedMessages = new BatchValidator(content, null)
    val eachMsg = batchedMessages.debatchMessages()
    println("found " + eachMsg.size + " Messages")
    //eachMsg.foreach(l => println("Found msg... "  + l.substring(0, 100)))
    println("First Message: " )
    println(eachMsg.head)
  }

  "Parser" should "split single message" in {
    val batchedMessages = new HL7ParseUtils(testMessage)
    val eachMsg = batchValidator.debatchMessages()
    //eachMsg.foreach(l => println("Found msg... " + l.substring(0, 100)))
    println("First Message: ")
    println(eachMsg.head)
    assert(eachMsg.size == 1)
  }

  "Parser" should "return empty for non-existing segment" in {
      var fhs = hl7Util.peek("FHS")
      assert(fhs == 0)
  }

  "Parser" should "check no MSH available" in {
    val emptyMessage = "FHS|asdfasdfafads\n" +
                        "BHS|adsfasfasfadsf\n" +
                        "BTS|0\n" +
                        "FTS|1"
    val emptyHL7Parser = new HL7ParseUtils(emptyMessage, null, false)
    val emptyBatchValidator = new BatchValidator(emptyMessage, null)
    val noMsg = emptyBatchValidator.debatchMessages()
    assert(noMsg.isEmpty)

  }

  "OBX Lines" should "retrieve multiple lines" in {
    val obxMap = hl7Util.retrieveMultipleSegments("OBX")
    for ((line, seg) <- obxMap) println(s"line: $line, se: $seg")
  }

  "Parser" should "debatch to 4 messages" in {
    var allLines: String = readFile("oregonMessage.hl7")
    //val parser: HL7ParseUtils = new HL7ParseUtils(allLines)
    val validator: BatchValidator = new BatchValidator(allLines, null)
    val msgs: List[String] = validator.debatchMessages()
    println(" found " + msgs.length+ "  messages")

  }

  private def readFile(filename: String) = {
    val source = io.Source.fromResource(filename)
    var allLines = ""
    if (source != null) {
      for (line <- source.getLines) {
        allLines += line + "\n"
      }
    }
    allLines
  }

  "Parser" should "GetCondition from first OBR" in {
    val obxMap = hl7Util.retrieveFirstSegmentOf("OBR")
    println(s"line: ${obxMap._1}")
    obxMap._2.foreach(s => println(s))
  }
  val PATH_REGEX = "([A-Z]{3})(\\[([0-9]+|\\*|(@[0-9\\.\\='\\- ]*))\\])?(\\-([0-9]+)(\\[([0-9]+|\\*)\\])?((\\.([0-9]+))(\\.([0-9]+))?)?)?".r

  "MHS has no OBR?"  must "flag error" in {
    var allLines: String = readFile("MSHNoOBR.hl7")
    val hl7Utils = new HL7ParseUtils(allLines, null,false)
    try {
      val obr = hl7Utils.retrieveFirstSegmentOf("OBR")
      println(s"Line ${obr._1} ")
      obr._2.foreach(s => println(s))
      assert(false)
    } catch {
      case e:HL7ParseError => assert(true)
      case _ => assert(false)
    }

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
  "bug" should "be fixed" in {
    //printResults(hl7Util.getValue("OBX[@3.1='NOT109']-5.2"))
    printResults(getParser().getValue("OBX[4]-6.3"))

  }
  "Paths" should "be found" in {

    println("Simple Evals...")
    assert(getParser().getFirstValue("MSH").get.startsWith("MSH|"))
    assert(getParser().getFirstValue("MSH-21[3].1").get.equals("Hepatitis_MMG_V1.0"))
    assert(getParser().getFirstValue("OBR[1]-4[1]").get.equals("68991-9^Epidemiologic Information^LN"))
    assert(getParser().getFirstValue("MSH[1]-9[1].3").get.equals("ORU_R01"))
    assert(getParser().getFirstValue("PID-3.4.2").get.equals("2.16.840.1.114222.4.1.3660"))
    assert(getParser().getFirstValue("MSH-12").get.equals("2.5.1"))
    assert(getParser().getFirstValue("MSH-12.1").get.equals("2.5.1"))
    assert(getParser().getFirstValue("MSH-12.1.1").get.equals("2.5.1"))
    assert(getParser().getFirstValue("MSH[1]-12.1.1").get.equals("2.5.1"))
    assert(getParser().getFirstValue("MSH-12[1].1.1").get.equals("2.5.1"))
    assert(getParser().getFirstValue("MSH[1]-12[1].1.1").get.equals("2.5.1"))

    println("\n\nOrder...")
    assert(getParser().getFirstValue("OBX[1]").get.startsWith("OBX|1|CWE|NOT116^National Reporting Jurisdiction"))
    assert(getParser().getFirstValue("OBX[2]").get.startsWith("OBX|2|CWE|NOT109^Reporting State"))
    assert(getParser().getFirstValue("OBX[3]").get.startsWith("OBX|3|CWE|INV163^Case Class Status Code"))


    println("\n\nRepeats...")
    assert(getParser().getValue("OBR[*]-4[1].1").get.length == 2)
    assert(getParser().getValue("OBR-4[1]").get.length == 2)
    assert(getParser().getValue("PID[1]-5[*]").get.length == 1)


    println("\n\nEmpty or no match evals")
    // All those are beyond possible indexes.. should return None!
    assert(getParser().getValue("MSH[2]-9").isEmpty)
    assert(getParser().getValue("OBR[1]-4[2]").isEmpty)
    assert(getParser().getValue("MSH[1]-9[1].4").isEmpty)
    assert(getParser().getValue("PID[1]-3[1].4.4").isEmpty)
    assert(getParser().getValue("INV[1]").isEmpty) //Invalid Segment altogether!
    assert(getParser().getFirstValue("MSH-99").isEmpty)
    assert(getParser().getFirstValue("MSH[2]-9").isEmpty)
    assert(getParser().getFirstValue("OBR[1]-4[2]").isEmpty)
    assert(getParser().getFirstValue("MSH[1]-9[1].4").isEmpty)
    assert(getParser().getFirstValue("PID[1]-3[1].4.4").isEmpty)
    assert(getParser().getFirstValue("INV[1]").isEmpty) //Invalid Segment altogether!

    //empty results...
    assert(getParser().getValue("OBR[*]-4.1.2").isEmpty)
    assert(getParser().getValue("OBR[*].4.1.badpath").isEmpty)

  }

  "Paths with filters" should "be found" in {
    println("Both NOT116 -> 109")
    val result = hl7Util.getValue("OBX[@3.1='NOT116||NOT109']-5")
    result.get.foreach (it => it.foreach { itt => println(itt)})
    assert(result.get.length == 2)

    println("109 second")
    val result2 = hl7Util.getValue("OBX[@3.1='Invalid||NOT109']-5")
    result2.get.foreach (it => it.foreach { itt => println(itt)})
    assert(result2.get.length == 1)

    println("NOT109 first")
    val result3 = hl7Util.getValue("OBX[@3.1='NOT109||Invalid']-5")
    result3.get.foreach (it => it.foreach { itt => println(itt)})
    assert(result3.get.length == 1)

    println("Neither")
    val result4 = hl7Util.getValue("OBX[@3.1='Invalid1||Invalid2']-5")
    assert(result4.isEmpty)


    assert(hl7Util.getValue("OBX[*]").get.length == 4)
    assert(hl7Util.getFirstValue("OBX[@3.1='NOT116']-5").get.equals("26^Michigan^FIPS5_2"))
    assert(hl7Util.getValue("MSH[@12='2.5.1']-21").get(0).length == 3)
    assert(hl7Util.getValue("MSH[@12.1='2.5.1']-21").get(0).length == 3)
    assert(hl7Util.getValue("MSH[@12.2='2.5.1']-21").isEmpty)
    assert(hl7Util.getValue("MSH[@99='2.5.1']-21").isEmpty)
    assert(hl7Util.getValue("OBX[@BADFILTER]").isEmpty)
  }


  "PathRegEx" should "match these" in {
    val segmentName = "OBX[@3.1='77990-0']-5[1].2.3"

    PATH_REGEX.findAllIn(segmentName).foreach(println)
    println("groups:")

    val matched = PATH_REGEX.findFirstMatchIn(segmentName)
    matched match {
      case Some(m) => println("matched - " + m.groupCount)
      //      println(s"${m.group(1)} .. ${m.group(2)} .. ${m.group(3)}.. ${m.group(4)}.. ${m.group(5)}.. ${m.group(6)}.. ${m.group(7)}.. ${m.group(8)}.. ${m.group(9)}.. ${m.group(10)}.. ${m.group(11)}.. ${m.group(12)}.. ${m.group(13)}")
      case None => println("No matched!")
    }

    printGroups("OBX[@3.1='77990-0']-5[1].2.3")
    println("----")
    printGroups("OBX[179]-5[1].2.3")

  }
  def printGroups(segmentName: String) = {
    segmentName match {
      case PATH_REGEX(one, _, three, _, five, _, seven, _, _, ten, _, twelv) =>
        println(s"12: $one .. $three .. $five ..  $seven ..  $ten ..  $twelv")

      case PATH_REGEX(one, two, three, four, five, six, seven, eight, nine, ten, elev, twelv, thir ) =>
        println(s"13: $one .. $two .. $three .. $four .. $five .. $six .. $seven .. $eight .. $nine ..  $ten .. $elev ..  $twelv .. $thir")
      case PATH_REGEX(one) =>
        println(s"1: $one")
      case FILTER_REGEX(one, _, _, four, _, six, seven) => println(s"12: $one .. $four .. $six ..  $seven")
      case _ => println(" No match!")
    }
  }

  val FILTER_REGEX = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?\\='([A-Za-z0-9\\-]+)'".r
  "FilterRegEx" should "match these" in {
    val segmentName = "@3.1.2='77990-0'"

    FILTER_REGEX.findAllIn(segmentName).foreach(println)
    println("groups:")

    val matched = FILTER_REGEX.findFirstMatchIn(segmentName)
    matched match {
      case Some(m) => println("matched - " + m.groupCount)
            println(s"${m.group(1)} .. ${m.group(2)} .. ${m.group(3)} .. ${m.group(4)} .. ${m.group(5)} .. ${m.group(6)} .. ${m.group(7)} ")//.. ${m.group(8)}.. ${m.group(9)}.. ${m.group(10)}.. ${m.group(11)}.. ${m.group(12)}.. ${m.group(13)}")
      case None => println("No matched!")
    }
    printGroups("@3.1.2='77990-0'")
    println("----")
    printGroups("@4='abc'")
  }

  "regex" should "match" in {
    println("2.16.8.140".matches("[0-9](\\.[0-9]+)+"))
  }


   "Hierarchy" must "be loaded" in {
      val profile = getProfile()
      val message = Source.fromResource("23zExample.hl7").getLines().mkString("\n")
      val parser = new HL7ParseUtils(message, profile, true)
      println(parser.getFirstValue("MSH-12"))
   }


  def getProfile(): Profile = {
    val profileFile = Source.fromResource("COVID_ORC.json").getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(profileFile, classOf[Profile])

  }
}



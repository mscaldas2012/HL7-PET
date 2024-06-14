import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.model.Profile
import gov.cdc.hl7.{HL7ParseUtils, HL7StaticParser}
import org.scalatest.flatspec.AnyFlatSpec

import scala.io.Source

class DemoUseCases extends AnyFlatSpec {

  "static hl7-pet-demo " should "query data" in {
    //load message from file into String var
    val testMessage = loadFile("covid19_elr.hl7")
    //query message
    val results = HL7StaticParser.getValue(testMessage, "OBR[1] -> OBX")
    //show results
    printResults(results)
  }

  "hierarchical hl7-pet-demo " should "query data" in {
    //load message from file into String var
    val testMsg = loadFile("covid19_elr.hl7")

    //init profile and Hl7 Parser
    val profile = getProfile("DefaultProfile.json")
    val hl7Util = new HL7ParseUtils(testMsg, profile, true )

    //query message
    val results = hl7Util.getFirstValue("PID[1]-10[2].2")
    println(s"First Value is ${results.get}")
    //show results
    //printResults(results)
  }

  private def printResults(resultSet: Option[Array[Array[String]]]) = {
    println(s"results ")
    if (resultSet.isDefined) {
      val flat = resultSet.get.flatten
        flat.foreach (v => println(s"\t--> $v"))
    }
    println("---")
  }

  private def getProfile(fileName: String): Profile = {
    val profileFile = loadFile(fileName)
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(profileFile, classOf[Profile])
  }

  private def loadFile(fileName: String): String = {
    Source.fromResource(fileName).getLines().mkString("\n")
  }

}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.model.Profile
import gov.cdc.hl7.{HL7ParseUtils, HL7StaticParser}
import org.scalatest.flatspec.AnyFlatSpec

import scala.io.Source

class DemoUseCases extends AnyFlatSpec {
  "static hl7-pet-demo " should "query data" in {
    //load mesage from file into String var
    val testMsg = Source.fromResource("covid19_elr.hl7").getLines().mkString("\n")
    //query message
    val matchfound = HL7StaticParser.getValue(testMsg, "MSH")
    //show results
    printResults(matchfound)
  }

  "hierarchical hl7-pet-demo " should "query data" in {
    //load mesage from file into String var
    val testMsg = Source.fromResource("covid19_elr.hl7").getLines().mkString("\n")

    val profile = getProfile("DefaultProfile.json")
    var hl7Util = new HL7ParseUtils(testMsg, profile, true)
    //declare PATH to query
    val PATH = "OBR[2]->OBX"

    //query message
    val matchfound = hl7Util.getValue(PATH)

    //show rresults
    printResults(matchfound)
  }


  private def printResults(resultSet: Option[Array[Array[String]]]) = {
    println(s"results ")
    if (resultSet.isDefined) {
      resultSet.get foreach {
        v => v.foreach(f => println(s"\t--> $f"))
      }
    }
    println("---")
  }

  private def getProfile(fileName: String): Profile = {
    val profileFile = Source.fromResource(fileName).getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(profileFile, classOf[Profile])

  }

}

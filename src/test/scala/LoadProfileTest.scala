import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.gson.{JsonObject, JsonParser}
import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.{Profile, SegmentConfig}
<<<<<<< HEAD

import org.scalatest.flatspec.AnyFlatSpec
=======
import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.FlatSpec
>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d

import scala.io.Source

class LoadProfileTest extends AnyFlatSpec {

  "Profile" should "be loaded" in {
    val content: String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    val mapper: ObjectMapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val profile: Profile = mapper.readValue(content, classOf[Profile])
    //val profile = scala.util.parsing.json.JSON.parseFull(content)
    print(profile)
  }


  "Segment" should "be validated" in {
    val content: String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    val mapper: ObjectMapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val profile: Profile = mapper.readValue(content, classOf[Profile])
  }

  "PhinGuide" should "be loaded" in {
    val content = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val profile = mapper.readValue(content, classOf[Profile])

    println(profile)
  }

  "HL7Hierachy" should "be loaded" in {
    val profileFile = Source.fromFile("src/test/resources/BasicProfile.json").getLines().mkString("\n")
//    val mapper = new ObjectMapper()
//    mapper.registerModule(DefaultScalaModule)
//    val profile = mapper.readValue(profileFile, classOf[Profile])
    val message = Source.fromFile("src/test/resources/DHQP_SPM_OTH_SECOND.hl7").mkString

    val parser = HL7HierarchyParser.parseMessageHierarchyFromJson(message, profileFile)
//    val output = parser.parseMessageHierarchy()

    println(parser)
  }

  "HL7Hierarchy" should "be loaded iwth default profile" in {
    val message = Source.fromResource("covidMsg.hl7").mkString

    val parser = HL7HierarchyParser.parseMessageHierarchy(message, null)
//    val output = parser.parseMessageHierarchy()

    println(parser)
  }

  "Profile" should "be created with Factory" in {
    val content =  Source.fromResource("COVID_ORC.json").getLines().mkString("\n")
    //val profile = ProfileFactory(content)
    val profileJson = JsonParser.parseString(content).getAsJsonObject()
    val profile = new Profile()
    profile.segmentDefinition = processSegmentDefinition(profileJson.get("segmentDefinition").getAsJsonObject())

    println(profile)
  }

  def processSegmentDefinition(segments: JsonObject): scala.collection.mutable.Map[String, SegmentConfig] = {
    val segMap: scala.collection.mutable.Map[String, SegmentConfig] = scala.collection.mutable.Map()
    segments.entrySet().forEach { it => {
      val seg = new SegmentConfig()
      seg.cardinality = it.getValue().getAsJsonObject.get("cardinality").toString()
      if (it.getValue().getAsJsonObject().get("children") != null)
        seg.children = processSegmentDefinition(it.getValue().getAsJsonObject.get("children").getAsJsonObject)
      segMap += it.getKey -> seg
    }
    }
    return segMap
  }

}


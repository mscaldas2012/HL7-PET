import java.util.NoSuchElementException

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.overrides.MutableList
import com.google.gson.{JsonObject, JsonParser}
import open.HL7PET.tools.model.ProfileFactory.processSegmentDefinition
import open.HL7PET.tools.{HL7HieararchyParser, HL7ParseError}
import open.HL7PET.tools.model.{Profile, ProfileFactory, SegmentConfig}
import org.scalatest.FlatSpec

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

class LoadProfileTest extends FlatSpec {

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
    val profileFile = Source.fromResource("COVID_ORC.json").getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val profile = mapper.readValue(profileFile, classOf[Profile])
    val message = scala.io.Source.fromFile("src/test/resources/covidMsg.hl7").mkString

    val parser = new HL7HieararchyParser(message, profile)
    val output = parser.parseMessageHierarchy()

    println(output)
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


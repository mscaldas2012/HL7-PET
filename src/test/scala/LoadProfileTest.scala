import java.util.NoSuchElementException

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.deser.overrides.MutableList
import open.HL7PET.tools.{HL7ParseError, HL7HieararchyParser}
import open.HL7PET.tools.model.{Profile, SegmentConfig}
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
    val profileFile = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val profile = mapper.readValue(profileFile, classOf[Profile])
    val message = scala.io.Source.fromFile("src/test/resources/ARLN_GC_DUB.hl7").mkString

    val parser = new HL7HieararchyParser(message, profile)
    val output = parser.parseMessageHierarchy()

    println(output)
  }

}


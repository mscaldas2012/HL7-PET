import open.HL7PET.tools.model.Profile
import org.scalatest.FlatSpec
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.annotate._

import scala.io.Source

class LoadProfileTest extends FlatSpec {

  "Profile" should "be loaded" in {
    val content:String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    val mapper:ObjectMapper = new ObjectMapper()
    val profile: Profile = mapper.readValue(content, classOf[Profile])
    //val profile = scala.util.parsing.json.JSON.parseFull(content)
    print(profile)
  }


  "Segment" should "be validated" in {
    val content:String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    val mapper:ObjectMapper = new ObjectMapper()
    val profile: Profile = mapper.readValue(content, classOf[Profile])
  }

  "PhinGuide" should "be loaded" in {
    val content = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")
    val mapper = new ObjectMapper()
    val profile = mapper.readValue(content, classOf[Profile])
    println(profile)
  }
}

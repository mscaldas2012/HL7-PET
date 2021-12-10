package open.HL7PET.tools

import java.util.NoSuchElementException

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import open.HL7PET.tools.model.{HL7Hierarchy, Profile, SegmentConfig}

import scala.io.Source

class HL7HieararchyParserBackup(message: String, var profile: Profile) {

  val NEW_LINE_FEED = "\\\r?\\\n"

  if (profile == null) {
    println("Using Default profile")
    val content: String = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")
    val mapper: ObjectMapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    profile = mapper.readValue(content, classOf[Profile])
  }

  def parseMessageHierarchy(): HL7Hierarchy = {
    var profilePointer = profile.segmentDefinition("MSH")
    val root_output: HL7Hierarchy = new HL7Hierarchy(0, "root")
    var output: HL7Hierarchy = root_output
    var stackProfile = scala.collection.mutable.Stack[SegmentConfig]()

    val rootProfile = new SegmentConfig()
//    rootProfile.children.addAll(profile.segmentDefinition)
    rootProfile.children ++= profile.segmentDefinition
    rootProfile.cardinality="[1..1]"
    stackProfile.push(rootProfile) //add root

    var stackOutput = scala.collection.mutable.Stack[HL7Hierarchy]()
    stackOutput.push(root_output)
    message.split(NEW_LINE_FEED).zipWithIndex.foreach {
      case (line, index) if index == 0 => { //Initialize with MSH
        output = new HL7Hierarchy(index+1, line)
//        root_output.children.addOne(output)
        root_output.children += output
        //output = root_output
      }
      case (line, index) if index > 0 => { //Process the rest of the file...
        //util.control.Breaks.breakable {
          var lineProcessed = false
          val segment = line.substring(0, 3)
        // Save State if Segment is bad:
          val stackOutputbackup = stackOutput
          val stackProfileackup = stackProfile
          val profilePointerBackup = profilePointer
          val outputBackup = output
          do {

              try {
                  val found = profilePointer.children(segment) //Found it as child..
                  val newSeg = new HL7Hierarchy(index + 1, line)
                  stackOutput.push(output)
                  stackProfile.push(profilePointer)
                  output.children += newSeg
                  profilePointer = found
                  output = newSeg
                  lineProcessed = true
              } catch {
                  case e: NoSuchElementException => { //not children...
                      if (stackProfile.isEmpty) { // Segment is not recognized as child of current segment.
                          //throw new HL7ParseError("Unable to parse message hierarchy", segment)
                          println("do nothing")
                          //ignore segment and keep going...
                          //util.control.Breaks.break;
                          lineProcessed = true
                          stackOutput = stackOutputbackup
                          stackProfile = stackProfileackup
                          profilePointer = profilePointerBackup
                          output =  outputBackup
                      } else {
                          profilePointer = stackProfile.pop() //Back Profile Pointer and try again
                          output = stackOutput.pop()
                          if (profilePointer == null)
                            println("error! Invalid message")
                      }
                  }
            }

          } while (!lineProcessed)
        }
      //}
    }


    return root_output
  }
}



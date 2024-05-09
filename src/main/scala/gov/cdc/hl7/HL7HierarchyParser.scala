package gov.cdc.hl7

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.gson.Gson
import gov.cdc.hl7.model.{HL7Hierarchy, Profile, SegmentConfig}

object HL7HierarchyParser {
  val gson = new Gson()
  val NEW_LINE_FEED = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
  //
  //  if (profile == null) {
  ////    println("Using Default profile")
  //    val content: String = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")
  //    val mapper: ObjectMapper = new ObjectMapper()
  //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  //    mapper.registerModule(DefaultScalaModule)
  //
  //    profile = mapper.readValue(content, classOf[Profile])
  //  }

  def parseMessageHierarchy(message: String, profile: Profile): HL7Hierarchy = {
    var profilePointer = profile.segmentDefinition("MSH")
    var root_output: HL7Hierarchy = new HL7Hierarchy(0, "root")
    var output: HL7Hierarchy = root_output
    val stackProfile = scala.collection.mutable.Stack[SegmentConfig]()

    val rootProfile = new SegmentConfig()
    //    rootProfile.children.addAll(profile.segmentDefinition)
    rootProfile.children ++= profile.segmentDefinition
    rootProfile.cardinality = "[1..1]"

    stackProfile.push(rootProfile) //add root

    val stackOutput = scala.collection.mutable.Stack[HL7Hierarchy]()
    stackOutput.push(root_output)

    message.split(NEW_LINE_FEED).filter { it => !it.isBlank }.zipWithIndex.foreach {
      //case (line) if line._1.isBlank() => {} //ignore
      case (line, index) if index == 0 => { //Initialize with MSH
        output = new HL7Hierarchy(index + 1, line)
        //        root_output.children.addOne(output)
        root_output.children += output
        //output = root_output

      }
      case (line, index) if index > 0 => { //Process the rest of the file...
        var lineProcessed = false;
        val segment = line.substring(0, 3)
        //backup state in case segment is not recognized...
        val profilePointerBackup = profilePointer
        val outputBackup = output
        val stackProfileBackup = scala.collection.mutable.Stack[SegmentConfig]()
        val stackOutputBackup = scala.collection.mutable.Stack[HL7Hierarchy]()
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
//                println("Unable to process segment " + segment)
                //Ingore Segment and go back to where we were...
                lineProcessed = true
                while (stackProfileBackup.nonEmpty) {
                  stackProfile.push(stackProfileBackup.pop())
                }
                while (stackOutputBackup.nonEmpty) {
                  stackOutput.push(stackOutputBackup.pop())
                }
                profilePointer = profilePointerBackup
                output = outputBackup
              } else { //Try next segment on stacks
                profilePointer = stackProfile.pop() //Back Profile Pointer and try again
                stackProfileBackup.push(profilePointer) //store profile on backup in case we can't fit the segment
                output = stackOutput.pop() //same for the output...
                stackOutputBackup.push(output)
                if (profilePointer == null) //should never happen...
                  println("error! Invalid message")
              }
            }
          }
        } while (!lineProcessed)

      }
    }
    return root_output
  }

  def parseMessageHierarchyFromJson(message: String, profile: String): HL7Hierarchy = {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val profileObj:Profile = mapper.readValue(profile, classOf[Profile])
     return parseMessageHierarchy(message, profileObj)
  }
}

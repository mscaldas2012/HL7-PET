package open.HL7PET.tools

import java.text.ParseException

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import open.HL7PET.tools.model.{HL7SegmentField, Profile}
//import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import com.fasterxml.jackson.databind.DeserializationFeature

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Try

class BatchValidator(message: String, var profile: Profile, var buildHierarchy: Boolean = false  ) {
  val FILE_HEADER_SEGMENT = "FHS"
  val FILE_TRAILER_SEGMENT = "FTS"
  val BATCH_HEADER_SEGMENT = "BHS"
  val BATCH_TRAILER_SEGMENT = "BTS"


  val NEW_LINE_FEED = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"

  val mapper:ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.registerModule(DefaultScalaModule)

  if (profile == null) {
    println("Using Default profile for batch...")
    val content:String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    profile = mapper.readValue(content, classOf[Profile])
  }


  val parser: HL7ParseUtils = new HL7ParseUtils(message, profile, buildHierarchy )
  val structureValidator: StructureValidator = new StructureValidator(message, profile, null)

  val fieldDefContent = Source.fromResource("DefaultFieldsProfile.json").getLines().mkString("\n")
  val fieldDefinitions:Profile = mapper.readValue(fieldDefContent, classOf[Profile])





  @throws(classOf[HL7ParseError])
  def validateBatchingInfo(): ValidationErrors = {
    var errors = new ValidationErrors()
    var hasBatchSegments = scala.collection.mutable.Map[String, Int]()

    hasBatchSegments += FILE_HEADER_SEGMENT -> parser.peek(FILE_HEADER_SEGMENT)
    hasBatchSegments += FILE_TRAILER_SEGMENT -> parser.peek(FILE_TRAILER_SEGMENT)
    hasBatchSegments += BATCH_HEADER_SEGMENT -> parser.peek(BATCH_HEADER_SEGMENT)
    hasBatchSegments += BATCH_TRAILER_SEGMENT -> parser.peek(BATCH_TRAILER_SEGMENT)

    val nbrOfMessages = parser.peek(parser.MSH_SEGMENT)
    val nbrOfLines = message.split(NEW_LINE_FEED).length

    //Either need all four segments (FHS, BHS, BTS, FTS) or None at all:
    if (hasBatchSegments.foldLeft(0)(_+_._2) > 0) {
      for ((seg, count) <- hasBatchSegments) {
        if (count == 0) {
          val entry = new ErrorEntry(1, 1, 1, seg+"[1]" , ERROR, "INVALID_BATCH_SEGMENTS")
          entry.description = "If sending batched messages, all 4 segments - FHS, BHS, BTS and FTS - must be included."
          errors.addEntry(entry)
        }
      }
    }

    hasBatchSegments(FILE_HEADER_SEGMENT) match {
      case 0 => //Not present - Make sure FTS is not present either:
        if (hasBatchSegments(FILE_TRAILER_SEGMENT) > 0) {
          val (line, seg) = parser.retrieveFirstSegmentOf(FILE_TRAILER_SEGMENT)
          val loc = structureValidator.getFieldLocation(seg, seg.length, 1)
          val entry = new ErrorEntry(line, 1, loc._2, FILE_TRAILER_SEGMENT+"[1]", ERROR, "INVALID_BATCH_SEGMENTS")
          entry.description = "FTS segment present without a pairing FHS segment."
        }
      case 1 => //Best result , one FHS is good, but optional..
        validateFHS(errors) // If FHS is present, it must be valid.
        //FTS should be present and valid (if not present, issue an warning)
        validateFTS(errors, hasBatchSegments(FILE_TRAILER_SEGMENT), nbrOfLines)
      case _ => //Invalid - Can only have one FHS
        populateDuplicateErrors(errors, FILE_HEADER_SEGMENT)
        validateFTS(errors, hasBatchSegments(FILE_TRAILER_SEGMENT), nbrOfLines)
    }

    hasBatchSegments(BATCH_HEADER_SEGMENT) match {
      case 0 =>
        //NOT preent - Make sure BTS is not present either:
        if (hasBatchSegments(BATCH_TRAILER_SEGMENT) > 0) {
          val (line, seg) = parser.retrieveFirstSegmentOf(BATCH_TRAILER_SEGMENT)
          val loc = structureValidator.getFieldLocation(seg, seg.length, 1)
          val entry = new ErrorEntry(line, 1, loc._2, BATCH_TRAILER_SEGMENT+"[1]", ERROR, "INVALID_BATCH_SEGMENTS")
          entry.description = "BTS segment present without a pairing BHS segment."
        }
      case 1 => //Best scenario, one BHS is good, and mandatory if multiple messages present. Optional for a single message.
        validateBHS(errors) // If BHS is present, it must be valid.
        //BTS must be present and valid
        validateBTS(errors, nbrOfLines, nbrOfMessages)
      case _ =>  //Invalid - can only have on BHS
        populateDuplicateErrors(errors, BATCH_HEADER_SEGMENT)
        validateBTS(errors, nbrOfLines, nbrOfMessages)
    }
    //Make sure there's a message in the payload.
    if (nbrOfMessages == 0) {
      val entry = new ErrorEntry(1, 1, 1, "MSH[1]", ERROR, "INVALID_MESSAGE")
      entry.description = "At least one Message is required to be present on the payload."
      errors.addEntry(entry)
    }
    errors
  }

  private def populateDuplicateErrors(errors: ValidationErrors, segment: String): Unit = {
    val ftsSegs = parser.retrieveMultipleSegments(segment)
    var index = 1
    for ((line, value) <- ftsSegs) {
      val entry = new ErrorEntry(line, 1, segment.length, segment+"[" + index + "]", ERROR, "DUPLICATE_SEGMENT")
      entry.description = s"Duplicate $segment found."
      errors.addEntry(entry)
      index += 1
    }
  }

  private def validateFHS(errors: ValidationErrors): Unit = {
    val (line, fhsFields) = parser.retrieveSegment(FILE_HEADER_SEGMENT)
    if (line != 1) {
      val loc = structureValidator.getFieldLocation(fhsFields, fhsFields.length,1)
      val entry = new ErrorEntry(line, 1, loc._2, FILE_HEADER_SEGMENT+"[1]", ERROR, "INVALID_FILE_HEADER_SEGMENT")
      entry.description = "FHS must be the first line of the message."
      errors.addEntry(entry)
    }
    structureValidator.validateSegment("FHS[1]", profile.getSegmentField("FHS"), fhsFields, 1, 1, errors)
  }

  private def validateFTS(errors: ValidationErrors, hasFTS: Int, nbrOfLines: Int): Unit = {
    hasFTS match {
      case 0 => //Error, since FHS is present
        var entry = new ErrorEntry(1, 1, 1, FILE_TRAILER_SEGMENT+"[1]", ERROR, "INVALID_BATCH_SEGMENT")
        entry.description = "Missing File Trailer Segment (FTS), when FHS is present."
        errors.addEntry(entry)
      case 1 => //FTS count must be 1
        val (line, fields) = parser.retrieveSegment(FILE_TRAILER_SEGMENT)
        if (line != nbrOfLines) {
          val loc = structureValidator.getFieldLocation(fields, fields.length,1 )
          val entry = new ErrorEntry(line, 1, loc._2, FILE_TRAILER_SEGMENT+"[1]", ERROR,"INVALID_BATCH_SEGMENT")
          entry.description = "FTS must be the last entry on the message"
          errors.addEntry(entry)
        }
        structureValidator.validateSegment("FTS[1]", profile.getSegmentField("FTS"), fields, line, 1, errors)
      case _ =>
        populateDuplicateErrors(errors, FILE_TRAILER_SEGMENT)
    }

  }


  private def validateBHS(errors: ValidationErrors): Unit = {
    val (line, bhsFields) = parser.retrieveSegment(BATCH_HEADER_SEGMENT)
    val hasFHS = parser.peek (FILE_HEADER_SEGMENT)
    if ((hasFHS > 0 && line != 2) || (hasFHS == 0 && line != 1)) {
      val loc = structureValidator.getFieldLocation(bhsFields, bhsFields.length,1)
      val entry = new ErrorEntry(line, 1, loc._2, BATCH_HEADER_SEGMENT+"[1]", ERROR,"INVALID_BATCH_SEGMENT")
      entry.description = "BHS must be defined before messages"
      errors.addEntry(entry)
    }
    structureValidator.validateSegment("BHS[1]", profile.getSegmentField("BHS"), bhsFields, line, 1, errors)
  }

  private def validateBTS(errors: ValidationErrors, nbrOfLines: Int, nbrOfMessages: Int): Unit = {
    val hasBTS = parser.peek(BATCH_TRAILER_SEGMENT)

    hasBTS match {
      case 0 =>
        val entry = new ErrorEntry(nbrOfLines, 1, 1, BATCH_TRAILER_SEGMENT+"[1]", ERROR,"INVALID_BATCH_SEGMENT")
        entry.description = "Missing Batch Trailer Segment"
        errors.addEntry(entry)
      case 1 =>
        //BTS count must match the number of messages:
        val (line, btsFields) = parser.retrieveSegment(BATCH_TRAILER_SEGMENT)
        val hasFTS = parser.peek(FILE_TRAILER_SEGMENT)
        if ((hasFTS > 0 && line != nbrOfLines - hasFTS) || (hasFTS == 0 && line != nbrOfLines)) {
          val loc = structureValidator.getFieldLocation(btsFields, btsFields.length,1)
          val entry = new ErrorEntry(line, 1, loc._2, BATCH_TRAILER_SEGMENT+"[1]", ERROR,"INVALID_BATCH_SEGMENT")
          entry.description = "BTS must be the last entry on the message or right before FTS if present"
          errors.addEntry(entry)
        }
        structureValidator.validateSegment("BTS[1]", profile.getSegmentField("BTS"), btsFields, line, 1, errors)

          val isNumber = Try { btsFields(1).toInt }.isSuccess
          if (isNumber && btsFields(1).toInt != nbrOfMessages) {
            val loc = structureValidator.getFieldLocation(btsFields, btsFields.length,1)

            val entry = new ErrorEntry(line, loc._1, loc._2, BATCH_TRAILER_SEGMENT + "[1]-1[1]", ERROR,"INVALID_BATCH_SEGMENT")
            entry.description = s"Batch Count does not match number of messages ($nbrOfMessages)."
            errors.addEntry(entry)
          }
      case _ =>
        populateDuplicateErrors(errors, BATCH_TRAILER_SEGMENT)
    }
  }



  def debatchMessages(): List[String] = {
    val result = new ListBuffer[String]()
    var newMessage = ""
    message.split(parser.NEW_LINE_FEED).filter { it => !it.isBlank }.foreach {
      line => line.substring(0,3).toUpperCase() match {
        case FILE_HEADER_SEGMENT | BATCH_HEADER_SEGMENT | BATCH_TRAILER_SEGMENT | FILE_TRAILER_SEGMENT =>
        //Ignore line...
        case parser.MSH_SEGMENT =>
          //persiste previous message:
          if (!newMessage.isEmpty) {
            //not first message:
            result.append(newMessage)
          }
          //Start New message:
          newMessage = line + "\n" //init message
        case _ =>
          newMessage += line + "\n" //continue building message.
      }
    }
    if (!"".equals(newMessage))
      result.append(newMessage)
    result.toList
  }
}

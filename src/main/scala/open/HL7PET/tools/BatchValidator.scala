package open.HL7PET.tools

import java.text.ParseException

import model.{HL7SegmentField, Profile}
import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Try

class BatchValidator(message: String, var profile: Profile ) {
  val FILE_HEADER_SEGMENT = "FHS"
  val FILE_TRAILER_SEGMENT = "FTS"
  val BATCH_HEADER_SEGMENT = "BHS"
  val BATCH_TRAILER_SEGMENT = "BTS"

  val parser: HL7ParseUtils = new HL7ParseUtils(message)

  val mapper:ObjectMapper = new ObjectMapper()
  mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  if (profile == null) {
    println("Using Default profile")
    val content:String = Source.fromResource("DefaultBatchingProfile.json").getLines().mkString("\n")

    profile = mapper.readValue(content, classOf[Profile])
  }

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
    val nbrOfLines = message.split("\n").length

    //Either need all four segments (FHS, BHS, BTS, FTS) or None at all:
    if (hasBatchSegments.foldLeft(0)(_+_._2) > 0) {
      for ((seg, count) <- hasBatchSegments) {
        if (count == 0) {
          val entry = new ErrorEntry(1, 1, seg+"[1]" , ERROR)
          entry.description = "If sending batched messages, all 4 segments - FHS, BHS, BTS and FTS - must be included."
          errors.addEntry(entry)
        }
      }
    }

    hasBatchSegments(FILE_HEADER_SEGMENT) match {
      case 0 => //Not present - Make sure FTS is not present either:
        if (hasBatchSegments(FILE_TRAILER_SEGMENT) > 0) {
          val (line, seg) = parser.retrieveFirstSegmentOf(FILE_TRAILER_SEGMENT)
          val entry = new ErrorEntry(line, 1, FILE_TRAILER_SEGMENT+"[1]", ERROR)
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
          val entry = new ErrorEntry(line, 1, BATCH_TRAILER_SEGMENT+"[1]", ERROR)
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
      val entry = new ErrorEntry(1, 1, "MSH[1]", ERROR)
      entry.description = "At least one Message is required to be present on the payload."
      errors.addEntry(entry)
    }
    errors
  }

  private def populateDuplicateErrors(errors: ValidationErrors, segment: String): Unit = {
    val ftsSegs = parser.retrieveMultipleSegments(segment)
    var index = 1
    for ((line, value) <- ftsSegs) {
      val entry = new ErrorEntry(line, 1, segment+"[" + index + "]", ERROR)
      entry.description = s"Duplicate $segment found."
      errors.addEntry(entry)
      index += 1
    }
  }

  private def validateFHS(errors: ValidationErrors): Unit = {
    val (line, fhsFields) = parser.retrieveSegment(FILE_HEADER_SEGMENT)
    if (line != 1) {
      val entry = new ErrorEntry(line, 1, FILE_HEADER_SEGMENT+"[1]", ERROR)
      entry.description = "FHS must be the first line of the message."
      errors.addEntry(entry)
    }
    validateSegment("FHS[1]", profile.segments("FHS"), fhsFields, 1, errors)
  }

  private def validateFTS(errors: ValidationErrors, hasFTS: Int, nbrOfLines: Int): Unit = {
    hasFTS match {
      case 0 => //Error, since FHS is present
        var entry = new ErrorEntry(1, 1, FILE_TRAILER_SEGMENT+"[1]", ERROR)
        entry.description = "Missing File Trailer Segment (FTS), when FHS is present."
        errors.addEntry(entry)
      case 1 => //FTS count must be 1
        val (line, fields) = parser.retrieveSegment(FILE_TRAILER_SEGMENT)
        if (line != nbrOfLines) {
          val entry = new ErrorEntry(line, 1, FILE_TRAILER_SEGMENT+"[1]", ERROR)
          entry.description = "FTS must be the last entry on the message"
          errors.addEntry(entry)
        }
        validateSegment("FTS[1]", profile.segments("FTS"), fields, line, errors)
      case _ =>
        populateDuplicateErrors(errors, FILE_TRAILER_SEGMENT)
    }

  }


  private def validateBHS(errors: ValidationErrors): Unit = {
    val (line, bhsFields) = parser.retrieveSegment(BATCH_HEADER_SEGMENT)
    val hasFHS = parser.peek (FILE_HEADER_SEGMENT)
    if ((hasFHS > 0 && line != 2) || (hasFHS == 0 && line != 1)) {
      val entry = new ErrorEntry(line, 1, BATCH_HEADER_SEGMENT+"[1]", ERROR)
      entry.description = "BHS must be defined before messages"
      errors.addEntry(entry)
    }
    validateSegment("BHS[1]", profile.segments("BHS"), bhsFields, line, errors)
  }

  private def validateBTS(errors: ValidationErrors, nbrOfLines: Int, nbrOfMessages: Int): Unit = {
    val hasBTS = parser.peek(BATCH_TRAILER_SEGMENT)

    hasBTS match {
      case 0 =>
        val entry = new ErrorEntry(nbrOfLines, 1, BATCH_TRAILER_SEGMENT+"[1]", ERROR)
        entry.description = "Missing Batch Trailer Segment"
        errors.addEntry(entry)
      case 1 =>
        //BTS count must match the number of messages:
        val (line, btsFields) = parser.retrieveSegment(BATCH_TRAILER_SEGMENT)
        val hasFTS = parser.peek(FILE_TRAILER_SEGMENT)
        if ((hasFTS > 0 && line != nbrOfLines - hasFTS) || (hasFTS == 0 && line != nbrOfLines)) {
          val entry = new ErrorEntry(line, 1, BATCH_TRAILER_SEGMENT+"[1]", ERROR)
          entry.description = "BTS must be the last entry on the message or right before FTS if present"
          errors.addEntry(entry)
        }
          validateSegment("BTS[1]", profile.segments("BTS"), btsFields, line, errors)

          val isNumber = Try { btsFields(1).toInt }.isSuccess
          if (isNumber && btsFields(1).toInt != nbrOfMessages) {
            val entry = new ErrorEntry(line, 2, BATCH_TRAILER_SEGMENT + "[1]-1[1]", ERROR)
            entry.description = s"Batch Count does not match number of messages ($nbrOfMessages)."
            errors.addEntry(entry)
          }
      case _ =>
        populateDuplicateErrors(errors, BATCH_TRAILER_SEGMENT)
    }
  }


  private def validateSegment(segment: String, fields: Array[HL7SegmentField], line: Array[String], lineNumber: Int, errors: ValidationErrors): Unit = {
    var pathSeparator = "-"
    if (segment.contains("-")) {
      pathSeparator = "."
    }

    for (field <- fields) {
      val fieldValue = getFieldValue(segment, field.fieldNumber, line)
      //Check Usage - R, O, RE, X
      var path = s"$segment$pathSeparator${field.fieldNumber}"
      if (pathSeparator.equals("-")) {
        path += "[1]"
      }

      if (!validateUsage(fieldValue, field.usage, segment)) {
        val entry = new ErrorEntry(lineNumber, field.fieldNumber, path, ERROR)
        entry.description = s"Required field $path (${field.name}) is missing."
        errors.addEntry(entry)

      }
      if (!Option(fieldValue).getOrElse("").isEmpty) {
        //if (field.fieldNumber != 2) {
          //Check Cardinality - [0..1], [1..1], [0..*], [1..*]
          val resultsCardinality = validateCardinality(fieldValue, field, segment)
          if (!resultsCardinality._1) {
            var fieldError = 1
            if (resultsCardinality._2.contains("Multiple"))
              fieldError = 2
            val entry = new ErrorEntry(lineNumber, field.fieldNumber, s"$segment$pathSeparator${field.fieldNumber}[$fieldError]", ERROR)
            entry.description = resultsCardinality._2
            errors.addEntry(entry)
          }
        //}
        //Split multiple fileds:
        val repeats:Array[String] = if (field.fieldNumber == 2) Array(fieldValue) else fieldValue.split(parser.HL7_FIELD_REPETITION)
        var answerIndex = 1
        for (aField <- repeats) {
          var innerpath = s"$segment$pathSeparator${field.fieldNumber}"
          if (pathSeparator.equals("-")) {
            innerpath += s"[$answerIndex]"
          }
          //check Data Type:
          val resultsDataType = validateFieldType(aField, field, segment, answerIndex, lineNumber, errors)
          if (!resultsDataType._1) {
            val entry = new ErrorEntry(lineNumber, field.fieldNumber, innerpath, ERROR)
            entry.description = resultsDataType._2
            errors.addEntry(entry)
          }
          //Check Max Length:
          if (aField.length > field.maxLength) {
            val entry = new ErrorEntry(lineNumber, field.fieldNumber, innerpath, ERROR)
            entry.description = s"Field value too big for $innerpath (${field.name}). Maximum value allowed is ${field.maxLength}"
            errors.addEntry(entry)
          }
          //Check default
          if (!validateDefault(aField, field.default)) {
            val defaultVal = field.default
            val entry = new ErrorEntry(lineNumber, field.fieldNumber, innerpath, ERROR)
            entry.description = s"Value for field $innerpath (${field.name}) must be $defaultVal"
            errors.addEntry(entry)
          }
          answerIndex += 1
        }
      }
    }
    //see if there are no extra fields...
    if (fields.length < line.length) {
      val entry = new ErrorEntry(lineNumber, fields.length + 1, s"$segment", ERROR)
      entry.description = s"Too many fields provided for segment $segment."
      errors.addEntry(entry)
    }
  }

  private def getFieldValue(segment: String, fieldNumber: Int, line: Array[String]): String = {
    val headerSeg = "^[MBF]HS\\[[0-9]+\\]$"
    val subFields = "^[MBF]HS\\[[0-9]+\\]\\-.*"

    if (segment.matches(subFields)) {
      fieldNumber match {
        case x if x > line.length =>
          return ""
        case _ =>
          return line(fieldNumber - 1)
      }
    } else if (segment.matches(headerSeg)) {
      fieldNumber match {
        case x if x > line.length =>
          return ""
        case 1 =>
          return "|"
        case _ =>
          return line(fieldNumber - 1)
      }
    } else {
        fieldNumber match {
          case x if x >= line.length =>
           return  ""
          case _ =>
            return line(fieldNumber  )
        }
    }
  }

  private def validateDefault(fieldValue: String, defaultValue: String): Boolean = {
    Option(defaultValue).getOrElse("").isEmpty || defaultValue.equals(fieldValue)
  }

  @throws(classOf[HL7ParseError])
  private def validateCardinality(fieldValue: String, field: HL7SegmentField, segment: String): (Boolean, String) = {
    field.cardinality match {
      case "[0..1]" => // Optional, but only one
        if (fieldValue != "^~\\&" && fieldValue.contains("~"))
          return (false, s"Multiple values for $segment-${field.fieldNumber} (${field.name}) found. Only 0 or 1 value is allowed.")
      case "[1..1]" => //Required, need one and Only one:
        if (Option(fieldValue).getOrElse("").isEmpty )
          return (false, s"Required field $segment-[${field.fieldNumber}] (${field.name}) is missing.")
        if ( fieldValue != "^~\\&" && fieldValue.contains("~"))
          return (false, s"Multiple values for $segment-${field.fieldNumber} (${field.name}) found. Only 1 value is allowed.")
      case "[1..*]" => //Make sure at least on value is there!
        if (Option(fieldValue).getOrElse("").isEmpty )
          return (false, s"Required field $segment-[${field.fieldNumber}] (${field.name}) is missing.")
      case "[0..*]" => //Not much to validate.. anything goes :-)
        return (true, "all good")
      case _ => //Invalid Profile
        throw HL7ParseError(s"Invalid Cardinality ${field.cardinality} on Profile Definition!", segment)
    }
    return (true, "all good")
  }

  @throws(classOf[HL7ParseError])
  private def validateUsage(fieldValue: String, usage: String, segment: String): Boolean = {
    usage match {
      case "R" =>
          !Option(fieldValue).getOrElse("").isEmpty
      case "RE" | "O" | "X" =>
        true
      case _ =>
        throw HL7ParseError(s"Invalid Usage $usage on Profile Definition!", segment)
    }
  }

  private def validateFieldType(fieldValue: String, field: HL7SegmentField, segment: String, answerIndex: Int, lineNumber: Int, errors: ValidationErrors): (Boolean, String) = {
    field.dataType match {
      case "NM" =>
        try{
          fieldValue.toInt
          return (true, "All good!")
        } catch {
          case e: NumberFormatException => (false, s"Invalid value for numeric field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Number expected, received: $fieldValue")
        }
      case "TS"|"DT" =>
        try {
          val format = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
          if (format.parse(fieldValue) == null) {
            return (false, s"Invalid value for date field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Date expected as 'yyyyMMddHHmmss', received: '$fieldValue'.")
          }
          (true, "all good")
        } catch {
          case e: ParseException =>
            return (false, s"Invalid value for date field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Date expected as 'yyyyMMddHHmmss', received: '$fieldValue'.")
        }
      case "HD" =>
          validateSegment(s"$segment-${field.fieldNumber}[$answerIndex]", fieldDefinitions.segments("HD"), fieldValue.split(parser.HL7_COMPONENT_SEPARATOR), lineNumber, errors)
          (true, "all good")
    case _ =>
      (true, "all good")
    }
  }

  def debatchMessages(): List[String] = {
    val result = new ListBuffer[String]()
    var newMessage = ""
    message.split(parser.NEW_LINE_FEED).foreach {
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

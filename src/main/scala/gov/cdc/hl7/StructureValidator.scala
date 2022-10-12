package gov.cdc.hl7

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.model.{HL7SegmentField, Profile, SegmentConfig}

import java.text.ParseException
//import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}

import scala.io.Source

/**
  *
  *
  * @Created - 2019-02-08
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
class StructureValidator(message: String, var profile: Profile, var fieldDefinitions: Profile)  {
    val CARDINALITY_REGEX = "\\[([0-9]+)\\.\\.([0-9]+)\\]".r
    val PREDICATE_REGEX = "C\\((RE?|O|X)\\/(RE?|O|X)\\)->(!)?([0-9]+)".r


    val mapper:ObjectMapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.registerModule(DefaultScalaModule)

    if (profile == null) {
        println("Using Default profile")
        val content:String = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")

        profile = mapper.readValue(content, classOf[Profile])
    }

    if (fieldDefinitions == null) {
        val fieldDefContent = Source.fromResource("DefaultFieldsProfile.json").getLines().mkString("\n")
        fieldDefinitions = mapper.readValue(fieldDefContent, classOf[Profile])
    }

  val parser: HL7ParseUtils = new HL7ParseUtils(message, profile,false)


  private val BATCH_SEGMENTS = List("FHS", "BHS", "BTS", "FTS")

  def validateMessage(): ValidationErrors = {
      val errors:ValidationErrors = new ValidationErrors()
      val segmentIndex = scala.collection.mutable.Map[String, Int]().withDefaultValue(0)

      if (BATCH_SEGMENTS.contains(message.substring(0,3)))
        validateBatchSegments(errors)
      else
        validateFile(errors)


      //TODO::Split MSHs before validating each message so that Segment cardinality matches.
      message.split(HL7StaticParser.NEW_LINE_FEED).zipWithIndex.foreach {
        case (line, index) =>
          val segmentLine = line.split(HL7StaticParser.HL7_FIELD_SEPARATOR)
          try {
            segmentIndex(segmentLine(0)) += 1
            validateSegment(s"${segmentLine(0)}[${segmentIndex(segmentLine(0))}]", profile.getSegmentField(segmentLine(0)), segmentLine, index + 1, 1, errors)
          } catch {
            case e: NoSuchElementException =>
              val entry = new ErrorEntry(index + 1, 1, segmentLine(0).length, segmentLine(0), ERROR, "INVALID_MESSAGE")
              entry.description = s"Segment ${segmentLine(0)} not supported"
              errors.addEntry(entry)
          }
      }

      errors
  }



  def checkSegmentCardinality(segmentConfig: (String, SegmentConfig), errors: ValidationErrors) = {
    val segments = parser.retrieveMultipleSegments(segmentConfig._1)
    segmentConfig._2.cardinality match {
      case "[0..1]" => // Optional, but only one
        if (!segments.isEmpty && segments.keySet.size > 1) {
          //val error = new ErrorEntry(segments.keySet.last, 1, segments(segments.keySet.last).foldLeft(0)( (acc, kv) => acc + kv.length + 1), t._1, ERROR, "INVALID_MESSAGE")
          val error = new ErrorEntry(segments.keySet.last, 1, 3, segmentConfig._1, ERROR, "INVALID_MESSAGE")
          error.description = s"Multiple segments for ${segmentConfig._1} found. Only 0 or 1 segments is allowed."

          errors.addEntry(error)
        }
      case "[1..1]" => //Required, need one and Only one:
        if (segments.isEmpty) {
          val error = new ErrorEntry(0, 1, 0, segmentConfig._1, ERROR, "INVALID_MESSAGE")
          error.description = s"Required segment ${segmentConfig._1} missing."
          errors.addEntry(error)
        }
        else if (segments.keySet.size > 1) {
          val error = new ErrorEntry(segments.keySet.last, 1, 3, segmentConfig._1, ERROR, "INVALID_MESSAGE")
          error.description = s"Multiple segments for ${segmentConfig._1} found. Only  1 segment is allowed."
          errors.addEntry(error)
        }
      case "[1..*]" => //Make sure at least on value is there!
        if (segments.isEmpty) {
          val error = new ErrorEntry(0, 1, 0, segmentConfig._1, ERROR, "INVALID_MESSAGE")
          error.description = s"Required segment ${segmentConfig._1} missing."
          errors.addEntry(error)
        }
      case CARDINALITY_REGEX(min, max) =>
        if (segments.keySet.size < min.toInt || segments.keySet.size > max.toInt) {
          val error = new ErrorEntry(1, 1, 2, segmentConfig._1, ERROR, "INVALID_MESSAGE")
          error.description = s"Multiple segments for ${segmentConfig._1} found. Only  $min to $max segments are allowed."
          errors.addEntry(error)
        }
      case "[0..*]" => //all good
      case _ => //Invalid Profile
        throw HL7ParseError(s"Invalid Cardinality ${segmentConfig._2.cardinality} on Profile Definition!", segmentConfig._1)
    }
  }


  private def recursiveBatchValidation(seg: (String, SegmentConfig), errors: ValidationErrors): Unit = {
    checkSegmentCardinality(seg, errors)
    seg._2.children.filter { it => BATCH_SEGMENTS.contains(it._1)}.foreach { it =>
      recursiveBatchValidation(it, errors)
    }
  }

  private def validateBatchSegments(errors: ValidationErrors) = {
    profile.segmentDefinition.filter { it => BATCH_SEGMENTS.contains(it._1)}.foreach { it =>
      recursiveBatchValidation(it, errors);
    }
  }


  private def recursiveValidation(seg: (String, SegmentConfig), errors: ValidationErrors): Unit = {
    checkSegmentCardinality(seg, errors)
    seg._2.children.foreach { it =>
      recursiveValidation(it, errors)
    }
  }
  private def validateFile(errors: ValidationErrors) = {
    profile.segmentDefinition.foreach { it =>
      recursiveValidation(it, errors)
    }
  }

    def getFieldLocation(line: Array[String], fieldNumber: Int, columnsBefore: Int): (Int, Int) = {
        var beginCol = columnsBefore // where to start counting columns  for recursive calls.
        for (idx <- 0 until fieldNumber) {
            if (idx < line.length)
                beginCol = beginCol + line(idx).length + 1 // +1 is the field delimiter, aka, the pipe ( \ )
        }
        if (fieldNumber < line.length)
            (beginCol, beginCol  + line(fieldNumber).length + 1)
        else //Field is missing.
            (beginCol, beginCol + 1)
    }
     def validateSegment(segment: String, fields: Array[HL7SegmentField], line: Array[String], lineNumber: Int, columnsBefore: Int, errors: ValidationErrors): Unit = {
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

            try {
                if (!validateUsage(fieldValue, field.usage, segment, line)) {
                    val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)
                    val entry = new ErrorEntry(lineNumber,loc._1, loc._2, path, ERROR, "INVALID_USAGE")
                    entry.description = s"Required field $path (${field.name}) is missing."
                    errors.addEntry(entry)

                }
            } catch {
                case e: HL7ParseError =>
                    val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)
                    val entry = new ErrorEntry(lineNumber, loc._1, loc._2, path, ERROR, "INVALID_SEGMENT")
                    entry.description = e.getMessage
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
                    val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)

                    val entry = new ErrorEntry(lineNumber, loc._1, loc._2, s"$segment$pathSeparator${field.fieldNumber}[$fieldError]", ERROR, "INVALID_CARDINALITY")
                    entry.description = resultsCardinality._2
                    errors.addEntry(entry)
                }
                //}
                //Split multiple fileds:
                val repeats:Array[String] = if (field.fieldNumber == 2) Array(fieldValue) else fieldValue.split(HL7StaticParser.HL7_FIELD_REPETITION)
                var answerIndex = 1
                for (aField <- repeats) {
                    var innerpath = s"$segment$pathSeparator${field.fieldNumber}"
                    if (pathSeparator.equals("-")) {
                        innerpath += s"[$answerIndex]"
                    }
                    //check Data Type:
                    var loc =
                        if ( segment.startsWith("MSH"))
                            getFieldLocation(line, field.fieldNumber-1, columnsBefore)
                        else
                            getFieldLocation(line, field.fieldNumber, columnsBefore)
                    val resultsDataType = validateFieldType(aField, field, segment, answerIndex, lineNumber, loc._1, errors)
                    if (!resultsDataType._1) {
                        val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)
                        val entry = new ErrorEntry(lineNumber, loc._1, loc._2, innerpath, ERROR, "INVALID_FIELD_TYPE")
                        entry.description = resultsDataType._2
                        errors.addEntry(entry)
                    }
                    //Check Max Length:
                    if (aField.length > field.maxLength) {
                        val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)
                        val entry = new ErrorEntry(lineNumber, loc._1, loc._2, innerpath, ERROR, "INVALID_FIELD_LENGTH")
                        entry.description = s"Field value too big for $innerpath (${field.name}). Maximum value allowed is ${field.maxLength}"
                        errors.addEntry(entry)
                    }
                    //Check default
                    if (!validateDefault(aField, field.default)) {
                        val defaultVal = field.default
                        val loc = getFieldLocation(line, field.fieldNumber, columnsBefore)
                        val entry = new ErrorEntry(lineNumber, loc._1, loc._2, innerpath, ERROR, "INVALID_DEFAULT_VALUE")
                        entry.description = s"Value for field $innerpath (${field.name}) must be $defaultVal"
                        errors.addEntry(entry)
                    }
                    answerIndex += 1
                }
            }
        }
        //see if there are no extra fields...
        if (fields.length < line.length) {
            val loc = getFieldLocation(line, fields.length, columnsBefore)
            val entry = new ErrorEntry(lineNumber, loc._1, loc._2, s"$segment", ERROR, "INVALID_SEGMENT")
            entry.description = s"Too many fields provided for segment $segment."
            errors.addEntry(entry)
        }
    }

    private def getFieldValue(segment: String, fieldNumber: Int, line: Array[String]): String = {
        val headerSeg = "^(MSH|[BF]HS)\\[[0-9]+\\]$"
        val subFields = "^([A-Z]{3})\\[[0-9]+\\]\\-.*"

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
            case  CARDINALITY_REGEX(min, max) =>
                //TODO::Implement
                    println(s"validating card... $min to $max")
            case _ => //Invalid Profile
                throw HL7ParseError(s"Invalid Cardinality ${field.cardinality} on Profile Definition!", segment)
        }
        return (true, "all good")
    }

    @throws(classOf[HL7ParseError])
    private def validateUsage(fieldValue: String, usage: String, segment: String, line: Array[String]): Boolean = {
        usage match {
            case "R" =>
                !Option(fieldValue).getOrElse("").isEmpty
            case "RE" | "O" | "X" =>
                true
            case PREDICATE_REGEX(iftrue, iffalse, absent, field) =>
                val relatedField =  getFieldValue(segment, field.toInt, line)

                val checkFirstCond =  (absent == null && relatedField != null && relatedField.trim().length > 0) ||
                    ("!".equals(absent) && (relatedField == null || relatedField.trim().length == 0))

                if (checkFirstCond) {
                    validateUsage(fieldValue, iftrue, segment, line)
                } else {
                    validateUsage(fieldValue, iffalse, segment, line)
                }


            case _ =>
                throw HL7ParseError(s"Invalid Usage $usage on Profile Definition!", segment)
        }
    }

    private def validateFieldType(fieldValue: String, field: HL7SegmentField, segment: String, answerIndex: Int, lineNumber: Int, columnsBefore: Int,  errors: ValidationErrors): (Boolean, String) = {
        field.dataType match {
            case "ST"|"IS"|"ID"|"TX"|"FT" =>
                return (true, "All good!")
            case "NM" =>
                try{
                    fieldValue.toInt
                    return (true, "All good!")
                } catch {
                    case e: NumberFormatException => (false, s"Invalid value for numeric field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Number expected, received: $fieldValue")
                }
            case "TS"|"DT" =>
                try {
                    val format = new java.text.SimpleDateFormat(if (fieldValue.size == 8) "yyyyMMdd" else "yyyyMMddHHmmss")
                    if (format.parse(fieldValue) == null) {
                        return (false, s"Invalid value for date field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Date expected as 'yyyyMMddHHmmss', received: '$fieldValue'.")
                    }
                    (true, "all good")
                } catch {
                    case e: ParseException =>
                        return (false, s"Invalid value for date field $segment-${field.fieldNumber}[$answerIndex] (${field.name}). Date expected as 'yyyyMMddHHmmss', received: '$fieldValue'.")
                }
//            case "DTM" =>
//              try {
//
//              }
            case x =>
                try {
                  //MSH[1] -> MSH[1]-fieldNumber[answerIndex] and separate component.
                  //MSH[1]-F[1] -> MSH[1]-F[1].1 and separate subcomponent:
                  val newseg: String =
                  if (segment.indexOf('-') < 0)
                    s"$segment-${field.fieldNumber}[$answerIndex]"
                  else
                    s"$segment.${field.fieldNumber}"
                    var split =
                        if (newseg.indexOf("].") > 0)
                            fieldValue.split(HL7StaticParser.HL7_SUBCOMPONENT_SEPARATOR)
                        else
                            fieldValue.split(HL7StaticParser.HL7_COMPONENT_SEPARATOR)

                    validateSegment(newseg, fieldDefinitions.getSegmentField(x), split, lineNumber, columnsBefore, errors)
                    (true, "all good")
                } catch {
                    case e: NoSuchElementException =>
                        (false,s"Field type '$x' is not supported.")

                }
            case _ =>
                (true, "all good")
        }
    }

}

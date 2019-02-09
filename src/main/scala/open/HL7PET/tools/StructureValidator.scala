package open.HL7PET.tools

import java.text.ParseException
import java.util.NoSuchElementException

import model.{HL7SegmentField, Profile}
import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}

import scala.io.Source

/**
  *
  *
  * @Created - 2019-02-08
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
class StructureValidator(message: String, var profile: Profile, var fieldDefinitions: Profile)  {
    val CARDINALITY_REGEX = "\\[([0-9]+)\\.\\.([0-9]+)\\]".r

    val parser: HL7ParseUtils = new HL7ParseUtils(message)

    val mapper:ObjectMapper = new ObjectMapper()
    mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    if (profile == null) {
        println("Using Default profile")
        val content:String = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")

        profile = mapper.readValue(content, classOf[Profile])
    }

    if (fieldDefinitions == null) {
        val fieldDefContent = Source.fromResource("DefaultFieldsProfile.json").getLines().mkString("\n")
        fieldDefinitions = mapper.readValue(fieldDefContent, classOf[Profile])
    }


    def validateMessage(): ValidationErrors = {
        val errors:ValidationErrors = new ValidationErrors()
        message.split(parser.NEW_LINE_FEED).zipWithIndex.foreach {
            case (line, index) =>
                var segmentLine = line.split(parser.HL7_FIELD_SEPARATOR)
                try {
                    validateSegment(s"${segmentLine(0)}[$index]", profile.segments(segmentLine(0)), segmentLine, index + 1, errors)
                } catch {
                    case e: NoSuchElementException =>
                    val entry = new ErrorEntry(index, 0, segmentLine(0), ERROR)
                        entry.description = s"Segment ${segmentLine(0)} not supported"
                        errors.addEntry(entry)
                }
        }
        errors
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

            try {
                if (!validateUsage(fieldValue, field.usage, segment)) {
                    val entry = new ErrorEntry(lineNumber, field.fieldNumber, path, ERROR)
                    entry.description = s"Required field $path (${field.name}) is missing."
                    errors.addEntry(entry)

                }
            } catch {
                case e: HL7ParseError =>
                    val entry = new ErrorEntry(lineNumber, field.fieldNumber, path, ERROR)
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
                    println(s"validating card... $min to $max")
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
            case "ST"|"IS"|"ID" =>
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
            case x =>
                try {
                    //MSH[1] -> MSH[1]-fieldNumber[answerIndex] and separate component.
                    //MSH[1]-F[1] -> MSH[1]-F[1].1 and separate subcomponent:
                    var newseg: String =
                        if (segment.indexOf('-') < 0)
                            s"$segment-${field.fieldNumber}[$answerIndex]"
                         else
                            s"$segment.${field.fieldNumber}"
                    var split =
                        if (newseg.indexOf("].") > 0)
                            fieldValue.split(parser.HL7_SUBCOMPONENT_SEPARATOR)
                        else
                            fieldValue.split(parser.HL7_COMPONENT_SEPARATOR)
                    validateSegment(newseg, fieldDefinitions.segments(x), split, lineNumber, errors)
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

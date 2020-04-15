package open.HL7PET.tools

import com.fasterxml.jackson.databind.{DeserializationConfig, ObjectMapper}
import open.HL7PET.tools.model.ValidationRules
//import org.codehaus.jackson.map.{DeserializationConfig, ObjectMapper}
import com.fasterxml.jackson.databind.DeserializationFeature

import scala.io.Source

class RulesValidator(rulesFile: String) {
  val rules = {
    val content:String = Source.fromResource(rulesFile).getLines().mkString("\n")
    val mapper:ObjectMapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.readValue(content, classOf[ValidationRules])
  }

  def validatePredicate(message: String): ValidationErrors = {
    val parser: HL7ParseUtils = new HL7ParseUtils(message, null, false)
    val errors = new ValidationErrors()
    rules.rules("predicates").foreach { e =>
      e.name match {
        case "requiredIfEmpty" =>
          val segmentValues = parser.retrieveMultipleSegments(e.segment)
          for (((k, seg), i) <- segmentValues.zipWithIndex) {
            val reference = parser.getValue(e.reference, seg, true)
            val parentField = e.reference.substring(0, e.reference.lastIndexOf("."))
            val parentFieldValue = parser.getValue(parentField, seg, true)
            if ( parentFieldValue.isDefined && reference.isEmpty) {
              val field = parser.getValue(e.field, seg, true)
              if (field.isEmpty) { //Error:
                val classification = if ("R".equals(e.usage)) ERROR else WARNING
                val entry = new ErrorEntry(k, 0, 1, e.comment.replaceAll("\\*", (i+1) + ""), classification, "INVALID_PREDICATE")
                entry.description = e.description
                errors.addEntry(entry)
              }
            }
          }
        case "requiredIfNotEmpty" =>
          val segmentValues = parser.retrieveMultipleSegments(e.segment)
          for (((k, seg), i) <- segmentValues.zipWithIndex) {
            val reference = parser.getValue(e.reference, seg, true)
              if (!reference.isEmpty) {
                val field = parser.getValue(e.field, seg, true)
                if (field.isEmpty) { //Error
                  val classification = if ("R".equals(e.usage)) ERROR else WARNING
                  val entry = new ErrorEntry(k, 0, 1, e.comment.replaceAll("\\*", (i + 1) + ""), classification, "INVALID_PREDICATE")
                  entry.description = e.description
                  errors.addEntry(entry)
                }
              }
          }
        case _ => println("Unknown Rule...")
      }

    }
    errors
  }

  def validateConformance(message: String): ValidationErrors = {
    val parser: HL7ParseUtils = new HL7ParseUtils(message, null, false)
    val errors = new ValidationErrors()
    rules.rules("conformance").foreach { e =>
      e.name match {
        case "regEx" =>
          val segmentValues = parser.retrieveMultipleSegments(e.segment)
          for (((k, seg), i) <- segmentValues.zipWithIndex) {
            val fieldValue = parser.getValue(e.field, seg, true)
            var allmatch = true
            if (fieldValue.isDefined) {
              fieldValue.get.foreach(f => allmatch = allmatch && f.matches(e.reference))
            }
            if (!allmatch) { //
              val classification = if ("R".equals(e.usage)) ERROR else WARNING
              val entry = new ErrorEntry(k, 0, 1, e.comment.replaceAll("\\*", (i + 1) + ""), classification, "INVALID_CONFORMANCE")
              entry.description = e.description
              errors.addEntry(entry)
            }
          }
        case "constant" =>
              val segmentValues = parser.retrieveMultipleSegments(e.segment)
              for (((k, seg), i) <- segmentValues.zipWithIndex) {
                val fieldValue = parser.getValue(e.field, seg, true)
                var allmatch = true
                if (fieldValue.isDefined ) {
                  fieldValue.get.foreach( f => allmatch = allmatch && f.equals(e.reference))
                }
                if (!allmatch) { //
                  val classification = if ("R".equals(e.usage)) ERROR else WARNING
                  val entry = new ErrorEntry(k, 0, 1, e.comment.replaceAll("\\*", (i + 1) + ""), classification,"INVALID_CONFORMANCE")
                  entry.description = e.description
                  errors.addEntry(entry)
                }
              }
        case "existsUnique" =>
            val fieldValue = parser.getValue(e.field)
            if (fieldValue.isEmpty || fieldValue.get.length != 1 ) {
              val classification = if ("R".equals(e.usage)) ERROR else WARNING
              val entry = new ErrorEntry(0, 0, 1, e.comment, classification, "INVALID_CONFORMANCE")
              entry.description = e.description
              errors.addEntry(entry)
            }
        case _ => println("Unknown Rule...")
      }
    }
    errors
  }
}

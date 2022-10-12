import gov.cdc.hl7.{RulesValidator, ValidationErrors}
import gov.cdc.utils.ConsoleProgress
import org.scalatest.FlatSpec

class RulesValidatorTests extends FlatSpec  {

  "PredicateRules" must "validate" in {

      val validator:RulesValidator = new RulesValidator("predicateRules.json")
      var errors:ValidationErrors = null
      ConsoleProgress.showProgress {
        errors = validator.validatePredicate(getMessage("FDD_CAMP_TC01_ADD.txt"))
      }
      println(s"found ${errors.totalErrors} errors and ${errors.totalWarnings} warnings")
      errors.getEntries().foreach(e => println(e))
  }

  "ConformanceRules" must "validate" in {
    val validator:RulesValidator = new RulesValidator("conformanceRules.json")
    var errors:ValidationErrors = null
    ConsoleProgress.showProgress {
      errors = validator.validateConformance(getMessage("FDD_CAMP_TC01_ADD.txt"))
    }
    println(s"found ${errors.totalErrors} errors and ${errors.totalWarnings} warnings")
    errors.getEntries().foreach(e => println(e))

  }

  def getMessage(filename: String): String = {
    var allLines = ""
    val source = io.Source.fromResource(filename)
    if (source != null) {
      for (line <- source.getLines) {
        allLines += line + "\n"
      }
    }
    allLines
  }

  }

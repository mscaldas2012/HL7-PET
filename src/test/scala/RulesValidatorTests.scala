import gov.cdc.hl7.{RulesValidator, ValidationErrors}
import gov.cdc.utils.ConsoleProgress
<<<<<<< HEAD

import org.scalatest.flatspec.AnyFlatSpec

=======
import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.FlatSpec

>>>>>>> 78c370b53da9a444962a2178ee2c33f169faea8d
class RulesValidatorTests extends AnyFlatSpec  {

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

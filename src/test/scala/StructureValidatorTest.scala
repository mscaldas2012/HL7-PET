import open.HL7PET.tools.{BatchValidator, StructureValidator, ValidationErrors}
import org.scalatest.FlatSpec

/**
  *
  *
  * @Created - 2019-02-08
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
class StructureValidatorTest extends FlatSpec {
    "SingleBatchedMessage" must "pass validation" in {
        val errors = processHappyPathMessage("FDD_CAMP_TC01_ADD.txt")
        assert(errors.totalErrors == 0)
        assert(errors.totalWarnings == 0)
    }

    def processHappyPathMessage(filename: String, verbose: Boolean = false): ValidationErrors = {
        var allLines = ""
        val source = io.Source.fromResource(filename)
        if (verbose) println("Source is null? " + (source == null))
        if (source != null) {
            for (line <- source.getLines) {
                if (verbose) println(s"line: $line")
                allLines += line + "\n"
            }
        }
        val validator:  StructureValidator = new StructureValidator(allLines, null, null)
        val errors = validator.validateMessage()
        println(errors)
        errors
    }
}

import gov.cdc.hl7.{BatchValidator, ValidationErrors}
import gov.cdc.hl7.model.Profile
import org.scalatest.FlatSpec


class BatchValidatorTest extends FlatSpec {

  //Happy Path:
  "SingleBatchedMessage" must "pass validation" in {
     val errors = processHappyPathMessage("FileBatchSingleMessage.hl7")
    assert(errors.totalErrors == 0)
    assert(errors.totalWarnings == 0)
  }
  "SingleBatchedMessageNoFHS" must "fail validation" in {
    val errors = processHappyPathMessage("FileBatchSingleMessageNoFHS.hl7")
    assert(errors.totalErrors > 0)
  }
  "SingleBatchedMessageNoBHS" must "pass validation" in {
    val errors = processHappyPathMessage("FileBatchSingleMessageNoBHS.hl7")
    assert(errors.totalErrors == 0)
    assert(errors.totalWarnings == 0)
  }
  "MultipleBatchedMessage" must "pass validation" in {
    val errors = processHappyPathMessage("FileBatchMultipleMessages.hl7")
    assert(errors.totalErrors == 0)
    assert(errors.totalWarnings == 0)
  }

  "InvalidBHSRepeat" must "fail validation" in {
    val errors = processHappyPathMessage("FileBatchInvalidRepeatBHS3.hl7")
    assert(errors.totalErrors == 2)
    assert(errors.totalWarnings == 0)
  }


  "MultipleBatchedMessageNoFHS" must "fail validation" in {
    val errors = processHappyPathMessage("FileBatchMultipleMessagesnoFHS.hl7")
    assert(errors.totalErrors > 0)
    assert(errors.totalWarnings == 0)
  }

    //Error Handling:
  "InvalidBTSCount" must "raise validation error" in {
    val errors = processHappyPathMessage("FileBatchInvalidBTSCount.hl7")
    assert(errors.totalErrors == 1)
  }
  "InvalidFTSCount" must "raise validation error" in {
    val errors = processHappyPathMessage("FileBatchInvalidFTSCount.hl7")
    assert(errors.totalErrors == 1)
  }
  "Missing FTS" must "raise validation error" in {
    val errors = processHappyPathMessage("FileBatchNoFTS.hl7")
    assert(errors.totalErrors >= 1)
  }

  "Multiple MSH without Batching info" must "pass validation" in {
    val errors = processHappyPathMessage("MultipleMSHWithoutBHS.hl7")
    assert(errors.totalErrors == 0)
  }

  "empty FHS" must "flag error" in {
    val errors = processHappyPathMessage("emptyFHS.hl7")
    assert(errors.totalErrors > 1)
  }

  "empty BHS" must "flag error" in {
    val errors = processHappyPathMessage("emptyBHS.hl7")
    assert(errors.totalErrors > 1)
  }

  "empty FHS|BHS" must "flag error" in {
    val errors = processHappyPathMessage("emptyFHSBHS.hl7")
    assert(errors.totalErrors == 1)
  }

  "Malformed FHS" must "flag error" in {
    val errors = processHappyPathMessage("invalidFHS.hl7",  false)
    assert(errors.totalErrors == 2)
  }

  "Malformed BHS"  must "flag error" in {
    val errors = processHappyPathMessage("invalidBHS.hl7", false)
    assert(errors.totalErrors == 4)
  }

  "Malformed FTS"  must "flag error" in {
    val errors = processHappyPathMessage("invalidFTS.hl7", false)
    assert(errors.totalErrors == 1)
  }

  "Malformed BTS"  must "flag error" in {
    val errors = processHappyPathMessage("invalidBTS.hl7", false)
    assert(errors.totalErrors == 1)
  }

  "Missing BTS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchNoBTS.hl7")
    assert(errors.totalErrors >= 1)
  }

  "Multiple FHS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleFHS.hl7")
    assert(errors.totalErrors > 1)
  }

  "Multiple FTS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleFTS.hl7")
    assert(errors.totalErrors > 1)
  }
  "Multiple BHS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleBHS.hl7")
    assert(errors.totalErrors > 1)
  }
  "Multiple BTS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleBTS.hl7")
    assert(errors.totalErrors > 1)
  }
  "MSH Before BHS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleMessagesMSHBeforeBHS.hl7")
    assert(errors.totalErrors > 0)
  }
  "MSH After BTS"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMultipleMessagesMSHAfterBTS.hl7")
    assert(errors.totalErrors > 0)
  }

  "Multiple BHS and BTS"  must "flag error" in {
    val errors = processHappyPathMessage("MultipleBHSandBTS.hl7")
    assert(errors.totalErrors > 0)
  }

  "BHS large fields"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchSingleMessageLargeFields.hl7")
    assert(errors.totalErrors > 0)
  }

  "FHS extra fields"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchExtraFHSFields.hl7")
    assert(errors.totalErrors > 0)
  }

  "MSH Missing"  must "flag error" in {
    val errors = processHappyPathMessage("FileBatchMissingMSH.hl7")
    assert(errors.totalErrors > 0)
  }

  "Two MSH" must "flag two message" in {
    val source = io.Source.fromResource("TwoMSH.hl7")
    var allLines = ""
    if (source != null) {
      for (line <- source.getLines) {
        allLines += line + "\n"
      }
    }
    val validator:  BatchValidator = new BatchValidator(allLines,null)
    val msgs = validator.debatchMessages()
    assert(msgs.size == 2)
  }

  "Batches with empty lines" must "not bomb - just ignore empyt lines" in {
    val errors = processHappyPathMessage("FileBatchEmptyLines.hl7")
    val debatcher = getBatchValidator("FileBatchEmptyLines.hl7", false)
    val msgList = debatcher.debatchMessages();
    assert (msgList.size == 4)

    assert(debatcher.parser.peek("FHS") == 1)
    assert(debatcher.parser.peek("BHS") == 1)
    assert(debatcher.parser.peek("BTS") == 1)
    assert(debatcher.parser.peek("FTS") == 1)

  }

  "Covid batch" must "pass"in {
    val debatcher = getBatchValidator("FileBatchExtraSegs.hl7", false)
    val msgList = debatcher.debatchMessages()
    assert(msgList.size == 6)

  }


  "FTS" must "be ignored" in {
    val debatcher = getBatchValidator("FileFTSONLY.hl7", true)
    val msgList = debatcher.debatchMessages()
    assert(msgList.size == 1)

  }
"test constructor" must "crdate profile" in {
  val profile = new Profile()
  println(profile)
}





  def processHappyPathMessage(filename: String, verbose: Boolean = false): ValidationErrors = {
    val validator: BatchValidator = getBatchValidator(filename, verbose)
    val errors = validator.validateBatchingInfo()
    println(errors)

    errors
  }

  private def getBatchValidator(filename: String, verbose: Boolean): BatchValidator = {
    var allLines = ""
    val source = io.Source.fromResource(filename)
    if (verbose) println("Source is null? " + (source == null))
    if (source != null) {
      for (line <- source.getLines) {
        if (verbose) println(s"line: $line")
        allLines += line + "\n"
      }
    }
    //val parser: HL7ParseUtils = new HL7ParseUtils(allLines)
    val validator: BatchValidator = new BatchValidator(allLines, null, false)
    validator
  }
}

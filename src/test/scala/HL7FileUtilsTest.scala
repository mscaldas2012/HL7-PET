import gov.cdc.hl7.HL7FileUtils

import org.scalatest.flatspec.AnyFlatSpec

/**
  *
  *
  * @Created - 6/2/17
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
class HL7FileUtilsTest extends AnyFlatSpec {
    "HL7FileUtils" should "split messages" in {
        var msgSplitter = new HL7FileUtils()
        msgSplitter.splitMessages("src/test/resources/Cancer.txt", "target/temp", "cancerMSG")
    }

    "Batch Files" should "split batches" in {
        var msgSplitter = new HL7FileUtils()
        msgSplitter.splitBatches("src/test/resources/FileBatchSplit.txt", "target/temp", "testMSG")
    }

}

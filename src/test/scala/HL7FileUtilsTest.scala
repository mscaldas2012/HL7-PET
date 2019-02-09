import open.HL7PET.tools.HL7FileUtils
import org.scalatest.FlatSpec

/**
  *
  *
  * @Created - 6/2/17
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
class HL7FileUtilsTest extends FlatSpec {
    "HL7FileUtils" should "split messages" in {
        var msgSplitter = new HL7FileUtils()
        msgSplitter.splitMessages("src/test/resources/Cancer.txt", "temp", "cancerMSG")
    }

}

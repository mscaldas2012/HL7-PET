package open.HL7PET.tools

/**
  *
  *
  * @Created - 2019-02-08
  * @Author Marcelo Caldas mcq1@cdc.gov
  */
case class HL7ParseError(private val message: String = "", segment: String,
                         private val cause: Throwable = None.orNull) extends Exception(message, cause) {

}

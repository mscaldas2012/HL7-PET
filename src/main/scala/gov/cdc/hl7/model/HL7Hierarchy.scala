package gov.cdc.hl7.model

import scala.collection.mutable.ListBuffer

/**
  * This model holds a HL7 v2 message in a hierarchical fashion.
  * The profile identifies how to parse Parent-Child relationship and a simple List is built
  * based on that knowledge.
  */
class HL7Hierarchy(val lineNbr: Int, val segment: String) {
    var children:  ListBuffer[HL7Hierarchy] = new ListBuffer[HL7Hierarchy]()
}

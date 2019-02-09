def filterValues(filter: String, segment: Array[String]): Boolean = {
    filter match {
        case FILTER_REGEX(field, _, _, comp, _, subcomp, constant) => {
            val offset = if ("MSH".equals(segment(0))) 1 else 0

            var valueToCompare = segment(field.toInt - offset )
            if (comp != null && valueToCompare != null) {
                val compSplit = valueToCompare.split(HL7_COMPONENT_SEPARATOR)
                if (compSplit.size >=  (comp.toInt )) {
                    valueToCompare = compSplit(comp.toInt-1)
                    if (subcomp != null && valueToCompare != null) {
                        val subcompSplit = valueToCompare.split(HL7_SUBCOMPONENT_SEPARATOR)
                        if (subcompSplit.size >= (subcomp.toInt )) {
                            valueToCompare = subcompSplit(subcomp.toInt - 1)
                        } else
                            return false
                        }

                } else
                    return false
            }
            return constant.equals(valueToCompare)
        }
        case _ => false

    }
    false
}

def getValue(path: String): Option[Array[Array[String]]] = {
//val EMPTY = new Array[String](0)
    path match {
        case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
            var segmentIndex = 0
            if (segIdx != null && segIdx != "*" && !segIdx.startsWith("@"))
                segmentIndex = segIdx.toInt
            var fieldIndex = 0
            if (fieldIdx != null && fieldIdx != "*" )
                fieldIndex = fieldIdx.toInt

            var segmentList = retrieveMultipleSegments(seg)
            if (segIdx != null && segIdx.startsWith("@")) {
                //Need to filter the Segments based on "filter"
                val filteredList = segmentList filter {
                    case (_,v) => filterValues(segIdx, v)
                }
                segmentList = filteredList
            }
            //User wants a specific field:
            val offset = if ("MSH".equals(seg)) 1 else 0

            var result: Array[Array[String]] = new Array[Array[String]](0)
            for (((k, segment), i) <- segmentList.zipWithIndex) {
                if (segmentIndex <= 0 || segmentIndex == (i + 1)) {
                    var finalValue = segment.mkString("|")
                    var fieldArray: Array[String] = new Array[String](0)
                    if (field != null) {
                        val fieldValue: String = segment(field.toInt - offset)
                        val fieldValueSplit = fieldValue.split(HL7_FIELD_REPETITION)
                         for ((onefield, j) <- fieldValueSplit.zipWithIndex) {
                            if ((fieldIndex <=0) || (fieldIndex == ( j+1))) {
                                finalValue = onefield
                                if (comp != null) {
                                    val compSplit = fieldValue.split(HL7_COMPONENT_SEPARATOR)
                                    if (comp.toInt > compSplit.length)
                                        finalValue = ""
                                    else
                                          finalValue = compSplit(comp.toInt - 1)

                                    if (subcomp != null) {
                                        val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
                                        if (subcomp.toInt > subCompSplit.length)
                                            finalValue = ""
                                        else
                                            finalValue = subCompSplit(subcomp.toInt - 1)
                                    }

                                }
                                fieldArray :+= finalValue
                            }
                         }
                    } else fieldArray :+= finalValue
                result :+= fieldArray
            }
            }
            //TODO::All results might be empty... better to return None if that Happens.

            if (result isEmpty)
            None
            else Option(result)
        }
        case _ => None
    }
}


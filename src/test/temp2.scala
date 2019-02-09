
private def filterValues(filter: String, segment: Array[String]): Boolean = {
  filter match {
    case FILTER_REGEX(field, _, _, comp, _, subcomp, constant) => {
      val offset = if ("MSH".equals(segment(0))) 1 else 0
      var valueToCompare = segment.lift(field.toInt - offset).getOrElse("")
      if (comp != null && !valueToCompare.isEmpty) {
        val compSplit = valueToCompare.split(HL7_COMPONENT_SEPARATOR)
        valueToCompare = compSplit.lift(comp.toInt-1).getOrElse("")
        if (subcomp != null && valueToCompare != null) {
          val subcompSplit = valueToCompare.split(HL7_SUBCOMPONENT_SEPARATOR)
          valueToCompare = subcompSplit.lift(subcomp.toInt - 1).getOrElse("")
        }
      }
      constant.equals(valueToCompare)
    }
    case _ => false
  }
}

private def getListOfMatchingSegments(seg: String, segIdx: String): scala.collection.immutable.SortedMap[Int, Array[String]] = {
  var segmentIndex = 0
  if (segIdx != null && segIdx != "*" && !segIdx.startsWith("@"))
    segmentIndex = segIdx.toInt

  var segmentList = retrieveMultipleSegments(seg) //All segments for SEG
  if (segIdx != null && segIdx.startsWith("@")) { //Filter Segments if filter is provided instead of Index...
    val filteredList = segmentList filter {
      case (_,v) => filterValues(segIdx, v)
    }
    segmentList = filteredList
  } else  if (segmentIndex > 0) { //Get the single
    segmentList = segmentList.take(segmentIndex)
  }
  segmentList
}

private def safeToInt(nbr: String, default: Int):Int = {
  try {
    nbr.toInt
  } catch {
    case _:Throwable => default
  }
}

///New code
def getValue(path: String): Option[Array[Array[String]]] = {
  //val EMPTY = new Array[String](0)
  path match {
    case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
      var segmentIndex = safeToInt(segIdx, 0)
      var fieldIndex = safeToInt(fieldIdx, 0)
      var segmentList = getListOfMatchingSegments(seg, segIdx)
      //User wants a specific field:
      val offset = if ("MSH".equals(seg)) 1 else 0
      var result: Array[Array[String]] = new Array[Array[String]](0)

      for (((k, segment), i) <- segmentList.zipWithIndex) {
        var finalValue: String = segment.mkString("|")
        var fieldArray: Array[String] = new Array[String](0)
        if (field != null) {
          val fieldValue: String = segment.lift(field.toInt - offset).getOrElse("")
          val fieldValueSplit = fieldValue.split(HL7_FIELD_REPETITION)
          for ((onefield, j) <- fieldValueSplit.zipWithIndex) {
            if ((fieldIndex <= 0) || (fieldIndex == (j + 1))) {
              finalValue = onefield
              if (comp != null) {
                val compSplit = fieldValue.split(HL7_COMPONENT_SEPARATOR)
                finalValue = compSplit.lift(comp.toInt - 1).getOrElse("")
                if (subcomp != null) {
                  val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
                  finalValue = subCompSplit.lift(subcomp.toInt - 1).getOrElse("")
                }
              }
              if (!"".equals(finalValue))
                fieldArray :+= finalValue
            }
          }
        } else
        if (!"".equals(finalValue))
          fieldArray :+= finalValue
        if (fieldArray.size > 0)
          result :+= fieldArray
      }
      //TODO::All results might be empty... better to return None if that Happens.
      if (result isEmpty)
        None
      else Option(result)
    }
    case _ => None
  }
}

def getFirstValue(path: String): Option[String] = {
  val value = getValue(path)
  if (value.isDefined)
    return Some(getValue(path).get(0)(0))
  None

}
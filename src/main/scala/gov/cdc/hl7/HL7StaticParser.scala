package gov.cdc.hl7

import gov.cdc.utils.IntUtils.SafeInt

import scala.collection.immutable.{ListMap, SortedMap}
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

object HL7StaticParser {
  val FILE_HEADER_SEGMENT = "FHS"
  val BATCH_HEADER_SEGMENT = "BHS"
  val MSH_SEGMENT = "MSH"

  val NEW_LINE_FEED = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
  val HL7_FIELD_SEPARATOR = "\\|"
  val HL7_COMPONENT_SEPARATOR = "\\^"
  val HL7_FIELD_REPETITION = "\\~"
  val HL7_SUBCOMPONENT_SEPARATOR = "\\&"

  val PATH_REGEX = "([A-Z0-9]{3})(\\[([0-9a-zA-Z\\$]+|\\*|(@[0-9A-Za-z\\|\\.!><\\='\\-_ ]*))\\])?(\\-([0-9]+)(\\[([0-9a-zA-Z\\$]+|\\*)\\])?((\\.([0-9]+))(\\.([0-9]+))?)?)?".r
  // val CHILDREN_REGEX = s"$PATH_REGEX>$PATH_REGEX".r
  //  val CHILDREN_REGEX = "(.*) *\\-> *(.*)".r
  //  val FILTER_REGEX = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?\\='([A-Za-z0-9\\-_\\.]+)'".r
  val FILTER_REGEX = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?([!><\\=]{1,2})'(([A-Za-z0-9\\-_\\.]+(\\|\\|)?)+)'".r

  val LAST_INDEX = "$LAST"

  //Returns the Number of segments present of message:
  def peek(msg: String, segment: String): Int = {
    try {
      val result = retrieveMultipleSegments(msg, segment)
      result.size
    } catch {
      case _: HL7ParseError => 0
    }
  }

  @throws(classOf[HL7ParseError])
  def retrieveSegment(msg: String, segment: String): (Int, Array[String]) = {
    val result = retrieveMultipleSegments(msg, segment)
    if (result.nonEmpty && result.size > 1)
      throw HL7ParseError(s"Found multiple segments $segment when only one was expected", segment)
    if (result.nonEmpty)
      result.head
    else
      throw HL7ParseError(s"$segment not found on message", segment)
  }

  @throws(classOf[HL7ParseError])
  def retrieveMultipleSegments(msg: String, segment: String): SortedMap[Int, Array[String]] = {
    var result = scala.collection.mutable.SortedMap[Int, Array[String]]()

    msg.split(NEW_LINE_FEED).zipWithIndex.foreach {
      case (line, index) =>
        if (line.startsWith(segment + "|")) {
          result += (index + 1) -> line.split(HL7_FIELD_SEPARATOR)
        }
    }
    var t = scala.collection.immutable.SortedMap[Int, Array[String]]()
    t ++= result
    t
  }


  @throws(classOf[HL7ParseError])
  def retrieveFirstSegmentOf(msg: String, segment: String): (Int, Array[String]) = {
    //Make sure the First segment is actually retrieved - the one with the smaller lineNumber!
    try {
      ListMap(retrieveMultipleSegments(msg, segment).toSeq.sortBy(_._1): _*).head
    } catch {
      case n: NoSuchElementException => throw HL7ParseError(s"No Segments available for $segment", segment)
    }
  }

  @throws(classOf[HL7ParseError])
  def splitFields(line: Array[String], field: Int): Array[String] = {
    splitFields(line.mkString("|"), field)
  }

  @throws(classOf[HL7ParseError])
  def splitFields(line: String, field: Int): Array[String] = {
    var result = new ListBuffer[Array[String]]()
    try {
      line.split(HL7_FIELD_SEPARATOR).foreach {
        field => result += field.split(HL7_COMPONENT_SEPARATOR)
      }
      if (line.startsWith(MSH_SEGMENT) || line.startsWith(FILE_HEADER_SEGMENT) || line.startsWith(BATCH_HEADER_SEGMENT))
        result(field - 1)
      else
        result(field)
    } catch {
      case _: IndexOutOfBoundsException => throw HL7ParseError(s"FIELD $field does not exist for the provided message ", line.substring(0, 2))
    }
  }

  /* private */ def filterValues(filter: String, segment: Array[String]): Boolean = {
    filter match {
      //      case FILTER_REGEX(field, _*) => {
      //
      //      }
      case FILTER_REGEX(field, _, _, comp, _, subcomp, comparison, constant, _*) => {
        val offset = if ("MSH".equals(segment(0))) 1 else 0

        var valueToCompare = segment.lift(field.toInt - offset).getOrElse("")
        if (comp != null && valueToCompare.nonEmpty) {
          val compSplit = valueToCompare.split(HL7_COMPONENT_SEPARATOR)
          val compInt = (if (comp == LAST_INDEX) compSplit.size else comp.toInt) - 1
          valueToCompare = compSplit.lift(compInt).getOrElse("")
          if (subcomp != null && valueToCompare != null) {
            val subcompSplit = valueToCompare.split(HL7_SUBCOMPONENT_SEPARATOR)
            val subcompInt = (if (subcomp == LAST_INDEX) subcompSplit.size else subcomp.toInt) - 1
            valueToCompare = subcompSplit.lift(subcompInt).getOrElse("")
          }
        }
        var result = false
        constant.split("\\|\\|").foreach { it =>
          comparison match {
            case "=" => result = result || it.equals(valueToCompare)
            case "!=" => result = result || !it.equals(valueToCompare)
            case ">" => result = result || valueToCompare.toInt > it.toInt
            case ">=" => result = result || valueToCompare.toInt >= it.toInt
            case "<" => result = result || valueToCompare.toInt < it.toInt
            case "<=" => result = result || valueToCompare.toInt <= it.toInt
          }

        }
        result
      }
      //      case s if s.matches("@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?\\='(([A-Za-z0-9\\-_\\.]+(\\|\\|)?)+)'") => {
      //        print("Matched! -> ")
      //        println(filter)
      //        true
      //      }
      case _ => false
    }
  }

  private def getListOfMatchingSegments(segIdx: String, segments: SortedMap[Int, Array[String]]): scala.collection.immutable.SortedMap[Int, Array[String]] = {
    var segmentIndex = 0
    var segmentList = segments

    if (segIdx != null && segIdx != "*" && !segIdx.startsWith("@")) {
      segmentIndex = if (segIdx == LAST_INDEX) segments.size else segIdx.toInt
    }
    if (segIdx != null && segIdx.startsWith("@")) { //Filter Segments if filter is provided instead of Index...
      val filteredList = segments filter {
        case (_, v) => filterValues(segIdx, v)
      }
      segmentList = filteredList
    } else if (segmentIndex > 0) { //Get the single
      segmentList = segments.slice(segmentIndex - 1, segmentIndex)
    }
    segmentList
  }

  /* private */ def getListOfMatchingSegments(msg: String, seg: String, segIdx: String): scala.collection.immutable.SortedMap[Int, Array[String]] = {
    getListOfMatchingSegments(segIdx, retrieveMultipleSegments(msg, seg))
  }

  private def drillDownToComponent(comp: String, currentVal: String, subcomp: String): String = {
    var finalValue = currentVal
    if (comp != null) {
      val compSplit = finalValue.split(HL7_COMPONENT_SEPARATOR)
      finalValue = compSplit.lift(comp.toInt - 1).getOrElse("")
      if (subcomp != null) {
        val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
        finalValue = subCompSplit.lift(subcomp.toInt - 1).getOrElse("")
      }
    }
    finalValue

  }


  //  //Get values from a subset of Segment lines:
  //  //Used internally when we need to search on a subset of the file...
  //  private def getValue(path: String, segments: SortedMap[Int, Array[String]]): Option[Array[Array[String]]] = {
  //    path match {
  //      case PATH_REGEX(_, _, _, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
  //        getValue(safeToInt(field), safeToInt(fieldIdx), safeToInt(comp), safeToInt(subcomp), segments)
  //      }
  //      case _ => None
  //    }
  //
  //  }

  //  //Used when retrieving specific children of a parent ( PARENT -> CHILDREN )
  //  private def recursiveAction(msg: String, seg: String, segIdx: String, hl7Hierarhy: HL7Hierarchy, result: ListBuffer[HL7Hierarchy]): Unit = {
  //    if (seg == hl7Hierarhy.segment.substring(0,3)) {
  //      //Not ideal, but working for now.. TODO::Refactor for a more streamlined process
  //      val matchParent = getListOfMatchingSegments(msg, seg, segIdx)
  //      val count = matchParent.count  {
  //        case (k, _) => k == hl7Hierarhy.lineNbr
  //      }
  //      if (count > 0) {
  //        //        result.addAll(hl7Hierarhy.children) //Potential children... to be cleaned up later...
  //        result ++= hl7Hierarhy.children
  //      }
  //    }
  //    hl7Hierarhy.children.foreach { it => // keep going recursively...
  //      recursiveAction(seg, segIdx, it, result)
  //    }
  //  }

  //  Used when retrieving specific children of a parent ( PARENT -> CHILDREN )
  //  private def getChildrenValues(msg: String, parent: String, child: String, removeEmpty: Boolean): Option[Array[Array[String]]] = {
  //    var children: Array[String] = new Array[String](0)
  //    var result: Array[Array[String]] = new Array[Array[String]](0)
  //    parent.trim() match {
  //      case PATH_REGEX(seg, _, segIdx, _, _, _, _, _, _, _, _, _, _) => {
  //        val parentList = ListBuffer[HL7Hierarchy]()
  //        recursiveAction(seg, segIdx, msgHierarchy, parentList)
  //        parentList.foreach( { it =>
  //          children ++= Option(it.segment)
  //        })
  //        //process children:
  //        children.zipWithIndex.foreach({ case (it, i) => //zip in case we want a child by position - say first child.
  //          child.trim() match {
  //            case PATH_REGEX(cseg, _, csegIdx, _, _, cfield, _, cfieldIdx, _, _, ccomp, _, csubcomp) => {
  //              if (cseg == it.substring(0, 3)) { //All children are here. we only want the children for a specific segment.
  //                //SegIDx can be 1) not present, 2) index/number, 3) filter
  //                var childMatch = csegIdx == null //1-> not present
  //                if (csegIdx != null && csegIdx != "*" && !csegIdx.startsWith("@")) { //2: index/number
  //                  childMatch = csegIdx.toInt == i
  //                } else if (csegIdx != null && csegIdx.startsWith("@")) { //3: filter
  //                  if (filterValues(csegIdx, it.split(HL7_FIELD_SEPARATOR))) {
  //                    childMatch = true
  //                  }
  //                }
  //                if (childMatch) {
  //                  result ++= getValue(msg, it.split(HL7_FIELD_SEPARATOR), safeToInt(cfield), safeToInt(cfieldIdx), safeToInt(ccomp), safeToInt(csubcomp), removeEmpty)
  //                }
  //              }
  //            }
  //            case _ => None
  //          }})
  //        Option(result)
  //      }
  //      case _ => None
  //    }
  //  }

  //Method to help non-scala code, because the default parameter value doesn't work
  def getValue(msg: String, path: String): Option[Array[Array[String]]] = {
    getValue(msg: String, path, true)
  }

  //main Entry - can be called  outside code to find values based on path
  def getValue(msg: String, path: String, removeEmpty: Boolean = true): Option[Array[Array[String]]] = {
    //val EMPTY = new Array[String](0)
    path match {
      //TODO:: see what to do with children!!!
      //      case CHILDREN_REGEX(parent, child) => { //Tried implementing a full RegEx, but run into a 22 limit of fields. Breaking down into multiple regex then...
      //        getChildrenValues(msg, parent, child, removeEmpty)
      //      }
      case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
        getValue(msg, seg, segIdx, field.safeToInt(0), fieldIdx, comp.safeToInt(0), subcomp.safeToInt(0), removeEmpty)
      }
      case _ => None
    }
  }

  //Gets values only from a single segment
  def getValue(msg: String, path: String, segment: Array[String], removeEmpty: Boolean): Option[Array[String]] = {
    path match {
      case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
        getValue(segment, field.safeToInt(0), fieldIdx, comp.safeToInt(0), subcomp.safeToInt(0), removeEmpty)
      }
      case _ => None
    }
  }

  //Get values for a subset of segments from the file, like children segments pre filtered before!
  private def getValue(field: Int, fieldIdx: String, comp: Int, subcomp: Int, segments: SortedMap[Int, Array[String]], removeEmpty: Boolean): Option[Array[Array[String]]] = {
    //val offset = if ("MSH".equals(seg)) 1 else 0
    var result: Array[Array[String]] = new Array[Array[String]](0)

    for (((k, segment), i) <- segments.zipWithIndex) {
      val e = getValue(segment, field, fieldIdx, comp, subcomp, removeEmpty)
      if (e.isDefined) {
        result :+= e.get
      }

    }
    //TODO::All results might be empty... better to return None if that Happens.
    return if (result isEmpty)
      None
    else Option(result)
  }

  //Get values from segments matching seg and segIdx of entire file.
  private def getValue(msg: String, seg: String, segIdx: String, field: Int, fieldIdx: String, comp: Int, subcomp: Int, removeEmpty: Boolean): Option[Array[Array[String]]] = {
    val segmentList = getListOfMatchingSegments(msg, seg, segIdx)
    getValue(field, fieldIdx, comp, subcomp, segmentList, removeEmpty)
  }


  //Gets values from a single Segment...
  /* private */ def getValue(segment: Array[String], field: Int, fieldIdx: String, comp: Int, subcomp: Int, removeEmpty: Boolean): Option[Array[String]] = {
    var finalValue: String = segment.mkString("|")
    var fieldArray: Array[String] = new Array[String](0)
    val offset = if ("MSH".equals(segment(0))) 1 else 0
    // var result: Array[String] = new Array[String](0)

    if (field > 0) {
      val fieldValue: String = segment.lift(field.toInt - offset).getOrElse("")
      val fieldValueSplit = fieldValue.split(HL7_FIELD_REPETITION)
      val fieldIdxInt = if (fieldIdx == LAST_INDEX) fieldValueSplit.size else fieldIdx.safeToInt(0)
      if (fieldIdxInt > 0) {
        finalValue = fieldValueSplit.lift(fieldIdxInt - 1).getOrElse("")
        if (comp > 0) {
          val compSplit = finalValue.split(HL7_COMPONENT_SEPARATOR)
          finalValue = compSplit.lift(comp.toInt - 1).getOrElse("")
          if (subcomp > 0) {
            val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
            finalValue = subCompSplit.lift(subcomp.toInt - 1).getOrElse("")
          }
        }
        if (!"".equals(finalValue))
          fieldArray :+= finalValue
      } else {
        for (onefield <- fieldValueSplit.zipWithIndex) {
          finalValue = onefield._1
          if (comp > 0) {
            val compSplit = finalValue.split(HL7_COMPONENT_SEPARATOR)
            finalValue = compSplit.lift(comp.toInt - 1).getOrElse("")
            if (subcomp > 0) {
              val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
              finalValue = subCompSplit.lift(subcomp.toInt - 1).getOrElse("")
            }
          }
          if (!"".equals(finalValue) || !removeEmpty)
            fieldArray :+= finalValue
        }
      }
    } else if (!"".equals(finalValue))
      fieldArray :+= finalValue

    //return if (fieldArray isEmpty)
    return if (removeEmpty && (fieldArray isEmpty))
      None
    else Option(fieldArray)
  }


  def getFirstValue(msg: String, path: String): Option[String] = {
    val value = getValue(msg, path)
    if (value.isDefined && value.isDefined)
      return Some(getValue(msg, path).get(0)(0))
    None
  }

}

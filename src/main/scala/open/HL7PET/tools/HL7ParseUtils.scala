package open.HL7PET.tools

import java.util.NoSuchElementException

import scala.collection.immutable.{ListMap, SortedMap}
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps

class HL7ParseUtils(message: String) {
    val FILE_HEADER_SEGMENT = "FHS"
    val BATCH_HEADER_SEGMENT = "BHS"


    val MSH_SEGMENT = "MSH"
    val PID_SEGMENT = "PID"
    val OBR_SEGMENT = "OBR"
    val OBX_SEGMENT = "OBX"

    val NEW_LINE_FEED = "\\\r?\\\n"
    val HL7_FIELD_SEPARATOR = "\\|"
    val HL7_COMPONENT_SEPARATOR = "\\^"
    val HL7_FIELD_REPETITION = "\\~"
    val HL7_SUBCOMPONENT_SEPARATOR = "\\&"

    val PATH_REGEX = "([A-Z]{3})(\\[([0-9]+|\\*|(@[0-9A-Za-z\\.\\='\\-_ ]*))\\])?(\\-([0-9]+)(\\[([0-9]+|\\*)\\])?((\\.([0-9]+))(\\.([0-9]+))?)?)?".r
   // val CHILDREN_REGEX = s"$PATH_REGEX>$PATH_REGEX".r
    val CHILDREN_REGEX = "(.*) *\\-> *(.*)".r
    val FILTER_REGEX = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?\\='([A-Za-z0-9\\-_\\.]+)'".r


    //Returns the Number of segments present of message:
    def peek(segment: String): Int = {
        try {
            val result = retrieveMultipleSegments(segment)
            result.size
        } catch {
            case e: HL7ParseError => 0
        }
    }

    @throws(classOf[HL7ParseError])
    def retrieveSegment(segment: String): (Int, Array[String]) = {
        val result = retrieveMultipleSegments(segment)
        if (result.nonEmpty && result.size > 1)
            throw HL7ParseError(s"Found multiple segments $segment when only one was expected", segment)
        if (result.nonEmpty)
            result.head
        else
            throw HL7ParseError(s"$segment not found on message", segment)
    }

    @throws(classOf[HL7ParseError])
    def retrieveMultipleSegments(segment: String): SortedMap[Int, Array[String]] = {
        var result = scala.collection.mutable.SortedMap[Int, Array[String]]()

        message.split(NEW_LINE_FEED).zipWithIndex.foreach {
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
    def retrieveFirstSegmentOf(segment: String): (Int, Array[String]) = {
        //Make sure the First segment is actually retrieved - the one with the smaller lineNumber!
        try {
            ListMap(retrieveMultipleSegments(segment).toSeq.sortBy(_._1): _*).head
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

   private def getListOfMatchingSegments(seg: String, segIdx: String, segments: SortedMap[Int, Array[String]]): scala.collection.immutable.SortedMap[Int, Array[String]] = {
     var segmentIndex = 0
     var segmentList = segments

     if (segIdx != null && segIdx != "*" && !segIdx.startsWith("@"))
       segmentIndex = segIdx.toInt
     if (segIdx != null && segIdx.startsWith("@")) { //Filter Segments if filter is provided instead of Index...
       val filteredList = segments filter {
         case (_,v) => filterValues(segIdx, v)
       }
       segmentList = filteredList
     } else  if (segmentIndex > 0) { //Get the single
       segmentList = segments.slice(segmentIndex -1, segmentIndex)
     }
     segmentList
   }

    private def getListOfMatchingSegments(seg: String, segIdx: String): scala.collection.immutable.SortedMap[Int, Array[String]] = {
      getListOfMatchingSegments(seg, segIdx, retrieveMultipleSegments(seg))
    }

    private def safeToInt(nbr: String, default: Int):Int = {
        try {
            nbr.toInt
        } catch {
            case _:NumberFormatException => default
        }
    }

    private def drillDownToComponent(comp: String, currentVal: String, subcomp: String):String  = {
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


    //Get values from a subset of Segment lines:
    //Used internally when we need to search on a subset of the file...
    private def getValue(path: String, segments: SortedMap[Int, Array[String]]): Option[Array[Array[String]]] = {
        path match {
            case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
                getValue(seg, segIdx, safeToInt(field, 0), safeToInt(fieldIdx, 0), safeToInt(comp, 0), safeToInt(subcomp,0), segments)
            }
            case _ => None
        }

    }

    private def processChildren(parentSegment: String, childPath: String, firstIndex: Int): Option[Array[Array[String]]]  = {
        val parentSegments = retrieveMultipleSegments(parentSegment).filter(e => e._1 > firstIndex)
        childPath match {
            case PATH_REGEX(seg, _, segIdx, _, _, _, _, _, _, _, _, _, _) => {
              val secondIndex =
                if (parentSegments.nonEmpty)
                  parentSegments.firstKey
                else {
                  retrieveMultipleSegments(seg).lastKey
                  //message.split(NEW_LINE_FEED).length
                }
              val childrenSegments = retrieveMultipleSegments(seg).filter( e => e._1 > firstIndex && e._1 <= secondIndex)
                return getValue(childPath, getListOfMatchingSegments(seg, segIdx,childrenSegments))
            }
        }
        None
    }

    private def getChildrenValues(parent: String, child: String): Option[Array[Array[String]]]  = {
        var result: Array[Array[String]] = new Array[Array[String]](0)
        parent.trim() match {
            case PATH_REGEX(seg, _, segIdx, _, _, _, _, _ , _, _, _, _, _ ) => {
                val parentList = getListOfMatchingSegments(seg, segIdx)
                var firstIndex = 0
                var secondIndex = 0
                for (((k, _), i) <- parentList.zipWithIndex) {
                    //Get Segments between the seg(k) and seg(k+1)
                    if (i > 0) {
                        firstIndex = secondIndex
                        val e = processChildren(seg, child, firstIndex)
                        if (e.isDefined) {
                            result ++= e.get
                        }
                    }
                    secondIndex = k
                }
                //Need to perform the last batch...
                firstIndex = secondIndex
                val e = processChildren(seg, child, firstIndex)
                if (e.isDefined) {
                    result ++= e.get
                }
                Option(result)
            }
            case _ => None
        }


    }
    //main Entry - can be called out outside code to find values based on path
    def getValue(path: String): Option[Array[Array[String]]] = {
        //val EMPTY = new Array[String](0)
        path match {
            case CHILDREN_REGEX(parent, child) => { //Tried implementing a full RegEx, but run into a 22 limit of fields. Breaking down into multiple regex then...
               getChildrenValues(parent,child)
            }
            case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
                getValue(seg, segIdx, safeToInt(field, 0), safeToInt(fieldIdx, 0), safeToInt(comp, 0), safeToInt(subcomp,0))
            }
            case _ => None
        }
    }

    //Gets values only from a single segment
    def getValue(path: String, segment: Array[String]): Option[Array[String]] = {
        path match {
            case PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
                getValue(segment, seg, segIdx, safeToInt(field, 0), safeToInt(fieldIdx, 0), safeToInt(comp, 0), safeToInt(subcomp,0))
            }
            case _ => None
        }
    }

    //Get values for a subset of segments from the file, like children segments pre filtered before!
    private def getValue(seg: String, segIdx: String, field: Int, fieldIdx: Int, comp: Int, subcomp: Int, segments: SortedMap[Int, Array[String]]): Option[Array[Array[String]]] = {
        val offset = if ("MSH".equals(seg)) 1 else 0
        var result: Array[Array[String]] = new Array[Array[String]](0)

        for (((k, segment), i) <- segments.zipWithIndex) {
            val e =  getValue(segment, seg, segIdx, field, fieldIdx, comp, subcomp)
            if (e.isDefined) {
                result :+= e.get
            }

        }
        //TODO::All results might be empty... better to return None if that Happens.
        return if (result isEmpty)
             None
        else  Option(result)
    }

    //Get values from segments matching seg and segIdx of entire file.
    private def getValue(seg: String, segIdx: String, field: Int, fieldIdx: Int, comp: Int, subcomp: Int): Option[Array[Array[String]]] = {
        var segmentList = getListOfMatchingSegments(seg, segIdx)
        return getValue(seg, segIdx, field, fieldIdx, comp, subcomp, segmentList)
    }


    //Gets values from a single Segment...
    private def getValue(segment: Array[String], seg: String, segIdx: String, field: Int, fieldIdx: Int, comp: Int, subcomp: Int): Option[Array[String]] = {
        var finalValue: String = segment.mkString("|")
        var fieldArray: Array[String] = new Array[String](0)
        val offset = if ("MSH".equals(seg)) 1 else 0
       // var result: Array[String] = new Array[String](0)

        if (field > 0 ) {
            val fieldValue: String = segment.lift(field.toInt - offset).getOrElse("")
            val fieldValueSplit = fieldValue.split(HL7_FIELD_REPETITION)
            if (fieldIdx > 0) {
                finalValue = fieldValueSplit.lift(fieldIdx - 1).getOrElse("")
                if (comp >0) {
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
                for ((onefield, j) <- fieldValueSplit.zipWithIndex) {
                    finalValue = onefield
                    if (comp > 0 ) {
                        val compSplit = finalValue.split(HL7_COMPONENT_SEPARATOR)
                        finalValue = compSplit.lift(comp.toInt - 1).getOrElse("")
                        if (subcomp > 0 ) {
                            val subCompSplit = finalValue.split(HL7_SUBCOMPONENT_SEPARATOR)
                            finalValue = subCompSplit.lift(subcomp.toInt - 1).getOrElse("")
                        }
                    }
                    if (!"".equals(finalValue))
                        fieldArray :+= finalValue
                }
            }
        } else if (!"".equals(finalValue))
            fieldArray :+= finalValue

       return if (fieldArray isEmpty)
             None
        else  Option(fieldArray)
    }


    def getFirstValue(path: String): Option[String] = {
        val value = getValue(path)
        if (value.isDefined && !value.isEmpty)
            return Some(getValue(path).get(0)(0))
        None
    }

}

object HL7ParseUtils {}




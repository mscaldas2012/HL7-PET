package gov.cdc.hl7

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import gov.cdc.hl7.model.{HL7Hierarchy, Profile}
import gov.cdc.utils.IntUtils.SafeInt

import scala.collection.immutable.SortedMap
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.language.postfixOps
import scala.util.matching.Regex

class HL7ParseUtils(message: String, var profile: Profile = null, val buildHierarchy: Boolean = true) {
  //If no Profile is passed, we assume no Hierarchy will be used.
  var profileName = "DefaultProfile.json"

  def this(message: String) {
    this(message, null, false)
  }


  //If a profile is passed, hierarchy is assumed to be ON!
  def this(message: String, profile: Profile) {
    this(message, profile, true)
  }

  if (profile == null) {
    val mapper: ObjectMapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.registerModule(DefaultScalaModule)

    //    println("Using Default profile for hl7")
    val content: String = Source.fromResource("PhinGuideProfile.json").getLines().mkString("\n")

    profile = mapper.readValue(content, classOf[Profile])
  }
  var msgHierarchy: HL7Hierarchy = null
  if (buildHierarchy) {
    msgHierarchy = HL7HierarchyParser.parseMessageHierarchy(message, profile)
  }
  val CHILDREN_REGEX: Regex = "(.*) *\\-> *(.*)".r

  //Returns the Number of segments present of message:
  def peek(segment: String): Int = {
    HL7StaticParser.peek(message, segment)
  }

  @throws(classOf[HL7ParseError])
  def retrieveSegment(segment: String): (Int, Array[String]) = {
    HL7StaticParser.retrieveSegment(message, segment)
  }

  @throws(classOf[HL7ParseError])
  def retrieveMultipleSegments(segment: String): SortedMap[Int, Array[String]] = {
    HL7StaticParser.retrieveMultipleSegments(message, segment)
  }

  @throws(classOf[HL7ParseError])
  def retrieveFirstSegmentOf(segment: String): (Int, Array[String]) = {
    HL7StaticParser.retrieveFirstSegmentOf(message, segment)
  }

  @throws(classOf[HL7ParseError])
  def splitFields(line: Array[String], field: Int): Array[String] = {
    HL7StaticParser.splitFields(line.mkString("|"), field)
  }

  @throws(classOf[HL7ParseError])
  def splitFields(line: String, field: Int): Array[String] = {
    HL7StaticParser.splitFields(line, field)
  }

  //Used when retrieving specific children of a parent ( PARENT -> CHILDREN )
  private def recursiveAction(seg: String, segIdx: String, hl7Hierarhy: HL7Hierarchy, result: ListBuffer[HL7Hierarchy]): Unit = {
    if (seg == hl7Hierarhy.segment.substring(0, 3)) {
      //Not ideal, but working for now.. TODO::Refactor for a more streamlined process
      val matchParent = HL7StaticParser.getListOfMatchingSegments(message, seg, segIdx)
      val count = matchParent.count {
        case (k, _) => k == hl7Hierarhy.lineNbr
      }
      if (count > 0) {
        //        result.addAll(hl7Hierarhy.children) //Potential children... to be cleaned up later...
        result ++= hl7Hierarhy.children
      }
    }
    hl7Hierarhy.children.foreach { it => // keep going recursively...
      recursiveAction(seg, segIdx, it, result)
    }
  }

  //Used when retrieving specific children of a parent ( PARENT -> CHILDREN )
  private def getChildrenValues(parent: String, child: String, removeEmpty: Boolean): Option[Array[Array[String]]] = {
    var children: Array[String] = new Array[String](0)
    var result: Array[Array[String]] = new Array[Array[String]](0)
    parent.trim() match {
      case HL7StaticParser.PATH_REGEX(seg, _, segIdx, _, _, _, _, _, _, _, _, _, _) => {
        val parentList = ListBuffer[HL7Hierarchy]()
        recursiveAction(seg, segIdx, msgHierarchy, parentList)
        parentList.foreach({ it =>
          children ++= Option(it.segment)
        })
        //process children:
        children.zipWithIndex.foreach({ case (it, i) => //zip in case we want a child by position - say first child.
          child.trim() match {
            case HL7StaticParser.PATH_REGEX(cseg, _, csegIdx, _, _, cfield, _, cfieldIdx, _, _, ccomp, _, csubcomp) => {
              if (cseg == it.substring(0, 3)) { //All children are here. we only want the children for a specific segment.
                //SegIDx can be 1) not present, 2) index/number, 3) filter
                var childMatch = csegIdx == null //1-> not present
                if (csegIdx != null && csegIdx != "*" && !csegIdx.startsWith("@")) { //2: index/number
                  childMatch = csegIdx.toInt == i
                } else if (csegIdx != null && csegIdx.startsWith("@")) { //3: filter
                  if (HL7StaticParser.filterValues(csegIdx, it.split(HL7StaticParser.HL7_FIELD_SEPARATOR))) {
                    childMatch = true
                  }
                }
                if (childMatch) {
                  result ++= HL7StaticParser.getValue(it.split(HL7StaticParser.HL7_FIELD_SEPARATOR), cfield.safeToInt(0), cfieldIdx, ccomp.safeToInt(0), csubcomp.safeToInt(0), removeEmpty)
                }
              }
            }
            case _ => None
          }
        })
        Option(result)
      }
      case _ => None
    }
  }

  //Method to help non-scala code, because the default parameter value doesn't work
  def getValue(path: String): Option[Array[Array[String]]] = {
    this.getValue(path, true)
  }

  //main Entry - can be called  outside code to find values based on path
  def getValue(path: String, removeEmpty: Boolean = true): Option[Array[Array[String]]] = {
      if (buildHierarchy) {
        path match {
          case CHILDREN_REGEX(parent, child) => { //Tried implementing a full RegEx, but run into a 22 limit of fields. Breaking down into multiple regex then...
            getChildrenValues(parent, child, removeEmpty)
          }
          case _ => HL7StaticParser.getValue(message, path, removeEmpty)
        }
      } else HL7StaticParser.getValue(message, path, removeEmpty)

  }

  //Gets values only from a single segment
  def getValue(path: String, segment: Array[String], removeEmpty: Boolean): Option[Array[String]] = {
    HL7StaticParser.getValue(message, path, segment, removeEmpty)
  }

  def getFirstValue(path: String): Option[String] = {
    val value = getValue(path)
    if (value.isDefined && value.isDefined)
      return Some(value.get(0)(0))
    None
  }
}

object HL7ParseUtils {

  def getParser(message: String, profileFilename: String): HL7ParseUtils = {
    val profileFile = Source.fromResource(profileFilename).getLines().mkString("\n")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val profile: Profile = mapper.readValue(profileFile, classOf[Profile])

    new HL7ParseUtils(message, profile, true)

  }
}




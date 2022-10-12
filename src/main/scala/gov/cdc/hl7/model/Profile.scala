package gov.cdc.hl7.model

import com.fasterxml.jackson.annotation.JsonAnySetter

import com.google.gson.{JsonObject, JsonParser}

import scala.beans.BeanProperty

class Profile() {

  @BeanProperty
  var segmentDefinition: scala.collection.mutable.Map[String, SegmentConfig] =  scala.collection.mutable.Map()

  @BeanProperty
  var segmentFields: scala.collection.mutable.Map[String, Array[HL7SegmentField]] =  scala.collection.mutable.Map()

  @JsonAnySetter
  def add(segmentName: String, segmentConfig: SegmentConfig) {
    segmentDefinition += (segmentName -> segmentConfig)
  }

  //Shortcut method to avoid profile.segments.segments call...
  def getSegmentField(segmentName: String): Array[HL7SegmentField] = {
    //Need to go down to children to find segments...
     this.segmentFields(segmentName)
  }
}
//
//class FileConfig {
//  var fileSegments: scala.collection.mutable.Map[String, SegmentConfig] =  scala.collection.mutable.Map()
//  @JsonAnySetter
//  def add(segmentName: String, segmentConfig: SegmentConfig) {
//    fileSegments += (segmentName -> segmentConfig)
//  }
//}
//
//class Segment {
//  @BeanProperty
//  var segments: scala.collection.mutable.Map[String, Array[HL7SegmentField]] =  scala.collection.mutable.Map()
//
//  @JsonAnySetter
//  def add(segmentName: String, fields: Array[HL7SegmentField]) {
//    segments += (segmentName -> fields)
//  }
//}


class SegmentConfig {

  @BeanProperty var cardinality: String = _
//  @BeanProperty var fields:  Array[HL7SegmentField] =  _
//  @JsonManagedReference
  @BeanProperty var children: scala.collection.mutable.Map[String, SegmentConfig] = scala.collection.mutable.Map()
//  @JsonBackReference
//  var parent: SegmentConfig = _

  @JsonAnySetter
  def add(segmentName: String, item: SegmentConfig) {
    children += (segmentName -> item)
//    item.parent = this
  }
}

class HL7SegmentField {
  @BeanProperty var fieldNumber: Int = 0
  @BeanProperty var name: String = _
  @BeanProperty var dataType: String = _
  @BeanProperty var maxLength: Int = 0
  @BeanProperty var usage: String = _
  @BeanProperty var cardinality: String = _
  @BeanProperty var default: String = _
  @BeanProperty var conformance: String = _
  @BeanProperty var notes: String = _
  @BeanProperty var xtraValidation: Array[RuleValidation] = _
}

class RuleValidation {
  @BeanProperty var rule: String = _
  @BeanProperty var category: String = _
  @BeanProperty var message: String = _
}


object ProfileFactory {
   def apply(json: String):Profile = {
     val profileJson = JsonParser.parseString(json).getAsJsonObject()
     val profile = new Profile()
     profile.segmentDefinition = processSegmentDefinition(profileJson.get("segmentDefinition").getAsJsonObject())
     profile
   }

  def processSegmentDefinition(segments: JsonObject): scala.collection.mutable.Map[String, SegmentConfig] = {
    val segMap: scala.collection.mutable.Map[String, SegmentConfig] = scala.collection.mutable.Map()
    segments.entrySet().forEach { it => {
        val seg = new SegmentConfig()
        seg.cardinality = it.getValue().getAsJsonObject.get("catdinality").toString()
        seg.children = processSegmentDefinition(it.getValue().getAsJsonObject.get("children").getAsJsonObject)
        segMap += it.getKey -> seg
      }
    }
    return segMap
  }
}


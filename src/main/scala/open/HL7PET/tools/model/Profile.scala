package open.HL7PET.tools.model

import org.codehaus.jackson.annotate.{JsonAnySetter}

import scala.beans.BeanProperty

class Profile {
  @BeanProperty var segments: scala.collection.mutable.Map[String, Array[HL7SegmentField]] =  scala.collection.mutable.Map()

  @JsonAnySetter
  def add(segmentName: String, fields: Array[HL7SegmentField]) {
    segments += (segmentName -> fields)
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
  @BeanProperty var notes: String = _
}

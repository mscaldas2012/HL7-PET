package open.HL7PET.tools.model

import com.fasterxml.jackson.annotation.JsonAnySetter

import scala.beans.BeanProperty

class ValidationRules {
  @BeanProperty var rules: scala.collection.mutable.Map[String, Array[Rule]] =  scala.collection.mutable.Map()

  @JsonAnySetter
  def add(ruleName: String, fields: Array[Rule]) {
    rules += (ruleName -> fields)
  }

}

class Rule {
  @BeanProperty var name: String =  _
  @BeanProperty var comment: String = _
  @BeanProperty var description: String = _
  @BeanProperty var usage: String = _
  @BeanProperty var segment: String = _
  @BeanProperty var field: String = _
  @BeanProperty var reference: String = _
}


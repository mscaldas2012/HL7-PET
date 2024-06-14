package gov.cdc.hl7

import gov.cdc.hl7.HL7StaticParser.NEW_LINE_FEED
import gov.cdc.utils.{ConsoleProgress, FileUtils}

import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

case class RedactInfo(path: String,  var rule:  String, @transient condition: String, lineNumber: Int) {
   @transient var rulemsg = s"Redacted $path with "
    if (rule == null || rule.isEmpty)
           rulemsg += "empty value"
    else  rulemsg += s"value '$rule'"
    if (condition != null && condition.nonEmpty)
        rulemsg += s" when ${condition}"
    rule = rulemsg
}

/**
  * This is a simple De-identifier of HL7 messages where it replaces entire Lines that can potentially have PII data
  * It uses a comma delimited file to configure which lines need to be replaced and the values to replace with.
  * The first column on the config file, should be a HL7 path to find the information to be redacted
  * The second column on the config file, is the text to replace value of the matching path.
  * An optional third column on the config file can provide a special condition of whether to redact or not.
  *
  * Created - 6/2/17
  * Author Marcelo Caldas mcq1@cdc.gov
  */

class DeIdentifier() {
    val FN_REMOVE = "$REMOVE"
    val FN_HASH   = "$HASH"

    def deidentifyFile(filename: String, rules: List[Rule]): Unit = {
        var cleanFile = ""
        FileUtils.using(io.Source.fromFile(filename)) { bf =>
            bf.getLines foreach (line => {
                var subline = line
                rules.foreach(c => {
                    c._regex.findAllIn(line).matchData foreach {
                        m => {
                            subline = c._sentence
                            m.subgroups.zipWithIndex.foreach {
                                case (s, i) => subline = subline.replace(s"$$GROUP(${i + 1})", s)
                            }
                        }
                    }
                })
                cleanFile += subline + "\n"
            })

            val extension = filename.substring(filename.lastIndexOf("."))
            val newFileName = filename.substring(0, filename.lastIndexOf(".")) + "_deidentified" + extension
            FileUtils.writeToFile(newFileName, cleanFile)
        }
    }


    /**
      * Method to deidentify one message with given rules.
      * Returns the de-id message + a list of Redacted fields.
      */
    def deIdentifyMessage(message: String, rules: Array[String]) = {
        var cleanMessage = ""
        val report = scala.collection.mutable.ArrayBuffer.empty[RedactInfo]
        message.split(NEW_LINE_FEED).zipWithIndex.foreach { case(line, lineNbr) => {
            var subline = line
            rules.foreach( r => {
                val rule = r.split(",")
                val path = rule(0)
                val replacement =  if (rule.length > 1) rule(1) else ""
                val condition = if (rule.length > 2) rule(2) else null
                val matchLine = HL7StaticParser.getValue(subline, path) //Make sure the path matches something
                if (matchLine.isDefined && matchLine.get.length > 0) {
                    if (evalCondition(subline, condition)) { //Redact only if Condition evals to TRUE!
                        replacement match {
                            case FN_REMOVE => {
                                subline = ""
                                report += RedactInfo(path, replacement, condition, lineNbr + 1)
                            }
                            case _ =>
                                val lineIndexed = HL7StaticParser.retrieveFirstSegmentOf(subline, path.substring(0, 3))
                                path match {
                                    case HL7StaticParser.PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
                                        if (field != null && lineIndexed._2(field.toInt) != null) {
                                            //Get repeats:
                                            val repeats = lineIndexed._2(field.toInt).split("\\~")
                                            repeats.zipWithIndex.foreach {
                                                case (elem, i) => {
                                                    var redacted = false
                                                    if (fieldIdx == null || fieldIdx.toInt == i + 1) {
                                                        if (comp != null) {
                                                            val compArray = elem.split("\\^")
                                                            if (compArray.length >= comp.toInt) {
                                                                redacted = !compArray(comp.toInt - 1).equals(replacement)
                                                                compArray(comp.toInt - 1) = getReplacementValue(replacement, compArray(comp.toInt - 1))

                                                            }
                                                            if (fieldIdx == null || fieldIdx.toInt == i + 1)
                                                                repeats(i) = compArray.mkString("^")

                                                            else {
                                                                repeats(i) = elem
                                                                redacted = !elem.equals(replacement)
                                                            }
                                                            if (redacted)
                                                                report += RedactInfo(path, replacement, condition, lineNbr + 1)
                                                        } else {
                                                            if (!repeats(i).isEmpty && !replacement.equals(elem)) {
                                                                repeats(i) = getReplacementValue(replacement, elem)
                                                                report += RedactInfo(path, replacement, condition, lineNbr + 1)
                                                            }
                                                        }
                                                    }
                                                }
                                                    lineIndexed._2(field.toInt) = repeats.mkString("~")
                                            }
                                            subline = lineIndexed._2.mkString("|")

                                        } else {
                                            subline = getReplacementValue(replacement, subline) //The whole segment will be replaced!
                                            report += RedactInfo(path, replacement, condition, lineNbr + 1)
                                        }
                                    }
                                }
                        }
                    }
                }
            })
            if (!subline.isEmpty)
                cleanMessage += subline + "\n"
        }}
        (cleanMessage, report.asJava)

    }

    private def evalCondition(msg: String, condition: String): Boolean = {
        if (condition == null || condition.isEmpty)
            return true //empty condition -> Redact!
        val condParts = condition.split(" ") //get PATH<space>Comparator<space>Value
        val msgValue = HL7StaticParser.getFirstValue(msg, condParts(0))//we only support single cardinality for now.
        if (msgValue == null || msgValue == None || msgValue.get.isEmpty)
            return false //don't redact
        return condParts(1).toUpperCase() match {
            case "=" =>   condParts(2).equals( msgValue.get.toUpperCase())
            case "!=" =>  !condParts(2).equals( msgValue.get.toUpperCase())
            case "IN" =>
                val values = condParts(2).substring(1, condParts(2).length -1).split(";")
                values.contains(msgValue.get.toUpperCase())
            case "!IN" =>
                val values = condParts(2).substring(1, condParts(2).length -1).split(";")
                !values.contains(msgValue.get.toUpperCase())
        }
    }

    def deIdentifyFile(messageFileName: String, rulesFileName: String): Unit = {
        val rulesFile = FileUtils.readFile(rulesFileName)
        val message = FileUtils.readFile(messageFileName)
        val (cleanFile, report) = deIdentifyMessage(message, rulesFile.split(NEW_LINE_FEED))

        val extension = messageFileName.substring(messageFileName.lastIndexOf("."))
        val newFileName = messageFileName.substring(0, messageFileName.lastIndexOf(".")) + "_deidentified" + extension
        FileUtils.writeToFile(newFileName, cleanFile)
    }

    def getReplacementValue(replacement: String, originalValue: String): String = {
        replacement match {
            case FN_HASH => Math.abs(originalValue.hashCode).toString
            case _ => replacement
        }
    }

}

object DeIdentifier {

}

object DeIdentifierApp {
    val DEFAULT_RULES_FILE = "redaction_rules.txt"

    def showUsage() = {
        println("Pass the file you want to translate and the file with rules.")
        println("You might also pass a second parameter indicating which file contains the rules to use as replacement")
        println("\nUsage:")
        println("java hl7.DeIdentifierApp <file_to_be_de-identified> [<file_with_rules>]")
        System.exit(1)
    }

    def main(args: Array[String]) = {
        if (args.length < 0 || args.length > 2)
            showUsage()

//        val configRules: List[Array[String]] =
//            if (args.length == 1)
//                CSVReader.readFileFromResource(DEFAULT_RULES_FILE)
//            else
//                CSVReader.readFileFromPath(args(1))

        val deid = new DeIdentifier
        ConsoleProgress.showProgress {
            //convert configRules to Regex:
//            var allRules = configRules.map(c => Rule(c(0), c(1)))
            deid.deIdentifyFile(args(0), args(1))
        }

    }
}
/**
  * Helper class to Have the strings on config file translated to Regex expressions to be used
  * @param _rule the rule to be translated
  * @param sentence the sentence to replace
  * @param allowedValues the list of allowed values that do not need to be redacted. (Optional)
  */
case class Rule(_rule: String, sentence: String, allowedValues: String) {
    val _regex:Regex = _rule.r
    val _sentence = sentence
}


//object TestApp extends App {
//    var deid = new DeIdentifier("src/main/resources/default_rules.txt")
//    deid.deidentifyFile("src/test/resources/ShortCancer.txt")
//}


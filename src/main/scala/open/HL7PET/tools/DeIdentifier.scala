package open.HL7PET.tools

import open.HL7PET.tools.HL7StaticParser.NEW_LINE_FEED
import utils.{CSVReader, ConsoleProgress, FileUtils}

import scala.util.matching.Regex

/**
  * This is a simple De-identifier of HL7 messages where it replaces entire Lines that can potentially have PII data
  * It uses a comma delimited file to configure which lines need to be replaced and the values to replace with.
  * The first column on the config file, should be a regular expression to match the text
  * The second column on the config file, is the text to replace the entire line with.
  *
  * Created - 6/2/17
  * Author Marcelo Caldas mcq1@cdc.gov
  */

class DeIdentifier() {
    val FN_REMOVE = "$REMOVE"

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

    def deIdentifyFile(messageFileName: String, rulesFileName: String): Unit = {
        val rulesFile = FileUtils.readFile(rulesFileName)
        var cleanFile = ""
        FileUtils.using(io.Source.fromFile(messageFileName)) { bf =>
            bf.getLines foreach (line => {
                var subline = line
                val rules = rulesFile.split(NEW_LINE_FEED)
                rules.foreach( r => {
                    val rule = r.split(",")
                    val path = rule(0)
                    val replacement = if (rule.length > 1) rule(1) else ""
                    val matchLine = HL7StaticParser.getValue(subline, path) //Make sure the path matches something
                    if (matchLine.isDefined && matchLine.get.length >0) {
                        replacement match {
                            case FN_REMOVE => subline = ""
                            case _ =>
                                val lineIndexed = HL7StaticParser.retrieveFirstSegmentOf(subline, path.substring(0, 3))
                                path match {
                                    case HL7StaticParser.PATH_REGEX(seg, _, segIdx, _, _, field, _, fieldIdx, _, _, comp, _, subcomp) => {
                                        if (field != null && lineIndexed._2(field.toInt) != null) {
                                            //Get repeats:
                                            val repeats = lineIndexed._2(field.toInt).split("\\~")
                                            repeats.zipWithIndex.foreach {
                                                case (elem, i) => {
                                                    //                                                    if (fieldIdx == null) {
                                                    if (comp != null) {
                                                        val compArray = elem.split("\\^")
                                                        if (compArray.length >= comp.toInt)
                                                            compArray(comp.toInt - 1) = replacement
                                                        if (fieldIdx == null || fieldIdx.toInt == i + 1)
                                                            repeats(i) = compArray.mkString("^")
                                                        else
                                                            repeats(i) = elem
                                                    }
                                                    else {
                                                        if (!repeats(i).isEmpty)
                                                            repeats(i) = replacement
                                                    }
                                                    //                                                    } else { //replace a single fieldIdx
                                                    //
                                                    //                                                    }
                                                }
                                                    lineIndexed._2(field.toInt) = repeats.mkString("~")
                                            }
                                            subline = lineIndexed._2.mkString("|")
                                        } else {
                                            subline = replacement //The whole segment will be replaced!
                                        }
                                    }
                                }

                        }
                    }
                })
                if (!subline.isEmpty)
                    cleanFile += subline + "\n"
            })
        }
        val extension = messageFileName.substring(messageFileName.lastIndexOf("."))
        val newFileName = messageFileName.substring(0, messageFileName.lastIndexOf(".")) + "_deidentified" + extension
        FileUtils.writeToFile(newFileName, cleanFile)
    }

}

object DeIdentifier {

}

object DeIdentifierApp {
    val DEFAULT_RULES_FILE = "deid_rules.txt"

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
  * @param sentence the sentense to replace
  */
case class Rule(_rule: String, sentence: String) {
    val _regex:Regex = _rule.r
    val _sentence = sentence
}


//object TestApp extends App {
//    var deid = new DeIdentifier("src/main/resources/default_rules.txt")
//    deid.deidentifyFile("src/test/resources/ShortCancer.txt")
//}


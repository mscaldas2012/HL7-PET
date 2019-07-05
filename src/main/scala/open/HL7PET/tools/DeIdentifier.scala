package open.HL7PET.tools

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

}

object DeIdentifier {

}

object DeIdentifierApp {
    val DEFAULT_RULES_FILE = "default_rules.txt"

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

        val configRules: List[Array[String]] =
            if (args.length == 1)
                CSVReader.readFileFromResource(DEFAULT_RULES_FILE)
            else
                CSVReader.readFileFromPath(args(1))

        var deid = new DeIdentifier
        ConsoleProgress.showProgress {
            //convert configRules to Regex:
            var allRules = configRules.map(c => Rule(c(0), c(1)))
            deid.deidentifyFile(args(0), allRules)
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


package open.HL7PET.tools

import utils.{ConsoleProgress, FileUtils}

/**
  *
  *
  * Created - 6/2/17
  * Author Marcelo Caldas mcq1@cdc.gov
  */
class HL7FileUtils {
    def generateFileName(outputDir: String, prefix: String , counter: Int, extension: String = ".txt"): String = {
        var newFile = outputDir
        //Make sure output directory ends in a slash to separate frm the filename
        if (!outputDir.endsWith("/")) {
            newFile +=  "/"
        }
        //make sure extension has a dot
        var newExtension = extension
        if (!newExtension.startsWith(".")) {
            newExtension = "." + extension
        }
        newFile += prefix +  "_" + "%05d".format(counter) + newExtension
        newFile
    }
    val MSH = "MSH"

    def splitMessages(filename: String, outputDir: String, outputFileNamePrefix: String): Unit = {
        //Make sure the outputDir exists:
        FileUtils.mkdirs(outputDir.split("/"))
        var newFile = 1
        var newMessage = ""

        //val bufferedSource = io.Source.fromFile(filename)
        FileUtils.using(io.Source.fromFile(filename)) {
            bufferedSource => {
                val allLines = bufferedSource.mkString
                //This method does not cope with the string MSH being used anywhere else in the code and not as a Field Header.
                //But then again, it should'n't be used, right!?
                allLines.split("MSH").zipWithIndex.foreach {
                    case (l, i)  =>
                        if (!l.isEmpty)
                            FileUtils.writeToFile(generateFileName(outputDir, outputFileNamePrefix,  i), "MSH" +l)

                }
                //Line by line style
                /**
                for (line <- bufferedSource.getLines) {
                    if (line.startsWith(MSH)) {
                        //persiste previous message:
                        if (!newMessage.isEmpty) {
                            //not first message:
                            //implicit val codec = scalax.io.Codec.UTF8
                            FileUtils.writeToFile(generateFileName(outputDir, outputFileNamePrefix, newFile), newMessage)
                        }
                        //Start New message:
                        newMessage = line + "\n" //init message
                        newFile += 1
                    } else {
                        newMessage += line + "\n" //conitnue building message.
                    }
                }
                //Save Last Message:
                FileUtils.writeToFile(generateFileName(outputDir, outputFileNamePrefix, newFile), newMessage)
                  **/
            }
        }
    }

    def genOBXMesages(filename: String): Unit = {
        val OBX = "OBX"
        var cleanFile = ""
        FileUtils.using(io.Source.fromFile(filename)) { bufferedSource =>
            bufferedSource.getLines foreach { line =>
                if (line.startsWith(OBX)) {
                    val fields = line.split("\\|")
                    val title = fields(3).split("\\^")(1)
                    cleanFile = s"$cleanFile$title :\n${fields(5)}\n\n"
                }
            }
            val newfile = filename.substring(0, filename.indexOf("."))
            val extension = filename.substring(filename.lastIndexOf("."))
            FileUtils.writeToFile(newfile + "_clean" + extension, cleanFile)
        }
    }

    def cleanAllFiles(dir: String): Unit = {
        FileUtils.getListOfFiles(dir).foreach( f => {
            genOBXMesages(f.getAbsolutePath)
        })
    }



}
object HL7FileUtils {}

object TESTApp extends App {
    var hl7Util = new HL7FileUtils()
    //hl7Util.genOBXMesages("target/gen/Cancer_00002.txt")
    hl7Util.cleanAllFiles("target/gen")
}

object HL7FileUtilsApp  {

    def showUsage(): Unit = {
        println("You must pass at least the name of the file you would like to split")
        println("\nUsage: ")
        println("java open.HL7PET.tools.HL7FileUtilsApp <fileName> [-o outputDirectory] [-p filePrefix]")
        println("\n\nwhere:")
        println("\t<fileName> is the name of the file with all HL7 Messages to be split")
        println("\t[-o outputDirectory] is an optional parameter to specify the output directory where files should be created. Default current directory.")
        println("\t[-p filePrefix] is an optiona parameter to specify the prefix of the generated files. Default to fileName")
        System.exit(1)
    }

    def main(args:Array[String]) = {
        if (args.length < 1) {
            showUsage()
        }
        var messageFile = args(0)
        var outDir = "./gen"
        var file = messageFile.substring(messageFile.lastIndexOf("/") + 1, messageFile.indexOf("."))
        if (args.length > 1) {
            //Need 3 or 5 parameters:
            if (!(args.length == 3 || args.length == 5)) {
                showUsage()
            }
            var index = 1
            while (index < args.length) {
                if (args(index).equalsIgnoreCase("-o")) {
                    outDir = args(index + 1)
                } else if (args(index).equalsIgnoreCase("-p")) {
                    file = args(index + 1)
                } else {
                    showUsage()
                }
                index +=2
            }

        }
        ConsoleProgress.showProgress( {
            var hl7Utils = new HL7FileUtils()
            hl7Utils.splitMessages(messageFile, outDir, file)
            hl7Utils.cleanAllFiles(outDir)
        })
    }

}


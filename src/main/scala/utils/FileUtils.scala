package utils

import java.io.{File, FileWriter, PrintWriter}
import scala.io.Source

/**
  *
  *
  * Created - 6/2/17
  * Author Marcelo Caldas mcq1@cdc.gov
  */

object FileUtils {

    /**
      * Handles a file and closes it after use
      */
    def using[A <: {def close() : Unit}, B](param: A)(f: A => B): B =
        try {
            f(param)
        } finally {
            param.close()
        }

    /**
      * Writes a new File - overwrites if already exists.
      * @param fileName
      * @param data
      */
    def writeToFile(fileName: String, data: String) =
        using(new FileWriter(fileName)) {
            fileWriter => fileWriter.write(data)
        }

    /**
      * Writes to a file - creates the file if doesn't exist. Appends to the end otherwise
      * @param fileName
      * @param textData
      */
    def appendToFile(fileName: String, textData: String) =
        using(new FileWriter(fileName, true)) {
            fileWriter =>
                using(new PrintWriter(fileWriter)) {
                    printWriter => printWriter.println(textData)
                }
        }

    /**
      * Make sure all directories in the path exists. If not creates them.
      * @param path
      * @return
      */
    def mkdirs(path: Array[String]) = // return true if path was created
        path.tail.foldLeft(new File(path.head)){(a,b) => a.mkdir; new File(a,b)}.mkdir

    /**
      * Gets all the files on a give directory. (Only files)
      * @param dir
      * @return
      */
    def getListOfFiles(dir: String):List[File] = {
        val d = new File(dir)
        if (d.exists && d.isDirectory) {
            d.listFiles.filter(_.isFile).toList
        } else {
            List[File]()
        }
    }

    def readResourceFile(fileName: String): String = {
        val content: String = Source.fromResource(fileName).getLines().mkString("\n")
        content
    }
    def readFile(fileName: String): String = {
        val content: String = Source.fromFile(fileName).getLines().mkString("\n")
        content
    }

}

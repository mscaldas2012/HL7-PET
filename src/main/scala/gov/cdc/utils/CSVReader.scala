package gov.cdc.utils

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by caldama on 2/16/17.
  */

class CSVReader {}

object CSVReader {
    def readFile(file: Source, delim: String): List[Array[String]] = {
        var lines = new ListBuffer[Array[String]]()
        file.getLines.foreach ( line => {
            val row = line.split(delim + "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)").map(_.trim.replaceAll("\"", ""))
            lines += row
        })
        lines.toList
    }
    def readFileFromResource(filename: String, delim: String = ","): List[Array[String]] = {
        FileUtils.using(io.Source.fromResource(filename)) {
            readFile(_, delim)
        }
    }

    def readFileFromPath(filename: String, delim: String = ","): List[Array[String]] = {
        FileUtils.using(io.Source.fromFile(filename)) {
            readFile(_, delim)
        }
    }
}

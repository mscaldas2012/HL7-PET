import open.HL7PET.tools.HL7ParseUtils

import scala.io.StdIn

object ExampleApp {

  def printResults(maybeStrings: Option[Array[Array[String]]]) = {
    println(s"results ")
    if (maybeStrings.isDefined) {
      maybeStrings.get foreach {
        v => v.foreach(f => println(s"\t--> $f"))
      }
    }
    println("---")
  }

  def main(args:Array[String]) = {

    var filename = args.lift(0).getOrElse("ARLN_GC_DUB.hl7")
    val source = io.Source.fromResource(filename)
    var allLines = ""
    for (line <- source.getLines) {
      allLines += line + "\n"
    }

    val hl7Util = new HL7ParseUtils(allLines)

    var ok = true
    while (ok) {
      print("path> ")
      val command = StdIn.readLine
      ok = command != null
      if (ok)
        printResults(hl7Util.getValue(command))
    }



  }

}

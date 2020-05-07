package open.HL7PET.tools

import scala.collection.mutable.ArrayBuffer


class ValidationErrors extends Serializable {
  var totalErrors:Int = _
  var totalWarnings:Int = _

  var entries: ArrayBuffer[ErrorEntry] = _

  def addEntry(entry: ErrorEntry) {
    if (entries == null)
      entries = new ArrayBuffer[ErrorEntry]()
    //If path already there, don't add .
    if (!entries.exists{ it => it.path == entry.path}) {
      entries += entry
      entry.classification match {
        case ERROR => totalErrors += 1
        case WARNING => totalWarnings += 1
      }

    }
  }

  override def toString: String = {
    var s = s"totalErrors: $totalErrors\ntotalWarnings: $totalWarnings\n"
    if (totalErrors > 0 || totalWarnings > 0) {
      s += "entries:\n"
      entries.foreach(e => s += s"$e\n")
    }
    s
  }

  //Methods for JSON serilization
  def getTotalErrors(): Int = totalErrors
  def getTotalWarnings(): Int = totalWarnings
  def getEntries(): List[ErrorEntry] = entries.toList
}

class ErrorEntry (val line: Int, val beginColumn: Int, val endColumn: Int, val path:  String, val classification: ClassificationEnum, val category: String) extends Serializable {
  var description: String = _
  var stacktrace: String = _
  //Methods used for serialization on java end.
  def getLine(): Int = { line}
  def getBeginColumn(): Int = { beginColumn }
  def getEndColumn(): Int = {endColumn }
  def getPath(): String = {path }
  def getClassification(): String = {classification.toString}
  def getDescription(): String = {description }
  def getCategory(): String = {category}
  def getStackTrace(): String = {stacktrace}

  override def toString: String = {
     s"$classification ==> Loc: $line:$beginColumn-$endColumn, Path: $path, Category: $category, Description: $description"
  }
}



sealed trait ClassificationEnum
case object ERROR extends ClassificationEnum
case object WARNING extends ClassificationEnum
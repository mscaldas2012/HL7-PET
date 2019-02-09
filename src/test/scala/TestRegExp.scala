import org.scalatest.FlatSpec

import scala.collection.immutable.ListMap
import scala.util.Try

class TestRegExp  extends FlatSpec {
  val myregExp = "OBX\\|([0-9]?)\\|TX\\|(19139)\\-5".r
  val myText = "OBX|9|TX|19139-5^PATHOLOGIST NAME^LN^500929^SIGNED^L|| Electronically signed:  "

  "RegExp" should "display groups" in {
    println("match data...")
    val data = myregExp.findAllIn(myText).matchData foreach(m => {
      println(s".  $m")
      println(s"Group count: ${m.groupCount}")
      println("\tsubgroups..")
      var i = 0
      m.subgroups foreach ( s => {
        i += 1
        println(s"\t $i. - $s")
      })
    })

    println("toList...")
    myregExp.findAllIn(myText).toList foreach(m => println(m))
  }

  "String " should "not throw NumberFormatExcpetion" in {
     val s = "abc"
    val i = Try{s.toInt}.isSuccess
     println("number: " + i)
  }


  "Map " should "be sorted by key" in {
    var testMap =  scala.collection.mutable.Map[Int, String]()
    testMap += 3 -> "third"
    testMap += 1 -> "first"
    testMap += 2 -> "seoncd"
    println(ListMap(testMap.toSeq.sortBy(_._1):_*).head)

  }

  "date" should "not parse" in {
    val date = "123456789012345678901234567"
    val format = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
    println(format.parse(date))
  }

  "repeatedFields" should "separate them" in {
    val string = "~OPH^2.1.840.1^ISO~OPH2^2.2.2^ISO"
    //val string = "OPH^2.1.840.1^ISO"
    val splitted = string.split("~")
    for ( s <- splitted )
      println(s"value: $s")
  }


  "BatchHeaders" should "match expression" in {
    var headerSeg = "^[MBF]HS\\[[0-9]+\\]$"

    println("abc".matches(headerSeg), false)
    println("FHS[1]".matches(headerSeg), true)
    println("BHS[1]".matches(headerSeg), true)
    println("MHS[1]".matches(headerSeg), true)
    println("BHS[1]-3[1]".matches(headerSeg), false)
    println("PID[1]".matches(headerSeg), false)

    if ("abc".matches(headerSeg)) {
      println("match found")
    } else {
      println("match not found")
    }

    val patter = headerSeg.r
    "abc" match {
      case patter(_) => println("case match found")
      case _ => println("case match not found")
    }
    "BHS[1]" match {
      case patter(_) => println("case match found")
      case _ => println("case match not found")
    }
    "BHS[1]-3[1]" match {
      case patter(_) => println("case match found")
      case _ => println("case match not found")
    }
  }
}



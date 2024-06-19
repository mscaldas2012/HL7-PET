import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.immutable.ListMap
import scala.util.Try

class TestRegExp  extends AnyFlatSpec {
  val myregExp = "OBX\\|([0-9]?)\\|TX\\|(19139)\\-5".r
  val myText = "OBX|9|TX|19139-5^PATHOLOGIST NAME^LN^500929^SIGNED^L|| Electronically signed:  "

  //The Reg Ex below represents the following format: YYYY[MM[DD[HH[MM[SS[.S[S[S[S]]]]]]]]][+/-ZZZZ].
  val anyDateRegEx = "\\d{4}(((\\d{2}){5}\\.\\d{1,4})|((\\d{2}){0,5}))([+-]\\d{4})?"
  val indexRegEx = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?([!><\\=]{1,2})'(([A-Za-z0-9\\-_\\.]+(\\|\\|)?)+)'".r
//  val indexRegEx = "@([0-9]+)((\\.([0-9]+))(\\.([0-9]+))?)?\\='(([A-Za-z0-9\\-_\\.]+(\\|\\|)?)+)'".r

  "RegExp" should "display groups" in {
    println("match data...")
    val path = "@1>='2'"
    val data = indexRegEx.findAllIn(path).matchData foreach(m => {
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
    indexRegEx.findAllIn(path).toList foreach(m => println(m))
    path match {
      case indexRegEx(field, _, _, comp, _, subcomp, comparison, constant, _*) => {
        println(field)
        println(comparison)
        println(constant)
      }
    }
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


  "arrays" should "behave" in {
    val test = Array("a", "b", "c", "d")
    val third = test.drop(3-1).take(1)
    println(third.foreach (i => println(i)))

    val testOne = Array("a")
    val single = testOne.drop(1-1).take(1)
    println(single.foreach (i => println(i)))

//    invalid:
     val noItem = test.drop(5-1).take(1)
    println(noItem.foreach (i => println(i)))

  }
}



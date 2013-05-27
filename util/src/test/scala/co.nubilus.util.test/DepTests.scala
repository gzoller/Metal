// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util
package test

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.{ Await, Future }
import akka.actor.ActorSystem
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

class DepTests extends FunSpec with MustMatchers with GivenWhenThen with BeforeAndAfterAll {

	case class D( val name:String, val _dependents:List[String], val _getDependent:(String)=>Option[Dependable] ) extends Dependable {
		implicit val dependentsCredential = dependents.allowAssignment
		implicit val getDependentCredential = getDependent.allowAssignment
		dependents   := _dependents
		getDependent := _getDependent
		def readyMe = true
	}
	case class Dfast( val name:String, val _dependents:List[String], val _getDependent:(String)=>Option[Dependable] ) extends Dependable {
		implicit val dependentsCredential = dependents.allowAssignment
		implicit val getDependentCredential = getDependent.allowAssignment
		dependents   := _dependents
		getDependent := _getDependent
		var _prepared = false
		def readyMe = { 
			if(!_prepared) 
				Thread.sleep(800);  
			else
				_prepared = true; 
			true 
		}
	}
	case class Dslow( val name:String, val _dependents:List[String], val _getDependent:(String)=>Option[Dependable] ) extends Dependable {
		implicit val dependentsCredential = dependents.allowAssignment
		implicit val getDependentCredential = getDependent.allowAssignment
		dependents   := _dependents
		getDependent := _getDependent
		var _prepared = false
		def readyMe = { 
			if(!_prepared) 
				Thread.sleep(5000);  
			else
				_prepared = true; 
			true 
		}
	}
	case class Dnot( val name:String, val _dependents:List[String], val _getDependent:(String)=>Option[Dependable] ) extends Dependable {
		implicit val dependentsCredential = dependents.allowAssignment
		implicit val getDependentCredential = getDependent.allowAssignment
		dependents   := _dependents
		getDependent := _getDependent
		def readyMe = false
	}

	val config = ConfigFactory.parseString("""
		akka {
			event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
			actor {
		  		provider = "akka.actor.LocalActorRefProvider"
		  	}
		}    
    """)
	implicit val sys = ActorSystem("foo",config)

	val twoSec = Duration("2 seconds")

	case class Block() {
		val aa = D(    "A",List[String](),       finder)
		val ba = D(    "B",List("C","D"),        finder)
		val ca = D(    "C",List[String](),       finder)
		val da = D(    "D",List[String](),       finder)
		val ea = D(    "E",List("D"),            finder)
		val fa = D(    "F",List("G","H","C","M"),finder)
		val ga = Dfast("G",List[String](),       finder)
		val ha = Dfast("H",List[String](),       finder)
		val ia = D(    "I",List("J","K","C","F"),finder)
		val ja = Dslow("J",List[String](),       finder)
		val ka = Dslow("K",List[String](),       finder)
		val la = Dnot ("L",List("B","A"),        finder)
		val ma = Dfast("M",List[String](),       finder)
		val na = D    ("N",List("O","P"),        finder)
		val oa = D    ("O",List("XX","YY"), {(s:String) => None})
		val pa = D    ("P",List("ZZ"),      {(s:String) => None})
		val all = collection.mutable.HashMap("A"->aa, "B"->ba, "C"->ca, "D"->da, "E"->ea, "F"->fa, "G"->ga, "H"->ha, "I"->ia, "J"->ja, "K"->ka, "L"->la, "M"->ma)
		def finder(s:String) : Option[Dependable] = all.get(s)
	}

	describe("==============================\n| --   Dependable Tests   -- |\n==============================") {
		it("Must recognize unknown dependencies") {
			val b = Block()
			b.all ++= Map("N"->b.na, "O"->b.oa, "P"->b.pa)
			val res = Dependable.waitForAll(List(b.na),b.all.values.toList,twoSec)
			res._1 must be (false)
			res._2.getAndClear must be (List(
				"Specified dependency [ZZ] was not defined.", 
				"Specified dependency [XX] was not defined.", 
				"Specified dependency [YY] was not defined."
				))
		}
		it("Must show ready for no dependencies") {
			val b = Block()
 			Dependable.waitForAll(List(b.aa),b.all.values.toList,twoSec)._1 must be ( true )
		}
		it("Must show ready when having dependencies that are themselves all ready") {
			val b = Block()
 			Dependable.waitForAll(List(b.ba),b.all.values.toList,twoSec)._1 must be ( true )
		}
		it("Must show ready when dependencies that are ready within the allowed time") {
			val b = Block()
 			Dependable.waitForAll(List(b.fa),b.all.values.toList,twoSec)._1 must be ( true )
		}
		it("Must show NOT ready when dependencies that are NOT ready within the allowed time") {
			val b = Block()
 			Dependable.waitForAll(List(b.ia),b.all.values.toList,twoSec)._1 must be ( false )
		}
		it("Must show NOT ready when dependencies are ready but we aren't") {
			val b = Block()
 			Dependable.waitForAll(List(b.la),b.all.values.toList,twoSec)._1 must be ( false )
		}
		it("Given a simple list of Dependables, see if they're all ready") {
			val b = Block()
 			Dependable.waitForAll(List(b.aa,b.ca,b.da),b.all.values.toList,twoSec)._1 must be ( true )
		}
		it("Given a deep list of Dependables, see if they're all ready") {
			val b = Block()
 			Dependable.waitForAll(List(b.aa,b.fa,b.ha),b.all.values.toList,twoSec)._1 must be ( true )
		}
		it("Given a deep list of Dependables that are NOT ready, report which ones failed") {
			val b = Block()
 			val res = Dependable.waitForAll(List(b.la,b.ia),b.all.values.toList,twoSec)
 			res._1 must be ( false )
 			res._2.getAndClear must be (List(
				"Dependable [J] failed/didn't initialize within the timeout window.",
				"Dependable [K] failed/didn't initialize within the timeout window.",
				"Dependable [L] failed/didn't initialize within the timeout window.",
				"Dependable [I] failed/didn't initialize within the timeout window."
				))
		}
	}

	override def afterAll(configMap: org.scalatest.ConfigMap) {
		sys.shutdown
	}
}

// S.D.G.
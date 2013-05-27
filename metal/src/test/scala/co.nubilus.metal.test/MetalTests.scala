// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package test

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.{ Await, Future }
import akka.actor.ActorSystem
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

class MetalTests extends FunSpec with MustMatchers with GivenWhenThen with BeforeAndAfterAll {

	def config1( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "5 seconds"
		                        extensions     = []
		                    }
		                    """ + akkaConfig(p)
	def config2( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "5 seconds"
		                        bingo          = dog
		                        extensions     = ["co.nubilus.metal.test.ExtA","co.nubilus.metal.test.ExtB"]
		                    }
		                    """ + akkaConfig(p)
	def config3( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "1 second"
		                        bingo          = aardvark
		                        extensions     = ["co.nubilus.metal.test.ExtB","co.nubilus.metal.test.ExtA"]
		                    }
		                    """ + akkaConfig(p)
	def akkaConfig(p:Int) = s"""akka {
			                        loglevel = INFO
			                        stdout-loglevel = INFO
			                        event-handlers = ["akka.event.slf4j.Slf4jEventHandler"]
			                        actor {
			                                provider = "akka.remote.RemoteActorRefProvider"
			                        }
			                        remote {
			                                transport = "akka.remote.netty.NettyRemoteTransport"
			                                netty {
			                                        port = $p
			                                }
			                        }
		                        }"""

	val metal1 = MyMetal( ConfigFactory.parseString( config1(9011) ) )
	val metal2 = MyMetal( ConfigFactory.parseString( config2(9012) ) )
	val metal3 = MyMetal( ConfigFactory.parseString( config3(9023) ) )
	val timeoutDur = Duration("5 seconds")
	implicit val timeout = akka.util.Timeout( timeoutDur.length, timeoutDur.unit )

	describe("=========================\n| --   Metal Tests   -- |\n=========================") {
		it("akka.remote.netty.hostname correctly instered into configuration at runtime") {
			metal1.config getString "akka.remote.netty.hostname" must equal( metal1.myHostname )
		}
		it("Akka URIs must be properly generated - local host, default port") {
			val ip = metal1.myHostname
			metal1.akkaUri( "blah" ) must equal( s"akka://nubilus@$ip:9011/user/blah" )
		}
		it("Akka URIs must be properly generated - local host, given port") {
			val ip = metal1.myHostname
			metal1.akkaUri( "blah", 9131 ) must equal( s"akka://nubilus@$ip:9131/user/blah" )
		}
		it("Akka URIs must be properly generated - given host, default port") {
			metal1.akkaUri( "blah", 9131, "1.2.3.4" ) must equal( "akka://nubilus@1.2.3.4:9131/user/blah" )
		}
		it("Extensions must load and configuration properly layered") {
			Given("There are extensions to the base Metal")
			When("The extensions are loaded in-order")
			Then("The configuration should be similarly layered")
			metal2.config getString "nubilus.bingo" must be ("dog")
			val aa = metal2.getExt("A")
			aa must be ('defined)
			val a = aa.get.asInstanceOf[ExtA]
			a.bingo must be ("cat")
			a.loadedConfig.get getString "A.foo" must be ("bar")
			a.loadedConfig.get.hasPath("B.here") must be (false)
			val bb = metal2.getExt("B")
			bb must be ('defined)
			val b = bb.get.asInstanceOf[ExtB]
			b.bingo must be ("fish")
			b.loadedConfig.get getString "A.foo" must be ("bar")
			b.loadedConfig.get getString "B.here" must be ("there")
		}
		it("""Must answer the Ping message""") {
			val resp = Await.result( metal2.actorSystem.actorFor(metal2.akkaUri("metal")) ? Ping(Map[String,Any]()), timeoutDur)
			val s = resp.asInstanceOf[StatusMsg]
			s.ready must equal (true)
		}
		it("""Must answer the Ping message server with init problems (is server "valid")?""") {
			val resp = Await.result( metal3.actorSystem.actorFor(metal3.akkaUri("metal")) ? Ping(Map[String,Any]()), timeoutDur)
			val s = resp.asInstanceOf[StatusMsg]
			s.ready must equal (false)
			s.problems must be( List("Dependable [A] failed/didn't initialize within the timeout window.","Dependable [B] failed/didn't initialize within the timeout window."))
		}
		it("""Must answer the Ping message server with any problems""") {
			metal2.problems.complain("issue_1")
			metal2.problems.complain("issue_2")
			val resp = Await.result( metal2.actorSystem.actorFor(metal2.akkaUri("metal")) ? Ping(Map[String,Any]()), timeoutDur)
			val s = resp.asInstanceOf[StatusMsg]
			s.problems must be( List("issue_1","issue_2") )
			metal2.problems.complain("issue_3")
			val resp2 = Await.result( metal2.actorSystem.actorFor(metal2.akkaUri("metal")) ? Ping(Map[String,Any]()), timeoutDur)
			val s2 = resp2.asInstanceOf[StatusMsg]
			s2.problems must be( List("issue_3") )
		}
		it("""Must propagate the Ping message's payload to the named extension""") {
			Await.result( metal2.actorSystem.actorFor(metal2.akkaUri("metal")) ? Ping(Map("A"->"Hello, A")), timeoutDur)
			Thread.sleep(500)  // Let the Future finish!
			metal2.getExt("A").get.asInstanceOf[ExtA].pay must equal( Some("Hello, A") )
		}
		it("""Must behave if the Ping message's payload references a non-existant extension""") {
			val resp = Await.result( metal2.actorSystem.actorFor(metal2.akkaUri("metal")) ? Ping(Map("X"->"Blah")), timeoutDur)
			val s = resp.asInstanceOf[StatusMsg]
			s.ready must be (true)
			s.problems must be( List[String]() )
		}
	}

	override def afterAll(configMap: org.scalatest.ConfigMap) {
		metal1.shutdown
		metal2.shutdown
		metal3.shutdown
	}
}

case class MyMetal( val basicConfig : Config ) extends Metal {
	override def configLoader = TestConfigLoader()
}

trait T_Ext extends Extension {
	var loadedConfig : Option[Config] = None
	override def _init( config:Config, metal:Metal ) { super._init(config,metal); loadedConfig = Some(config) }
	override def readyMe = true
}
class ExtA() extends T_Ext {
	val name = "A"
	var pay : Option[String] = None
	def bingo = loadedConfig.get getString "nubilus.bingo"
	var readyWork = false
	override def status( p:Any ) { pay = Some(p.asInstanceOf[String]) }
	override def readyMe = {
		if( !readyWork ) Thread.sleep(2000)
		readyWork = true
		true
	}
}
class ExtB() extends T_Ext {
	val name = "B"
	def bingo = loadedConfig.get getString "nubilus.bingo"
}

case class TestConfigLoader() extends ConfigLoader {
	def load( cfgName:String ) = cfgName match {
		case "config/A.conf" => ConfigFactory.parseString("""A {
					depends_on = []
					foo = bar
				}
				nubilus {
					bingo = cat
				}
			""")
		case "config/B.conf" => ConfigFactory.parseString("""B {
					depends_on = [A]
					here = there
				}
				nubilus {
					bingo = fish
				}
			""")
		case "config/C.conf" => ConfigFactory.parseString("""C {
					depends_on = []
				}
			""")
		case "config/D.conf" => ConfigFactory.parseString("""D {
					depends_on = []
				}
			""")
		case "config/E.conf" => ConfigFactory.parseString("""E {
					depends_on = []
				}
			""")
		case "config/F.conf" => ConfigFactory.parseString("""F {
					depends_on = [D,E]
				}
			""")
		case "config/Bogus.conf" => ConfigFactory.parseString("""Bogus {
					depends_on = [foobar]
				}
			""")
	}
}

// S.D.G.
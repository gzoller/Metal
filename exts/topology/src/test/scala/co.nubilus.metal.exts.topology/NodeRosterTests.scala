// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.topology
package test

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import Data._

class NodeRosterTests extends FunSpec with MustMatchers with GivenWhenThen with BeforeAndAfterAll {

	def config1( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "5 seconds"
		                        extensions     = ["co.nubilus.metal.exts.topology.TopologyExt"]
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

    val metal = MyMetal( ConfigFactory.parseString( config1(8811) ) )
    val nr    = metal.getExt("topology").get.asInstanceOf[TopologyExt].nodeRoster

	describe("==============================\n| --   NodeRoster Tests   -- |\n==============================") {
		it("Must load and update instance set changes (NodeRoster)") {
			nr.reset
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
		}
		it("Must filter out a node that stops running") {
			Given("A set of detected nodes")
			nr.reset
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)

			When("aws detects one of the nodes stopped")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4_stopped,ii_5,ii_6) )

			Then("NodeRoster must filter out the node that stopped")
			nr.probablyAlive.size must equal(4)
			nr.probablyAliveIps must not contain("100.01.01.103")
		}
		it("Must filter out a node that is thought un-responsive") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
			When("Someone marks a node as probably-dead (non-responsive)")
			nr.markNonresponsive("100.01.01.105")
			nr.probablyAlive.size must equal(4)
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			Then("We must remove the probably-dead node from the roster and mark it \"sketchy\"")
			nr.probablyAlive must have size(4)
			nr.iffy.head._1 must equal( "100.01.01.105" )
			And("We notify the sentinal that aws still thinks this node is alive")
			metal.problems.getAndClear.head must be ("Node 100.01.01.105 appears to be non-responsive (not answering Akka ping).")
			nr.notifyWho.contains("100.01.01.105") must be ( true )
		}
		it("Must ignore un-responsive node tracking for a node when aws confirms it's terminated (gone)") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
			When("Someone marks a node as probably-dead (non-responsive)")
			nr.markNonresponsive("100.01.01.105")
			nr.probablyAlive.size must equal(4)
			And("aws confirms the node is actually dead/terminated")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5) )
			nr.probablyAlive.size must equal(4)
			Then("NodeRoster clears the now-confirmed-dead node out of sketchy and cancels notifications to sentinal")
			nr.iffy.size must equal( 0 )
			metal.problems.getAndClear must have size(0)
			nr.notifyWho.size must equal( 0 )
		}
		it("Must ignore un-responsive node tracking for a node when aws confirms it's status is non-running") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
			When("Someone marks a node as probably-dead (non-responsive)")
			nr.markNonresponsive("100.01.01.105")
			And("aws confirms the node is still alive but now not running")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6_stopped) )
			nr.probablyAlive.size must equal(4)
			Then("NodeRoster clears the now-confirmed-dead node out of sketchy and cancels notifications to sentinal")
			nr.iffy.size must equal( 0 )
			metal.problems.getAndClear must have size(0)
			nr.notifyWho.size must equal( 0 )
		}
		it("Must only notify once for an un-responsive node within a window of time") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
			When("Someone marks a node as probably-dead (non-responsive)")
			nr.markNonresponsive("100.01.01.105")
			nr.probablyAlive.size must equal(4)
			And("NodeRoster notifies sentinal aws still thinks this probably-dead node is alive")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.iffy.size must equal( 1 )
			metal.problems.getAndClear.head must equal ( "Node 100.01.01.105 appears to be non-responsive (not answering Akka ping)." )
			nr.notifyWho.size must equal( 1 )
			metal.problems.getAndClear must have size(0) 
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			Then("NodeRoster must not notify the sentinal again about the same node during a given time window")
			nr.probablyAlive.size must equal(4)
			nr.iffy.size must equal( 1 )
			metal.problems.getAndClear must have size(0)  // no new notification sent
			nr.notifyWho.size must equal( 1 )
		}
		it("Must clear out both sketchy and notify list node entries when they expire") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.probablyAlive.size must equal(5)
			When("Someone marks a node as probably-dead (non-responsive)")
			nr.markNonresponsive("100.01.01.105")
			nr.probablyAlive.size must equal(4)
			And("NodeRoster notifies sentinal aws still thinks this probably-dead node is alive")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			nr.iffy.size must equal( 1 )
			metal.problems.getAndClear.head must equal ( "Node 100.01.01.105 appears to be non-responsive (not answering Akka ping)." )
			nr.notifyWho.size must equal( 1 )
			And("There's no further activity on that node within a time window -- possibly it started responding again")
			Thread.sleep(2200)
			metal.problems.getAndClear must have size(0)  // no new notification sent
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			Then("NodeRoster must clear the sketchy/notified lists and treat the node normally again")
			nr.probablyAlive must have size(5)  // everything expired and all forgiven unless declared probably-dead again
			nr.iffy must have size( 0 )
			metal.problems.getAndClear must have size(0)  // no new notification sent
			nr.notifyWho must have size( 0 )
		}
		it("Must successfully add a node to the roster if it starts running (status change)") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6_stopped) )
			nr.probablyAlive.size must equal(4)
			When("aws detects a previously-stopped node is now running")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			Then("NodeRoster must recognize the new node and add it to the roster")
			nr.probablyAlive.size must equal(5)
			nr.iffy must have size( 0 )
			metal.problems.getAndClear must have size(0)  // no new notification sent
			nr.notifyWho must have size( 0 )
		}
		it("Must successfully add a new node to the roster when discovered") {
			Given("A set of detected nodes")
			nr.reset
			metal.problems.getAndClear
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_5,ii_6) )
			nr.probablyAlive.size must equal(4)
			When("aws detects a brand-new, running node")
			nr.newInfo( Set(ii_1,ii_2,ii_3_stopped,ii_4,ii_5,ii_6) )
			Then("NodeRoster must recognize the new node and add it to the roster")
			nr.probablyAlive.size must equal(5)
			nr.iffy must have size( 0 )
			metal.problems.getAndClear must have size(0)  // no new notification sent
			nr.notifyWho must have size( 0 )
		}
	}

	override def afterAll(configMap: org.scalatest.ConfigMap) {
		metal.shutdown
	}
}

case class MyMetal( val basicConfig : Config ) extends Metal {
	override def configLoader = TestConfigLoader()
}

case class TestConfigLoader() extends ConfigLoader {
	def load( cfgName:String ) = cfgName match {
		case "config/topology.conf" => ConfigFactory.parseString("""topology {
					depends_on = []
					presumed_dead = "2 seconds"
				}
			""")
		}
}


// S.D.G.
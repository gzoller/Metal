// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.mongo
package test

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.Await
import akka.actor.ActorSystem
import akka.pattern.ask

class MongoTests extends FunSpec with MustMatchers with GivenWhenThen with BeforeAndAfterAll {

	def config1( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "5 seconds"
		                        extensions     = ["co.nubilus.metal.exts.mongo.MongoExt"]
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

    val metal = MyMetal( ConfigFactory.parseString( config1(8911) ) )
	val timeoutDur = Duration("5 seconds")
	implicit val timeout = akka.util.Timeout( timeoutDur.length, timeoutDur.unit )

	describe("=======================================\n| --   Extension Tests: MongoExt   -- |\n=======================================") {
		it("MongoExtActor must successfully retrieve status details from running mongo instance") {
			val mongoExt = metal.getExt("mongo").get.asInstanceOf[MongoExt]
			mongoExt.setDbIPs( List("localhost") )
			Thread.sleep(3000)  // wait for everything to get started
			mongoExt.getDbInfo must have size( 1 )
			mongoExt.getDbInfo("localhost").get.ok must be( 1 )
		}
		it("Must behave if trying to access a database node that doesn't exist/went bad") {
			val mongoExt = metal.getExt("mongo").get.asInstanceOf[MongoExt]
			mongoExt.setDbIPs( List("1.2.3.4","localhost") )
			Thread.sleep(5500)  // wait for everything to get started
			mongoExt.getDbInfo must have size( 2 )
			mongoExt.getDbInfo("localhost").get.ok must be( 1 )
			mongoExt.getDbInfo("1.2.3.4") must be( None )
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
		case "config/mongo.conf" => ConfigFactory.parseString("""mongo {
					depends_on = []
					mongo_period = "5 seconds"
					mongo_worker_wait = "2 seconds"
				}
			""")
		}
}

// S.D.G.
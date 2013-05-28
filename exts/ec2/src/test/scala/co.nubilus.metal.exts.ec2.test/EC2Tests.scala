// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2
package test

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory }
import exts.topology._

class EC2Tests extends FunSpec with MustMatchers with GivenWhenThen with BeforeAndAfterAll {

	def config1( p:Int ) = """nubilus {
		                        config_prefix  = "config/"
		                        env            = dev
		                        org            = acme
		                        actor_name     = nubilus
		                        ready_wait     = "5 seconds"
		                        extensions     = ["co.nubilus.metal.exts.topology.TopologyExt","co.nubilus.metal.exts.ec2.EC2Ext"]
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

    val metal  = MyMetal( ConfigFactory.parseString( config1(8844) ) )
    val nr     = metal.getExt("topology").get.asInstanceOf[TopologyExt].nodeRoster
    val getter = metal.getExt("ec2").get.asInstanceOf[EC2Ext].instanceGetter().asInstanceOf[Getter]

	describe("=======================\n| --   EC2 Tests   -- |\n=======================") {
		it("Must periodically retrieve AWS instance info and populate NodeRoster (TopologyExt)") {
			Thread.sleep(4400)
			getter.count must be >= (3)
			nr.probablyAlive.map(_.privateIP) must be (Set("10.46.57.57", "10.151.77.204", "10.60.10.3", "10.139.10.117", "10.211.91.245"))
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
		case "config/ec2.conf" => ConfigFactory.parseString("""ec2 {
					depends_on = [topology]
					ec2_period = "2 seconds"
					instance_getter = co.nubilus.metal.exts.ec2.test.Getter
				}
			""")
		}
}

class Getter() extends EC2InstanceGetter {
    var count = 0
    var filePath = "exts/ec2/src/test/resources/ec2"
    def getXML = {
		count += 1
		scala.xml.XML.loadFile( filePath+"_"+count+".xml" )
    }
}


// S.D.G.
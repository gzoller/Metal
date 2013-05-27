// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal

import com.typesafe.config.{ Config, ConfigFactory, ConfigList }
import scala.collection.JavaConversions._
import akka.actor.{ ActorSystem, Props }
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import co.nubilus.util._

trait Metal {

	// ------ Utilities 
	// For testing we run >1 nodes on a physical server, so they share IPs and need different Akka ports.
	private[nubilus] def akkaUri( akkaEpName:String, akkaPort:Int = nettyPort, hostIp:String = myHostname ) = s"""akka://$actorName@$hostIp:$akkaPort/user/$akkaEpName"""	
	private          def dynLoad[T](classNameList:java.util.List[String]) = classNameList.toList.map( cName => Class.forName(cName).newInstance ).toList.asInstanceOf[List[T]]
	private[metal]   val problems      = Problems()
	private[nubilus] def myHostname    = java.net.InetAddress.getLocalHost.getHostAddress

	// ------ Layered Configuration
	private[metal]   val basicConfig   : Config
	private[metal]   def configLoader  : ConfigLoader = BasicConfigLoader() // can override for testing
	private          val configPrefix  = basicConfig getString     "nubilus.config_prefix"
	private[metal]   val config        = ConfigFactory.parseString("akka.remote.netty.hostname = \""+myHostname+"\"").withFallback( basicConfig )

	private[nubilus] val org           = config getString          "nubilus.env"
	private[nubilus] val env           = config getString          "nubilus.org"
	private          val nettyPort     = config getInt             "akka.remote.netty.port"
	private          val actorName     = config getString          "nubilus.actor_name"

	// ------ Metal Extensions
	private          val extClasses    = config getStringList      "nubilus.extensions"
	private[metal]   val readyWait     = Duration(config getString "nubilus.ready_wait")
	private          val exts          = dynLoad[Extension](extClasses)
	private[metal]   def getExt( extName:String ) = exts.find(_.name == extName)

	// ------ Start Principle ActorSystem w/MetalActor
	private[nubilus] implicit val actorSystem  = ActorSystem(actorName, config)
	actorSystem.actorOf(Props(new MetalActor(this)),"metal")

	// ------ Lifecycle -- Load Extensions & Shutdown
	private[metal]   var ready         = false
	exts.foldLeft(config)( (c,ext) => { val blendedConfig = configLoader.load(configPrefix + ext.name + ".conf").withFallback( c ); ext.init(blendedConfig, this) } )
	// Make sure all extensions are ready 
	val extsReady = Dependable.waitForAll(exts,exts,readyWait)
	if( !extsReady._1 ) problems ++= extsReady._2 // oops!  errors on ready
	ready = extsReady._1

	private[metal]   def shutdown {
		exts.foreach( _.shutdown )
		actorSystem.shutdown
	}
}

// Abstracted out for testing--can provide all-strings config
trait ConfigLoader {
	def load( cfgName:String ) : Config
}

case class BasicConfigLoader() extends ConfigLoader {
	def load( cfgName:String ) = ConfigFactory.load( cfgName )
}

// S.D.G.
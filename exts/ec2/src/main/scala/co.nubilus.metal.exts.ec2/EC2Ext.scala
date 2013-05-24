// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2

import akka.actor.Props
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class EC2Ext() extends Extension {

	val name    = "ec2"
	def isReady = true

	private[mongo] val dbIPs  = scala.collection.mutable.ListBuffer[String]()
	private[mongo] val dbInfo = scala.collection.mutable.Map[String,Option[MongoInfo]]() 

	private[metal] def getDbInfo = dbInfo.toMap
	private[metal] def setDbIPs( ips : List[String] ) = { dbIPs.clear; dbIPs ++= ips }

	override def init( config:Config, metal:Metal ) : Config = {
		super.init(config,metal)
		val mongoPeriod     = Duration(config getString "ec2.ec2_period")
		// Fire up the periodic ec2-status-getter
		metal.actorSystem.scheduler.schedule( 
			FiniteDuration(2L,"seconds"), // let things settle on startup
			FiniteDuration(ec2Period.length,ec2Period.unit),
			metal.actorSystem.actorOf(Props(new EC2ExtActor(this)),"ext.ec2"),
			"query"
			)
		config
	}
}

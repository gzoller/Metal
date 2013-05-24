// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.mongo

import akka.actor.Props
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class MongoExt() extends Extension {

	val name    = "mongo"
	def isReady = dbInfo.size > 0

	private[mongo] val dbIPs  = scala.collection.mutable.ListBuffer[String]()
	private[mongo] val dbInfo = scala.collection.mutable.Map[String,Option[MongoInfo]]() 

	private[metal] def getDbInfo = dbInfo.toMap
	private[metal] def setDbIPs( ips : List[String] ) = { dbIPs.clear; dbIPs ++= ips }

	override def init( config:Config, metal:Metal ) : Config = {
		super.init(config,metal)
		val mongoPeriod     = Duration(config getString "mongo.mongo_period")
		val mongoWorkerWait = Duration(config getString "mongo.mongo_worker_wait") // how long to wait until all status workers return/time-out
		// Fire up the periodic mongo-status-getter
		metal.actorSystem.scheduler.schedule( 
			FiniteDuration(2L,"seconds"), // let things settle on startup
			FiniteDuration(mongoPeriod.length,mongoPeriod.unit),
			metal.actorSystem.actorOf(Props(new MongoExtActor(this,mongoWorkerWait)),"ext.mongo"), 
			"query"
			)
		config
	}
}

// This object is a subset of available status infomration from MongoDB
case class MongoInfo(
	uptime      : Int,
	connections : Connections,
	ok          : Int
	)

case class Connections (
	current     : Int,
	available   : Int
	)
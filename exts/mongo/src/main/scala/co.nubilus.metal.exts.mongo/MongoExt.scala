// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.mongo

import akka.actor.{ Props, ActorRef } 
import akka.pattern.ask
import akka.pattern.Patterns._
import com.typesafe.config.Config
import concurrent.duration._
import concurrent.{ Await, Future }
import concurrent.Future.{ sequence, firstCompletedOf }
import concurrent.ExecutionContext.Implicits.global
import co.nubilus.util._
import collection.mutable.{ ListBuffer, Map => MMap }

class MongoExt() extends Extension {

	val name    = "mongo"
	def readyMe = {
		if( _ready.isSet )
			_ready()
		else {
			// Make sure someone's set the list of db IPs (may be set by another extension, so be patient...)
			val timeoutFuture = after(FiniteDuration(mongoInitWait().length,mongoInitWait().unit), bareMetal.actorSystem.scheduler, global.prepare, Future.successful(false))
			val ensureListSet = Future {
				while( !dbIPsSet.isSet )
					Thread.sleep(250) // a little polling...
				true
			}
			firstCompletedOf(Seq(ensureListSet, timeoutFuture)).onSuccess({
				case true  => {
					implicit val timeout = akka.util.Timeout( 10L, java.util.concurrent.TimeUnit.MINUTES )
					mongoActor() ! "query"
				}
				case false => {
					bareMetal.ready = false
					bareMetal.problems.complain("Mongo extension did not have its dbIPs set in a reasonable time.")
				}
				})
			true
		}
	}

	private[mongo] val dbIPs         = ListBuffer[String]()
	private        val dbIPsSet      = new SetOnce[Boolean]
	private        val mongoInitWait = new SetOnce[Duration]
	private[mongo] val _ready        = new SetOnce[Boolean]
	private[mongo] val dbInfo        = MMap[String,Option[MongoInfo]]()
	private        val mongoActor    = new SetOnce[ActorRef] 

	private[metal] def getDbInfo  = dbInfo.toMap
	private[metal] def setDbIPs( ips : List[String] ) = { 
		dbIPs.clear; 
		dbIPs ++= ips 
		if( !dbIPsSet.isSet ) {
			implicit val dbIPsSetCredential = dbIPsSet.allowAssignment
			dbIPsSet := true
		}
	}

	override def _init( config:Config, metal:Metal ) = {
		val mongoPeriod     = Duration(config getString "mongo.mongo_period")
		implicit val mongoInitWaitCredential = mongoInitWait.allowAssignment
		mongoInitWait      := Duration(config getString "mongo.mongo_init_wait")
		val mongoWorkerWait = Duration(config getString "mongo.mongo_worker_wait") // how long to wait until all status workers return/time-out
		// Fire up the periodic mongo-status-getter
		implicit val mongoActorCredential = mongoActor.allowAssignment
		mongoActor := metal.actorSystem.actorOf(Props(new MongoExtActor(this,mongoWorkerWait)),"ext.mongo")
		metal.actorSystem.scheduler.schedule( 
			FiniteDuration(mongoPeriod.length,mongoPeriod.unit),
			FiniteDuration(mongoPeriod.length,mongoPeriod.unit),
			mongoActor(), 
			"query"
			)
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
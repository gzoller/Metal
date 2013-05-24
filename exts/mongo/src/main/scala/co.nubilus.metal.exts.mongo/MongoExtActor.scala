// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.mongo

import akka.actor.Actor
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import com.mongodb.casbah.{ MongoDB, MongoConnection }
import com.novus.salat._
import scala.concurrent.duration._

// set implicit context for Grater
package object context {
	val CustomTypeHint = "_t"
	implicit val ctx = new Context {
		val name = "JsonContext-As-Needed"
		override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = CustomTypeHint)
	}
}
import context._

class MongoExtActor( me:MongoExt, mongoWorkerWait:Duration ) extends Actor {
	val g = grater[MongoInfo]
	def receive = {
		// No need for a Future around this because there's no reply, hence no one is waiting anyway.
		case "query" => {
			val ipList = me.dbIPs.toList

			// send out parallel requests
			val results = Await.result( Future.sequence(for( ip <- ipList ) yield { Future(inquireDb(ip)) }), mongoWorkerWait )
			me.dbInfo.clear
			me.dbInfo ++= ipList.zip( results )
		}
	}

	def inquireDb( ip:String ) = {
		var conn : MongoConnection = null // rare case this is ok
		var result : Option[MongoInfo] = None
		try {
			if( java.net.InetAddress.getByName(ip).isReachable(1500) ) {
				conn = MongoConnection(ip)
				val resultJS = conn.getDB("admin").command("""serverStatus""").toString  // get stats
				result = Some( g.fromJSON(resultJS) )
			}
		} catch {
			case t:Throwable => 
		} finally {
			if( conn != null ) {
				conn.close
				conn = null
			}
		}
		result
	}
}

// S.D.G.
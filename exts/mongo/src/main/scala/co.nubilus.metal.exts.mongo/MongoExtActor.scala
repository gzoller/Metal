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
		case "query" => Future {
			val ipList = me.dbIPs.toList

			// send out parallel requests
			val results = Await.result( Future.sequence(for( ip <- ipList ) yield { Future(inquireDb(ip)) }), mongoWorkerWait )
			me.dbInfo.clear
			me.dbInfo ++= ipList.zip( results )
			// If this is the first time query ever processed, set our MongoExt to ready
			if( !me._ready.isSet ) {
				implicit val _readyCredential =  me._ready.allowAssignment
				me._ready := true
			}
		}
	}

	def inquireDb( ip:String ) = {
		var conn   : MongoConnection = null // rare case null is ok
		var result : Option[MongoInfo] = None
		try {
			if( java.net.InetAddress.getByName(ip).isReachable(1500) ) {
				conn = MongoConnection(ip)
				val resultJS = conn.getDB("admin").command("""serverStatus""").toString  // get stats
				result = Some( g.fromJSON(resultJS) )
			}
		} catch {
			case t:Throwable => println("Boom! "+t)
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
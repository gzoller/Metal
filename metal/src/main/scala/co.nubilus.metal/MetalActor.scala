// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal

import akka.actor.Actor
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MetalActor( m:Metal ) extends Actor {
	def receive = {
		case p:Ping => {
			Future( p.payload.foreach({ case (extName,v) => m.getExt(extName).fold()(_.status(v)) }) )
			sender ! StatusMsg(m)
		}
	}
}

case class Ping(payload:Map[String,Any])  // payload = map[ ext_name -> object ]

object StatusMsg {
	def apply( m:Metal ) : StatusMsg = new StatusMsg( m.ready, m.getAndClearProblems )
}
class StatusMsg(
	val ready    : Boolean,
	val problems : List[String]
	)

// S.D.G.
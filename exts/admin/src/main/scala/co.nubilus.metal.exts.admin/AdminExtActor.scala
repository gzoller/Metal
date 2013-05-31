// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.admin

import akka.actor.Actor
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try
import exts.topology._
import akka.pattern.ask

class AdminExtActor( me:AdminExt ) extends Actor {
	private  var working              = false
	private  val didntRespondLastTime = collection.mutable.ListBuffer[InstanceInfo]()
	implicit val timeout              = akka.util.Timeout( me.workerTimeout().length, me.workerTimeout().unit )

	def receive = {
		case "challenge" => Future {
			if( !working ) {
				working = true // prevent overlapping challenges
				val nodes = me.topology().byKind.get(NodeKind.NODE).getOrElse(collection.mutable.HashSet[InstanceInfo]())
				val communicators = nodes.map( node => 
					Future {
						val a = me.bareMetal.actorSystem.actorFor(me.bareMetal.akkaUri("metal", hostIp = node.privateIP))
						val status = Try( Await.result( a ? Ping(), me.workerTimeout ).asInstanceOf[StatusMsg] ).toOption
						status.fold[Option[InstanceInfo]](Some(node))({ s =>
							me.bareMetal.problems ++= s.problems
							None
							})
					}
				)
				// Wait for all the communicators then see if any are Some(InstanceInfo)...those ones couldn't be reached.
				noResponse( Await.result( Future.sequence(communicators), Duration.Inf ).filter( _.isDefined ).map( _.get ).toList )
			}
		}
	}

	def noResponse( shyOnes:List[InstanceInfo] ) {
		// If we've already got this one down as a non-responder, time to take action with prejudice (restart the node)
		val (doomed, onNotice) = shyOnes.partition( ii => didntRespondLastTime.contains(ii) )
		doomed.foreach( ii => me.controller.reboot( ii.id ) )
		didntRespondLastTime.clear
		didntRespondLastTime ++= onNotice
	}
}


// S.D.G.
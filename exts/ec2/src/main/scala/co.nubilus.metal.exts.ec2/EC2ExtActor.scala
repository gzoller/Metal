// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2

import akka.actor.Actor
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import com.mongodb.casbah.{ MongoDB, MongoConnection }
import com.novus.salat._
import scala.concurrent.duration._
import scala.util.Try
import context._

class EC2ExtActor( me:EC2Ext ) extends Actor {
	def receive = {
		// No need for a Future around this because there's no reply, hence no one is waiting anyway.
		case "query" => {
			// Do stuff to read cloud here...
			val worked = Try( me.topology().newInfo( me.instanceGetter().getInstances.filter(n => n.org.getOrElse("") == me.bareMetal.org && n.env.getOrElse("") == me.bareMetal.env) ) ).toOption

			// If this is the first time query ever processed, set our EC2Ext to ready
			if( worked.isDefined && !me._ready.isSet ) {
				implicit val _readyCredential =  me._ready.allowAssignment
				me._ready := true
			}
		}
	}
}

// S.D.G.
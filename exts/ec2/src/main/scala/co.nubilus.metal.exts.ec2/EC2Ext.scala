// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2

import akka.actor.{ Props, ActorRef }
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import co.nubilus.util._
import exts.topology._

class EC2Ext() extends Extension {

	val name    = "ec2"
	def readyMe = {
		if( _ready.isSet )
			_ready()
		else {
			implicit val timeout = akka.util.Timeout( 5L, java.util.concurrent.TimeUnit.SECONDS )
			ec2Actor() ! "query"
			true
		}
	}

	private[ec2] val _ready         = new SetOnce[Boolean]
	private      val ec2Actor       = new SetOnce[ActorRef] 
	private[ec2] val instanceGetter = new SetOnce[InstanceGetter]
	private[ec2] val nodeRoster     = new SetOnce[NodeRoster]

	override def _init( config:Config, metal:Metal ) = {
		// Configure ourselves
		implicit val ec2ActorCredential       = ec2Actor.allowAssignment
		implicit val instanceGetterCredential = instanceGetter.allowAssignment
		implicit val nodeRosterCredential     = nodeRoster.allowAssignment
		val ec2Period     = Duration(config getString "ec2.ec2_period")
		instanceGetter   := Class.forName( (config getString "ec2.instance_getter") ).newInstance.asInstanceOf[InstanceGetter]
		nodeRoster       := metal.getExt("topology").get.asInstanceOf[TopologyExt].nodeRoster
		ec2Actor         := bareMetal.actorSystem.actorOf(Props(new EC2ExtActor(this)),"ext.ec2")

		// Fire up the periodic ec2-status-getter
		metal.actorSystem.scheduler.schedule( 
			FiniteDuration(ec2Period.length,ec2Period.unit),
			FiniteDuration(ec2Period.length,ec2Period.unit),
			ec2Actor(),
			"query"
			)
	}
}

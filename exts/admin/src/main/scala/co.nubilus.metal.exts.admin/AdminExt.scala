// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.admin

import akka.actor.{ Props, ActorRef }
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import co.nubilus.util._
import exts.topology._

class AdminExt() extends Extension {

	val name    = "admin"
	def readyMe = true

	// private[ec2] val _ready         = new SetOnce[Boolean]
	private        val adminActor     = new SetOnce[ActorRef] 
	// private[ec2] val instanceGetter = new SetOnce[InstanceGetter]
	private[admin] val topology       = new SetOnce[Topology]
	private[admin] val workerTimeout  = new SetOnce[Duration]
	private[admin] val controller     = new SetOnce[Controller]

	override def _init( config:Config, metal:Metal ) = {
		// Configure ourselves
		implicit val adminActorCredential     = adminActor.allowAssignment
		implicit val topologyCredential       = topology.allowAssignment
		implicit val controllerCredential     = controller.allowAssignment
		implicit val workerTimeoutCredential  = workerTimeout.allowAssignment
		val adminPeriod   = Duration(config getString "admin.admin_period")
		workerTimeout    := Duration(config getString "admin.worker_timeout")
		topology         := TopologyExt.getTopo(metal)
		controller       := Class.forName( (config getString "ec2.controller") ).newInstance.asInstanceOf[Controller]
		adminActor       := bareMetal.actorSystem.actorOf(Props(new AdminExtActor(this)),"ext.admin")

		// Fire up the periodic ec2-status-getter
		metal.actorSystem.scheduler.schedule( 
			FiniteDuration(2L,"seconds"),
			FiniteDuration(adminPeriod.length,adminPeriod.unit),
			adminActor(),
			"challenge"
			)
	}
}

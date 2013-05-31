// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.topology

import com.typesafe.config.Config
import scala.concurrent.duration._
import co.nubilus.util._

class TopologyExt() extends Extension {

	val name    = "topology"
	def readyMe = true

	private[metal] val topology = new SetOnce[Topology]

	override def _init( config:Config, metal:Metal ) = {
		// Configure ourselves
		implicit val topologyCredential = topology.allowAssignment
		val presumedDead  = Duration(config getString "topology.presumed_dead")
		topology         := Topology(presumedDead, metal.problems)
	}
}

object TopologyExt {
	def getTopo( m:Metal ) = m.getExt("topology").get.asInstanceOf[TopologyExt].topology
}

// S.D.G.
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

	private[metal] val nodeRoster = new SetOnce[NodeRoster]

	override def _init( config:Config, metal:Metal ) = {
		// Configure ourselves
		implicit val nodeRosterCredential = nodeRoster.allowAssignment
		val presumedDead  = Duration(config getString "topology.presumed_dead")
		nodeRoster       := NodeRoster(presumedDead, metal.problems)
	}
}

// S.D.G.
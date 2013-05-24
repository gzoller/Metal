package co.nubilus.container.level0

// Copyright (c) 2013 Greg Zoller
// All rights reserved.

import java.net.InetAddress

case class Topology( alive:Set[InstanceInfo] ) {

	private def myHostIp = InetAddress.getLocalHost.getHostAddress
	private def myNode   = alive.find( _.privateIp == myHostIp )

	// Produces Map[ zone -> Map[ kind -> InstanceInfo ] ]
	private val collated = alive.groupBy(_.zone).map( { case (k,v) => (k,v.groupBy(_.kind)) } )

	def inMyZone( kind:NodeKind.Value ) = myNode.fold( List[InstanceInfo]() )(me => collated.get(me.zone).fold(List[InstanceInfo]())(_.get(kind).fold(List[InstanceInfo]())(_.toList) ))
}

// S.D.G.
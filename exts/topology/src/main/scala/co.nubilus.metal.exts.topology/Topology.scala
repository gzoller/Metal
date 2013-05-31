// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.topology

import java.util.Date
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import co.nubilus.util._

case class Topology( presumedDead:Duration, problems:Problems ) {

	// Concurrency-Safe Mutable Collection
	private val alive    = new scala.collection.mutable.HashSet[InstanceInfo] with scala.collection.mutable.SynchronizedSet[InstanceInfo]
	private val sketchy  = TrieMap[String, Date]()  // Node IP -> Date we noticed it non-responsive
	private val notified = new scala.collection.mutable.HashSet[String] with scala.collection.mutable.SynchronizedSet[String]

	// Replace contents of alive with contents of ii
	private[metal] def newInfo( ii : Set[InstanceInfo] ) {
		// Clean out sketchy and notified of old entries
		val timeAgo = (new Date()).getTime - presumedDead.toMillis
		sketchy.retain( (k,v) => v.getTime > timeAgo ) // clear out old entries--irrelevant now
		notified --= notified.diff(sketchy.keys.toSet)

		// Remove any non-running instances from list
		val runningOnly = ii.filterNot( info => info.isRunning == false )

		// Clear sketchy of entries that don't matter anymore
		val newRunningIps = runningOnly.map(_.privateIP)
		val curRunningIps = alive.map(_.privateIP)
		val sketchyGone   = sketchy.keys.toList.diff(newRunningIps.toList)
		val goneAway = curRunningIps.diff( newRunningIps ) ++ sketchyGone // remove these from sketchy--they're dead according to aws
		sketchy  --= goneAway
		notified --= goneAway

		// Debug
		// println("--------- Run ")
		// println("    New Running: "+newRunningIps)
		// println("    Cur Running: "+curRunningIps)
		// println("    Gone Away  : "+goneAway)
		// println("    Sketchy    : "+sketchy)
		// println("    Notified   : "+notified)

		// Need to notify anyone?
		sketchy.keys.toSet.intersect( newRunningIps ).foreach( stillHere => { // non-responsive nodes aws still thinks are alive
			if( !notified.contains( stillHere ) ) {
				notified += stillHere
				problems.complain( s"Node $stillHere appears to be non-responsive (not answering Akka ping)." )
			}
		})

		// Replace alive content, filtering supposedly-alive nodes that aren't responsive, and any others that aren't running
		alive.clear
		alive ++= runningOnly.filterNot( ii => sketchy.contains(ii.privateIP) )
	}

	def probablyAlive = alive.toSet // Return an immutable copy, please.
	def probablyDead  = sketchy.keys.toSet
	def byKind        = alive.groupBy(_.kind)

	// Keep track of those we detect may be dead (via heartbeat ping)
	def markNonresponsive( nodeIp : String ) {
		// Add to sketchy unless we already know the node is dead from aws.
		alive.find(_.privateIP == nodeIp).fold()(suspect => {
			sketchy.get( nodeIp ).fold( sketchy.put(nodeIp, new Date()) )(x => None)
			alive.remove(suspect)
			})
	}

	def confirmAlive( nodeIp : String ) {
		sketchy -= nodeIp
		notified -= nodeIp
	}

	def refresh { newInfo(probablyAlive) }  // usually called after processing an activity message w/several confirmAlive/probablyDead calls

	// For testing only.
	private[metal] def reset            = { sketchy.clear; notified.clear; alive.clear }
	private[metal] def iffy             = sketchy.toMap
	private[metal] def notifyWho        = notified.toSet
	private[metal] def probablyAliveIps = probablyAlive.map(_.privateIP)
}

// S.D.G.
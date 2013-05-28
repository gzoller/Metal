// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.topology

import scala.util.Try

case class InstanceInfo(
	id            :String,
	name          :Option[String],
	org           :Option[String],
	env           :Option[String],
	isRunning     :Boolean,
	kind          :NodeKind.Value,
	instType      :String,
	zone          :String,
	launchTime    :String,
	privateIP     :String
	)

object NodeKind extends Enumeration {
  val MDB, NODE, WEB, ADMIN, OTHER = Value
  def fromString( n:String ) = Try(withName(n)).toOption.getOrElse(OTHER)
}

// S.D.G.
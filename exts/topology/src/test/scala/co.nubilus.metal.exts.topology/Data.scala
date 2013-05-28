// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.topology

package test

object Data {
/*
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
		privateIp     :String
		)
*/

	val ii_1 = InstanceInfo(
		"node_1",
		None,
		Some("acme"),
		Some("dev"),
		true,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.100"
		)
	val ii_2 = InstanceInfo(
		"node_2",
		None,
		Some("acme"),
		Some("dev"),
		true,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.101"
		)
	val ii_3_stopped = InstanceInfo(
		"node_3",
		None,
		Some("acme"),
		Some("dev"),
		false,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.102"
		)
	val ii_4 = InstanceInfo(
		"node_4",
		None,
		Some("acme"),
		Some("dev"),
		true,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.103"
		)
	val ii_4_stopped = ii_4.copy(isRunning=false)
	val ii_5 = InstanceInfo(
		"node_5",
		None,
		Some("acme"),
		Some("dev"),
		true,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.104"
		)
	val ii_6 = InstanceInfo(
		"node_6",
		None,
		Some("acme"),
		Some("dev"),
		true,
		NodeKind.NODE,
		"mid",
		"eastern",
		"",
		"100.01.01.105"
		)
	val ii_6_stopped = ii_6.copy(isRunning=false)
}

// S.D.G.
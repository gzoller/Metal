// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2

import scala.xml._
import scala.util.Try
import exts.topology._

trait InstanceGetter {
	def getInstances : Set[InstanceInfo]
}

trait EC2InstanceGetter extends InstanceGetter {

	override def getInstances = read( getXML )

	protected def getXML : Elem

	private def read( ec2Xml:Elem ) : Set[InstanceInfo] = {
		(ec2Xml \ "reservationSet" \ "item").map( chunk => {
			(chunk \ "instancesSet" \ "item").map( instance => {
				val id         = (instance \ "instanceId").text
				val isRunning  = (instance \ "instanceState" \ "name").text == "running"
				val ip         = (instance \ "privateIpAddress").text
				val kind       = (instance \ "groupSet" \ "item" \ "groupName").text
				val instType   = (instance \ "instanceType").text
				val zone       = (instance \ "placement" \ "availabilityZone").text
				val launchTime = (instance \ "launchTime").text
				val name       = {
						val nameTag = (instance \ "tagSet" \ "item").collect{
							case c if( c \ "key").text == "Name" => (c \ "value").text
						}
						if( nameTag.size == 1) Some(nameTag.head) else None
					}
				val org        = {
						val orgTag = (instance \ "tagSet" \ "item").collect{
							case c if( c \ "key").text == "Org" => (c \ "value").text
						}
						if( orgTag.size == 1) Some(orgTag.head) else None
					}
				val env        = {
						val envTag = (instance \ "tagSet" \ "item").collect{
							case c if( c \ "key").text == "Env" => (c \ "value").text
						}
						if( envTag.size == 1) Some(envTag.head) else None
					}
				InstanceInfo(id,name,org,env,isRunning,NodeKind.fromString(kind),instType,zone,launchTime,ip)
				}).toSet
			}).toSet.flatten
	}
}

case class EC2Instance() extends EC2InstanceGetter {
	def getXML = XML.loadString( Util.runCmd("aws din") )
}

// S.D.G.

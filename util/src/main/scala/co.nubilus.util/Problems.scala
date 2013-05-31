// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util

case class Problems() {
	private val probs = collection.mutable.Queue[String]()

	def complain( p:String ) = probs.enqueue(p)
	def getAndClear = {
		val plist = probs.toList.distinct
		probs.clear
		plist
	}
	def hasProblems = probs.size > 0

	def ++= ( pb:Problems ) = probs ++= pb.probs
	def ++= ( probList : List[String] ) = probs ++= probList
}

// S.D.G.
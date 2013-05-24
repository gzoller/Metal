// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util

trait Problems {
	private val probs = collection.mutable.Queue[String]()

	def problem( p:String ) = probs.enqueue(p)
	def getAndClear = {
		val plist = probs.toList
		probs.clear
		plist
	}
	def hasProblems = probs.size > 0

	def += ( pb:Problems ) = probs ++= pb.probs
}

// S.D.G.
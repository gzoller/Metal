// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util

/** Support a set-once capability (usually for values that require one-time, post-construction initialization)
  */

import scala.language.implicitConversions

class SetOnce[T] {
	private[this] var value: Option[T] = None
	def isSet = value.isDefined
	def ensureSet { if (value.isEmpty) throwISE("uninitialized value") }
	def apply() = { ensureSet; value.get }
	def :=(finalValue: T)(implicit credential: SetOnceCredential) {
		value = Some(finalValue)
	}
	def allowAssignment = {
		if (value.isDefined) throwISE("final value already set")
			else new SetOnceCredential
	}
	private def throwISE(msg: String) = throw new IllegalStateException(msg)

	class SetOnceCredential private[SetOnce]
}

object SetOnce {
	implicit def unwrap[A](wrapped: SetOnce[A]): A = wrapped()
}

// S.D.G.
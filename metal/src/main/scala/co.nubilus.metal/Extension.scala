// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal

import co.nubilus.util._
import com.typesafe.config.Config
import scala.collection.JavaConversions._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

trait Extension extends Dependable {

	val name      : String
	val bareMetal = new SetOnce[Metal]

	// The idea of init is to set up initialization values, NOT to perform the work of 
	// getting the Extension ready.  The latter will happen when Metal calls the readyMe
	// (via Dependable.waitForAll).
	private[metal] def init( config:Config, metal:Metal ) : Config = {
		implicit val dependentsCredential   = dependents.allowAssignment
		implicit val getDependentCredential = getDependent.allowAssignment
		implicit val bareMetalCredential    = bareMetal.allowAssignment
		dependents   := (config getStringList name+".depends_on").toList
		getDependent := metal.getExt
		bareMetal    := metal
		_init(config, metal)
		config
	}
	protected def _init( config:Config, metal:Metal ) {} // override this one if needed

	// If the administrative Ping message includes a payload for a named extension, this function is called,
	// passing the payload in (must be cast to something meaningful).  No return is allowed because MetalActor
	// responds with its own reply.
	private[metal] def status( payload:Any ){}

	private[metal] def shutdown {}  // any shutdown cleanup here
}

// S.D.G.
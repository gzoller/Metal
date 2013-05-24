// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal

import com.typesafe.config.Config
import scala.collection.JavaConversions._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

trait Extension {
	//-------- Your Extension Implements or can Override These ----------
	val name     : String
	private[metal] def init( config:Config, metal:Metal ) : Future[Boolean]

	// If the administrative Ping message includes a payload for a named extension, this function is called,
	// passing the payload in (must be cast to something meaningful).  No return is allowed because MetalActor
	// responds with its own reply.
	private[metal] def status( payload:Any ){}

	private[metal] def shutdown {}  // any shutdown cleanup here

	//-------- You can, but shouldn't have to, override any of the following... ----------
	private[metal] def _init( config:Config, metal:Metal ) : Config = {

		val deps = (config getStringList name+".depends_on").map( dep => metal.getExt(dep) )

		// Confirm all needed extensions are present
		_ready = deps.foldLeft(true)( (exists,dep) => exists && dep.isDefined ) match {
			case false => {
				_errMsg = "Extension ["+name+"] depends on undefined extensions."
				Future.successful(false)
				}
			// Wait for all dependent exts to be ready then put our Future on the pile
			case true  => {
				println("--- True ---")
				val z = Await.result(Future.sequence(deps.map( d => d.get.isReady )), metal.readyWait ).foldLeft(true)( (a,b) => a && b ) match {
					case true  => {
						println(s"Deps for ext $name are ready")
						init(config,metal)
					}
					case false => {
						println(s"Deps for ext $name are NOT ready")
						_errMsg = "Extension ["+name+"]'s' dependencies did not become ready in time."
						Future.successful(false)
						}
					}
				}
		}
		config
	}
	private var _ready  = Future.successful(false)
	private var _errMsg = ""

	private[metal] def isReady = _ready
	private[metal] def error   = _errMsg
}

// S.D.G.
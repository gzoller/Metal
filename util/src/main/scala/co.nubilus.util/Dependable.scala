// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util

/** This trait is desinged to be mixed-in with anything needing dependency management where
  * modules can depend on one another and require time to initialize.  This module waits (time-
  * bounded) for the dependencies of module 'A' to initialize before initializing 'A'.
  */

import concurrent.duration._
import concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.pattern.Patterns._
import concurrent.Future.{ sequence, firstCompletedOf }
import concurrent.{ Await, Future }

trait Dependable {

	// --- Initialized or specified by users
	val name                    : String // intrinsic to implementing class...not typically set via initialization
	val dependents              = new SetOnce[List[String]]
	val getDependent            = new SetOnce[String => Option[Dependable]]

	def readyMe                 : Boolean
	//-------------------------

	def isReady( timeout:Duration )(implicit system:ActorSystem, p:Problems) : Future[Boolean] = Future{
		({ if( dependents().size > 0 )
			Dependable.waitFor( dependents().map(d => getDependent()(d).get), timeout )
			else List(true) } :+ readyMe).reduce(_ && _)
	}

	def undefinedDeps = dependents().collect{
		case d if(!getDependent()(d).isDefined) => d
	}
}

object Dependable {

	private def waitFor( deps:List[Dependable], timeout:Duration )( implicit system:ActorSystem, p:Problems ) = {
		val timeoutFuture = after(FiniteDuration(timeout.length,timeout.unit), system.scheduler, global.prepare, Future.successful(false))
		// Split list into two parts: with, and w/o dependencies:
		val (hasDeps, noDeps) = deps.partition( d => d.dependents.size > 0 )
		// Wait indefinitely for any w/deps (timeouts built into dep-processing)
		val depResult = Await.result( sequence( hasDeps.map( _.isReady( timeout ) ) ), Duration.Inf)
		depResult.zip(hasDeps).foreach({ case (tf,d) => if(!tf) p.complain("Dependable ["+d.name+"] failed/didn't initialize within the timeout window.") })
		// Wait w/timeout for no-deps
		val noDepResult = Await.result( sequence( noDeps.map( nd => firstCompletedOf(Seq(nd.isReady(timeout), timeoutFuture))  ) ), Duration.Inf )
		noDepResult.zip(noDeps).foreach({ case (tf,d) => if(!tf) p.complain("Dependable ["+d.name+"] failed/didn't initialize within the timeout window.") })
		noDepResult ++ depResult
	}

	def waitForAll( deps:List[Dependable], all:List[Dependable], timeout:Duration )( implicit system:ActorSystem ) : (Boolean,Problems) = {
		// Check that all dependencies exist
		implicit val p = new Problems(){}
		if( deps.size == 0 )
			(true,p)
		else {
			val finder  = all.head.getDependent
			val req = all.map( _.dependents ).flatten.distinct
			req.zip( req.map( d => finder(d))).foreach( { case (dName,found) => if(!found.isDefined) p.complain(s"Specified dependency [$dName] was not defined.") } )
			if( p.hasProblems ) 
				(false,p)
			else
				(waitFor( deps, timeout ).reduce(_ && _), p)
		}
	}
}

// S.D.G.

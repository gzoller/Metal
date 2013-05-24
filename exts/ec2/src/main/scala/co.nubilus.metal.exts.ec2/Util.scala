package co.nubilus.container.level0

// Copyright (c) 2013 Greg Zoller
// All rights reserved.

import scala.sys.process.{Process, ProcessIO}

object Util {

	def runCmd( cmd:String ) : String = Process( cmd ).!!

}

// S.D.G.
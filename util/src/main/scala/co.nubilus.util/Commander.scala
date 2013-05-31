// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.util

import scala.sys.process.Process

/** A trait for system call-outs
  */

trait Commander {

	// Run a system command
	protected def runCmd( cmd:String ) : String = Process( cmd ).!!

}

// S.D.G.
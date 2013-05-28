// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.ec2

import scala.sys.process.{Process, ProcessIO}

object Util {

	def runCmd( cmd:String ) : String = Process( cmd ).!!

}

// S.D.G.
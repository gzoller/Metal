// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal
package exts.admin

import co.nubilus.util.Commander
import xml._

trait Controller extends Commander {
	def reboot( id:String ) : Elem
	def start ( id:String ) : Elem
	def stop  ( id:String ) : Elem
}

class EC2Controller extends Controller {
	def reboot( id:String ) = XML.loadString( runCmd(s"aws reboot $id") )
	def start ( id:String ) = XML.loadString( runCmd(s"aws start $id" ) )
	def stop  ( id:String ) = XML.loadString( runCmd(s"aws stop $id"  ) )
}

// S.D.G.
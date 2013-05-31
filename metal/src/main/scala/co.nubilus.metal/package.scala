// ---- 
// ----   Copyright (c) 2013 Greg Zoller
// ----   All Rights Reserved
// ---- 

package co.nubilus.metal

import com.novus.salat._

// set implicit context for Grater
package object context {
	val CustomTypeHint = "_t"
	implicit val ctx = new Context {
		val name = "JsonContext-As-Needed"
		override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = CustomTypeHint)
	}
}
import context._

// S.D.G.
import sbt._
import sbt.Keys._

import sbtassembly.Plugin.AssemblyKeys._
import scala.Some
import spray.revolver.RevolverPlugin._

object Build extends Build { 
	
import Dependencies._

	lazy val basicSettings = Defaults.defaultSettings ++ Seq(
		version 					:= "0.1",
		organization 				:= "co.nubilus",
		description 				:= "The mother of all API engines.",
		startYear 					:= Some(2013),
		scalaVersion 				:= "2.10.1",
		parallelExecution in Test 	:= true,
		resolvers ++= Dependencies.resolutionRepos,
		scalacOptions				:= Seq("-feature", "-deprecation", "-encoding", "utf8", "-unchecked"),
		publish 					:= (),
		publishLocal 				:= (),
		assembleArtifact in packageScala := false
// put this one in specific sub-project properties
//		jarName in assembly := "shock.jar"
	)
	
	// configure prompt to show current project
	override lazy val settings = super.settings :+ {
		shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
	}

	lazy val root = Project("root", file("."), settings = basicSettings) aggregate(util)//metal)//,mongoExt)

   //  lazy val metal = Project("metal", file("metal"))
   //       .settings(basicSettings: _*)
   //       .settings(libraryDependencies ++=
			// compile(akka_actor, akka_slf4j, akka_remote, logback) ++
			// test(scalatest)
   //       )

   //  lazy val mongoExt = Project("mongoExt", file("exts/mongo"))
   //       .settings(basicSettings: _*)
   //       .settings(libraryDependencies ++=
			// compile(salat_core, salat_util, akka_actor, akka_slf4j, akka_remote, logback) ++
			// test(scalatest)
   //       ).dependsOn(metal)

	lazy val util = Project("util", file("util"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		)
}

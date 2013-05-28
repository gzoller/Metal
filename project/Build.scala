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
		assembleArtifact in packageScala := false,
		jarName in assembly         := "metal.jar"
	)
	
	// configure prompt to show current project
	override lazy val settings = super.settings :+ {
		shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
	}

	lazy val root = Project("root", file("."), settings = basicSettings ++ sbtassembly.Plugin.assemblySettings) 
		.aggregate(util,metal,mongoExt,topologyExt,ec2Ext)
		.dependsOn(util,metal,mongoExt,topologyExt,ec2Ext)

    lazy val metal = Project("metal", file("metal"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		).dependsOn(util)

    lazy val mongoExt = Project("mongoExt", file("exts/mongo"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(salat_core, salat_util, akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		).dependsOn(metal)

    lazy val topologyExt = Project("topologyExt", file("exts/topology"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(salat_core, salat_util, akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		).dependsOn(metal)

	lazy val ec2Ext = Project("ec2Ext", file("exts/ec2"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(salat_core, salat_util, akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		).dependsOn(metal,topologyExt)

	lazy val util = Project("util", file("util"))
		.settings(basicSettings: _*)
		.settings(libraryDependencies ++=
			compile(akka_actor, akka_slf4j, akka_remote, logback) ++
			test(scalatest)
		)
}

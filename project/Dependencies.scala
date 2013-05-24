import sbt._

object Dependencies {

	val resolutionRepos = Seq(
		"Typesafe Repo" 			at "http://repo.typesafe.com/typesafe/releases/",
		//"Typesafe Spapshot Repo" 	at "http://repo.typesafe.com/typesafe/snapshots/",
		"Typesafe Repo2"            at "http://repo.typesafe.com/typesafe/simple/ivy-releases/",
		"Spray Repo" 				at "http://repo.spray.io/",
		"Scala Tools"				at "http://scala-tools.org/repo-snapshots/",
		"OSS"               		at "http://oss.sonatype.org/content/repositories/releases"
	)

	def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile"  )
	def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided" )
	def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test"     )
	def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime"  )
	def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

	val SPRAY_VER 		= "1.1-M7"
	val AKKA_VER 		= "2.1.4"

	val akka_actor		= "com.typesafe.akka"		% "akka-actor_2.10"		% AKKA_VER
	val akka_slf4j 		= "com.typesafe.akka" 		% "akka-slf4j_2.10"		% AKKA_VER
	val akka_remote		= "com.typesafe.akka" 		% "akka-remote_2.10"	% AKKA_VER
	val spray_client	= "io.spray"				% "spray-client"		% SPRAY_VER
	val spray_routing	= "io.spray"				% "spray-routing"		% SPRAY_VER
	val logback 		= "ch.qos.logback" 			% "logback-classic"		% "1.0.11"
	val salat_core 		= "com.novus" 				% "salat-core_2.10"		% "1.9.2-SPECIAL"
	val salat_util 		= "com.novus" 				% "salat-util_2.10" 	% "1.9.2-SPECIAL"
	val mongo_java 		= "org.mongodb" 			% "mongo-java-driver" 	% "2.10.1"
	val scalaz_core 	= "org.scalaz"				% "scalaz-core_2.10"	% "7.0.0"
	val typesafe_config = "com.typesafe.config"     % "config_2.9.1"        % "0.1.7"
//	val scala_actor 	= "org.scala-lang"			% "scala-actors"		% "2.10.0"	
//	val specs2			= "org.specs2" 				%% "specs2_2.10"		% "1.13"
	val scalatest 		= "org.scalatest" 			% "scalatest_2.10"		% "2.0.M6-SNAP9"
	val slf4j_simple 	= "org.slf4j" 				% "slf4j-simple" 		% "1.6.4"
}
name := "Akka Emailer"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.3"

scalacOptions ++= Seq(
  "-target:jvm-1.8"
  , "-feature"
  , "-unchecked"
  , "-deprecation"
  , "-encoding"
  , "utf8"
  , "-language:existentials"
  , "-language:higherKinds"
  , "-language:implicitConversions"
  , "-Xcheckinit"
  , "-Xfuture"
  , "-Xlint"
  , "-Yno-adapted-args"
  , "-Ywarn-dead-code"
  , "-Ywarn-numeric-widen"
  , "-Ywarn-value-discard"
  , "-Ywarn-unused"
)

val akkaV = "2.5.3"
val mailV = "1.6.0"
val logbackV = "1.2.3"
val rabbitV = "4.1.1"
val scalamockV = "3.5.0"
val scalatestV = "3.0.3"
val slf4jV = "1.7.25"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV withSources() withJavadoc()
  , "com.typesafe.akka" %% "akka-slf4j" % akkaV withSources() withJavadoc()

  , "com.rabbitmq" % "amqp-client" % rabbitV withSources() withJavadoc()

  , "com.sun.mail" % "javax.mail" % mailV withSources() withJavadoc()
  , "javax.mail" % "javax.mail-api" % mailV withSources() withJavadoc()

  , "com.typesafe.play" %% "play-json" % "2.6.2" withSources() withJavadoc()

  , "com.typesafe" % "config" % "1.3.1" withSources() withJavadoc()

  , "org.clapper" %% "scalasti" % "3.0.1" withSources() withJavadoc()

  , "org.slf4j" % "slf4j-api" % slf4jV withSources() withJavadoc()
  , "ch.qos.logback" % "logback-classic" % logbackV withSources() withJavadoc()

  , "com.typesafe.akka" %% "akka-testkit" % akkaV % Test withSources() withJavadoc()
  , "org.scalamock" %% "scalamock-scalatest-support" % scalamockV % Test withSources() withJavadoc()
  , "org.scalatest" %% "scalatest" % scalatestV % Test withSources() withJavadoc()
  , "io.arivera.oss" % "embedded-rabbitmq" % "1.1.2" % Test withSources() withJavadoc()
  , "com.icegreen" % "greenmail" % "1.5.5" % "test"
)

fork in Test := true

assemblyJarName in assembly := "akka-emailer-" + version.value +".jar"

//excludeDependencies ++= Seq(
//  SbtExclusionRule("org.slf4j", "slf4j-log4j12")
//)


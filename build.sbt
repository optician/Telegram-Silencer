val cats = "1.0.0-MF"
val catsDeps = Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-macros",
  "org.typelevel" %% "cats-kernel"
).map(_ % cats)

val utilDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest"  %% "scalatest"      % "3.0.3"
)

lazy val silencer = project
  .in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq("info.mukel" %% "telegrambot4s" % "3.0.6") ++ catsDeps ++ utilDeps)

def commonSettings =
  Seq(name := "TelegramSilencer",
      organization := "me.optician_owl",
      version := "0.1.0",
      scalaVersion := "2.12.3")

lazy val silencer = project
  .in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq("info.mukel"    %% "telegrambot4s" % "3.0.4",
                                        "org.typelevel" %% "cats"          % "0.9.0") ++ utilDeps)

def commonSettings =
  Seq(name := "TelegramSilencer",
      organization := "me.optician_owl",
      version := "0.1.0",
      scalaVersion := "2.12.3")

def utilDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest"  %% "scalatest"      % "3.0.3"
)

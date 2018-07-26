val cats = "1.1.0"
val catsDeps = Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-macros",
  "org.typelevel" %% "cats-kernel"
).map(_ % cats)

val utilDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val silencer = project
  .in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "info.mukel"    %% "telegrambot4s"      % "3.0.15",
    "net.openhft"   % "chronicle-map"       % "3.15.1",
    "com.twitter"   %% "bijection-protobuf" % "0.9.6",
    "org.scalatest" %% "scalatest"          % "3.0.5" % "test"
  ) ++ catsDeps ++ utilDeps)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ))

def commonSettings =
  Seq(name := "TelegramSilencer",
      organization := "me.optician_owl",
      version := "0.1.0",
      scalaVersion := "2.12.6",
      scalacOptions ++= Seq(
        "-deprecation",
        "-Ypartial-unification"
      ))

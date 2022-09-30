enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName      := "fetch"
netLogoClassManager := "org.nlogo.extension.fetch.FetchExtension"
netLogoVersion      := "6.3.0-beta1-184a727"
netLogoTestExtras   += (baseDirectory.value / "lorem-ipsum.txt")

version      := "1.0.5"
isSnapshot   := true
scalaVersion := "2.12.8"

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test    / scalaSource := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq(
  "-deprecation"
, "-unchecked"
, "-Xfatal-warnings"
, "-encoding", "us-ascii"
, "-release", "11"
)

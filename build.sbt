enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName      := "fetch"
netLogoClassManager := "org.nlogo.extension.fetch.FetchExtension"
netLogoVersion      := "7.0.0-internal1"
netLogoTestExtras   += (baseDirectory.value / "lorem-ipsum.txt")

version      := "1.0.5"
isSnapshot   := true
scalaVersion := "2.13.16"

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test    / scalaSource := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq(
  "-deprecation"
, "-unchecked"
, "-Xfatal-warnings"
, "-encoding", "us-ascii"
, "-release", "11"
)

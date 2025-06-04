enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName      := "fetch"
netLogoClassManager := "org.nlogo.extension.fetch.FetchExtension"
netLogoVersion      := "7.0.0-beta1"
netLogoTestExtras   += (baseDirectory.value / "lorem-ipsum.txt")

version      := "1.1.0"
isSnapshot   := true
scalaVersion := "3.7.0"

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test    / scalaSource := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq(
  "-deprecation"
, "-unchecked"
, "-Xfatal-warnings"
, "-encoding", "us-ascii"
, "-release", "17"
)

import org.nlogo.build.NetLogoExtension

enablePlugins(NetLogoExtension)

netLogoExtName      := "fetch"
netLogoClassManager := "org.nlogo.extension.fetch.FetchExtension"
netLogoVersion      := "7.1.0-internal1-0194cd8"
netLogoTestExtras   += (baseDirectory.value / "lorem-ipsum.txt")

version      := "1.1.2"
isSnapshot   := true
scalaVersion := "3.7.0"

Compile / scalaSource := baseDirectory.value / "src" / "main"
Test    / scalaSource := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq(
  "-deprecation"
, "-unchecked"
, "-Xfatal-warnings"
, "-encoding", "us-ascii"
, "-release", "21"
, "-Wunused:linted"
)

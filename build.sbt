name := "K"

version := "3.0"

scalaVersion := "2.11.1"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"

lazy val kToolDirectory = settingKey[File]("K tool directory")

kToolDirectory := baseDirectory.value / "src" / "javasources" / "KTool"

// I'm sneaky and allow Scala

javaSource in Compile := kToolDirectory.value / "src"

scalaSource in Compile := kToolDirectory.value / "src"

// Linking the precompiled jars.
// This will need to be replaced with grabbing them from a remote repo.

unmanagedBase := baseDirectory.value / "lib" / "java"

unmanagedJars in Compile ++= Classpaths.findUnmanagedJars((configuration in Compile).value, kToolDirectory.value / "lib", includeFilter in (Compile, unmanagedJars) value, excludeFilter in (Compile, unmanagedJars) value)

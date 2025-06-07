Compile / javaSource := baseDirectory.value / "src"
Test / javaSource := baseDirectory.value / "test"
Test / parallelExecution := false
libraryDependencies += "net.aichler" % "jupiter-interface" % "0.11.1" % Test
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java


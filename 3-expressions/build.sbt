Compile / javaSource := baseDirectory.value / "src"
Test / javaSource := baseDirectory.value / "test"
Test / parallelExecution := false
libraryDependencies += "com.github.sbt.junit" % "jupiter-interface" % "0.17.0" % Test
libraryDependencies += "org.junit.jupiter" % "junit-jupiter-params" % "6.0.1" % Test
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java


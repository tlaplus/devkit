Compile / javaSource := baseDirectory.value / "src"
Test / javaSource := baseDirectory.value / "test"
libraryDependencies += "net.aichler" % "jupiter-interface" % "0.11.1" % Test

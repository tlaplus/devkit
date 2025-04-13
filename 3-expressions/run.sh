javac com/craftinginterpreters/tool/*.java
java -cp . com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/tla
javac com/craftinginterpreters/tla/*.java
java -cp . com.craftinginterpreters.tla.TlaPlus


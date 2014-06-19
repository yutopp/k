package org.kframework.test

import org.junit.Test
import org.kframework.main.Main
import scala.collection.JavaConversions._
import java.io.File
import scalax.file.ImplicitConversions._
import scalax.file.Path

class KTest {
  @Test def test() {
    val base = Path("").toAbsolute
    
    println(base)
    
    val quick = base / "tests" / "regression" / "quick"

    val output = quick / "out"

    output.createDirectory(failIfExists = false)
    
    kompile(quick / "kool-typed-dynamic.k" , quick / "out")
  }

  def kompile(definition: File, output: File, args: String*) {
    Main.main(Seq("-kompile", definition.toString, "--directory", output.toString)
      ++ args toArray)
  }

  def krun(args: String*) {
    Main.main("-krun" +: args.toArray)
  }
}
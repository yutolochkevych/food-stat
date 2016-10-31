package com.foodtstats

import java.text.NumberFormat

object Main extends App {
  val translate = loadConfiguration(args)
  val runtime: Runtime  = Runtime.getRuntime();

  if (translate) {

    val server = new MockServer()
    server.run()
    implicit val rclient = RestClient()
    val params = new TranslateParameters() {
      override val baseUrl: String = "http://localhost:10000/translate"
    }

    implicit val nullWriter = new  NullBuffer()
    val res = new Translator(params).runFlow(RepeatStrategy(1000))
    if (res) println("translated success")
    server.stop()
  }

  SparkRunner.getStats()

  System.out.println("##### Memory utilization statistics [MB] #####");
  val format: NumberFormat = NumberFormat.getInstance();
  val sb: StringBuilder = new StringBuilder()
  val maxMemory = runtime.maxMemory();
  val allocatedMemory = runtime.totalMemory();
  val  freeMemory = runtime.freeMemory();
  val mb = 1024*1024;

  def loadConfiguration( args: Array[String] ) = {
    if ( args.length > 0 && args(0)=="translate=true") true else false
  }

  println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

  println("Free Memory:" + runtime.freeMemory() / mb);

  println("Total Memory:" + runtime.totalMemory() / mb);

  println("Max Memory:" + runtime.maxMemory() / mb);
}

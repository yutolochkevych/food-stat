package com.foodtstats

import org.apache.commons.lang.StringUtils
import org.apache.spark.{SparkConf, SparkContext}

import org.apache.log4j.Logger
import org.apache.log4j.Level

object SparkRunner {

  def getStats() {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    
    val sparkConf = new SparkConf().
      setMaster("local[4]").
      setAppName("FoodStatsApp")
    val sc = new SparkContext(sparkConf)
    val filePath = getClass.getResource("/Reviews.csv")
    val reviews = sc.textFile(filePath.toString)
    val distinct = reviews.distinct

    val reviewRecords = distinct.map { record =>
      val splitted = record.split(",", -1).toList
      val textReview = record.substring(StringUtils.ordinalIndexOf(record, ",", 9) + 1)
      Review(splitted(1), splitted(3), textReview)
    }

    println("Getting Top 1000 food items:")
    reviewRecords map { r => (r.id, 1) } reduceByKey {
      _ + _
    } sortBy(_._2, false) take 1000 foreach {
      x => println(s"food item: ${x._1} -> comment count: ${x._2}")
    }

    println("Getting Top 1000 most active users")
    reviewRecords groupBy {
      _.profile
    } sortBy(_._2.size, false) take 1000 foreach (
      x => println(s"user: ${x._1} ->   count: ${x._2.size}"))

    println("Getting Top 1000 most used words in reviews")
    reviewRecords.flatMap(rec => rec.text.split("[\\s.]+")).map(
      word => (word, 1)).reduceByKey((a, b) => a + b) sortBy(_._2, false) take 1000 foreach (
      x => println(s"word: ${x._1} ->   count: ${x._2}"))

    sc.stop
  }

}

case class Review(id: String, profile: String, text: String)
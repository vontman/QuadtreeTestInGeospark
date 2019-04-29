package utils

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import com.vividsolutions.jts.index.quadtree.{NodeBase, Quadtree}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.serializer.KryoSerializer
import org.datasyslab.geospark.enums.{GridType, IndexType}
import org.datasyslab.geospark.spatialRDD.PointRDD
import org.datasyslab.geosparkviz.core.Serde.GeoSparkVizKryoRegistrator

import scala.util.Random

object QuadTreeTest {
  def generate(size: Int, range: Int, sc: SparkContext): PointRDD = {
    val geometryFactory = new GeometryFactory()
    val xBoundsMin = Random.nextInt(2 * range / 3)
    val xBoundsMax = xBoundsMin + range
    val yBoundsMin = Random.nextInt(2 * range / 3)
    val yBoundsMax = yBoundsMin + range
    new PointRDD(
      sc.parallelize(
        for {
          _ <- 1 to size
        } yield
          geometryFactory.createPoint(
            new Coordinate(
              (Random.nextGaussian + 4.0) / 8.0 * (xBoundsMax - xBoundsMin) + xBoundsMin,
              (Random.nextGaussian + 4.0) / 8.0 * (yBoundsMax - yBoundsMin) + yBoundsMin
            )),
        16
      ))
  }

  def main(args: Array[String]) = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)
    val conf = new SparkConf()
      .setAppName("GeoSparkRunnableExample")
      .setMaster("local[*]")
    conf.set("spark.serializer", classOf[KryoSerializer].getName)
    conf.set("spark.kryo.registrator",
             classOf[GeoSparkVizKryoRegistrator].getName)

    val sparkContext = new SparkContext(conf)

    val dataRDD = generate(100000, 800000, sparkContext)

    dataRDD.analyze()
    dataRDD.spatialPartitioning(GridType.QUADTREE)
    dataRDD.buildIndex(IndexType.QUADTREE, true)

    println("dataCount: " + dataRDD.spatialPartitionedRDD.rdd.count())

    val countPointsInLeaves = dataRDD.indexedRDD.rdd
      .map(_.asInstanceOf[Quadtree].getRoot)
      .flatMap(node => {
        var leaves = List[NodeBase]()
        var curr = List[NodeBase](node)
        while(curr.exists(_.hasChildren)) {
          leaves = leaves:::curr.filter(!_.hasChildren)
          curr = curr.flatMap(_.getChildren.filter(_ != null))
        }
        leaves
      })
      .map(_.size)
      .sum()
    println("dataCountInLeaves: " + countPointsInLeaves)
  }
}

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.*;
import org.apache.spark.api.java.function.*;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

public class HW6 {

  // the full input data file is at s3://us-east-1.elasticmapreduce.samples/flightdata/input

  /*
    You are free to change the contents of main as much as you want. We will be running a separate main for
     grading purposes.
   */
  public static void main(String[] args) {

    if (args.length < 2)
      throw new RuntimeException("Usage: HW6 <datafile location> <output location>");

    String dataFile = args[0];
    String output = args[1];
    String q = args.length > 2 ? args[2] : "q";

    // turn off logging except for error messages
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR);
    Logger.getLogger("org.apache.spark.storage.BlockManager").setLevel(Level.ERROR);

    // use this for running locally
//    SparkSession spark = SparkSession.builder().appName("HW6").config("spark.master", "local").getOrCreate();

    // use this for running on ec2
     SparkSession spark = SparkSession.builder().appName("HW6").getOrCreate();

//    if (q.equals("q") || q.equals("warmup")) {
//      Dataset<Row> r = warmup(spark, dataFile);
//      r.javaRDD().repartition(1).saveAsTextFile(output + "warmup");
//    }

    // uncomment each of the below to run your solutions

    if (q.equals("q") || q.equals("q1")) {
      /* Problem 1 */
      Dataset<Row> r1 = Q1(spark, dataFile);

      // collect all outputs from different machines to a single partition, and write to the output
      // make sure the output location does not already exists (otherwise it will throw an error)
      r1.javaRDD().repartition(1).saveAsTextFile(output + "q1");

    }

    if (q.equals("q") || q.equals("q2")){
      /* Problem 2 */
      JavaRDD<Row> r2 = Q2(spark, dataFile);
      r2.repartition(1).saveAsTextFile(output + "q2");
    }

    if (q.equals("q") || q.equals("q3")){
      /* Problem 3 */
      JavaPairRDD<Tuple2<String, Integer>, Integer> r3 = Q3(spark, dataFile);
      r3.repartition(1).saveAsTextFile(output + "q3");
    }

    if (q.equals("q") || q.equals("q4")){
      /* Problem 4 */
      Tuple2<String, Integer> r4 = Q4(spark, dataFile);
      spark.createDataset(Collections.singletonList(r4), Encoders.tuple(Encoders.STRING(), Encoders.INT()))
              .javaRDD().saveAsTextFile(output + "q4");
    }

    if (q.equals("q") || q.equals("q5")){
      /* Problem 5 */
      JavaPairRDD<String, Double> r5 = Q5(spark, dataFile);
      r5.repartition(1).saveAsTextFile(output + "q5");
    }
    // this saves the results to an output file in parquet format
    // (useful if you want to generate a test dataset on an even smaller dataset)
    // r.repartition(1).write().parquet(output);

    // shut down
    spark.stop();
  }

  // offsets into each Row from the input data read
  public static final int MONTH = 2;
  public static final int ORIGIN_CITY_NAME = 15;
  public static final int DEST_CITY_NAME = 24;
  public static final int DEP_DELAY = 32;
  public static final int CANCELLED = 47;

  public static Dataset<Row> warmup (SparkSession spark, String dataFile) {

    Dataset<Row> df = spark.read().parquet(dataFile);

    // create a temporary table based on the data that we read
    df.createOrReplaceTempView("flights");

    // run a SQL query
    Dataset<Row> r = spark.sql("SELECT * FROM flights LIMIT 10");

    // this prints out the results
    r.show();

    // this uses the RDD API to project a column from the read data and print out the results
    r.javaRDD()
     .map(t -> t.get(DEST_CITY_NAME))
     .foreach(t -> System.out.println(t));

    return r;
  }

  // Select all flights that leave from Seattle, WA, and return the destination city names.
  // Only return each city name once. Implement this using the Dataset API and writing a SQL query.
  // This should be trivial and is intended for you to learn about the Dataset API.
  // You can either use the functional form (i.e., join(...), select(...)) or write a SQL query using SparkSession.sql.
  // Check the corresponding Spark Javadoc for the parameters and return values.
  // Save the EMR output as q1.txt and add it to your repo. (10 points)
  // [Result Size: 79 rows (50 rows on the small dataset), 10 mins on EMR]
  //
  // Hint: If you decide to write a SQL query, note that you can use single quotes inside your query
  // for string literals (e.g., 'Seattle'). Also, it does not matter what you name the output column as,
  // since that information is not dumped to the output.
  //
  public static Dataset<Row> Q1 (SparkSession spark, String dataFile) {

    Dataset<Row> df = spark.read().parquet(dataFile);

    // create a temporary table based on the data that we read
    df.createOrReplaceTempView("flights");

    // run a SQL query
    Dataset<Row> r = spark.sql("SELECT DISTINCT destcityname FROM flights WHERE origincityname='Seattle, WA'");

    // this prints out the results
    r.show();

    return r;
  }

  // Implement the same query as above, but use the RDD API.
  // You can convert a Dataset to a JavaRDD by calling javaRDD, which we did for you in the skeleton code.
  // Save the EMR output as q2.txt and add it to your repo. (20 points)
  // [Result Size: 79 rows (50 rows on the small dataset), 15 mins on EMR]
  public static JavaRDD<Row> Q2 (SparkSession spark, String dataFile) {

    JavaRDD<Row> d = spark.read().parquet(dataFile).javaRDD();

    JavaRDD<Row> out = d.filter(t -> t.get(ORIGIN_CITY_NAME).equals("Seattle, WA"))
      .map(t -> t.get(DEST_CITY_NAME))
      .distinct()
      .map(t -> RowFactory.create(t));

    out.foreach(t -> System.out.println(t));

    return out;
  }

  // Find the number of non-cancelled flights per month that departs from each city,
  // return the results in a RDD where the key is a pair (i.e., a Tuple2 object),
  // consisting of a String for the departing city name, and an Integer for the month.
  // The value should be the number of non-cancelled flights.
  // Save the EMR output as q3.txt and add it to your repo. (20 points)
  // [Result Size: 4383 rows (281 rows on the small dataset), 17 mins on EMR]
  public static JavaPairRDD<Tuple2<String, Integer>, Integer> Q3 (SparkSession spark, String dataFile) {

    JavaRDD<Row> d = spark.read().parquet(dataFile).javaRDD();

    JavaPairRDD<Tuple2<String, Integer>, Integer> out = d.filter(t -> (int)t.get(CANCELLED) == 0)
            .mapToPair(t -> new Tuple2<Tuple2<String, Integer>, Integer> (
                    new Tuple2<String,Integer>((String)t.get(ORIGIN_CITY_NAME).toString(), (int)t.get(MONTH)), 1))
            .reduceByKey((x,y) -> x + y);

    out.foreach(t -> System.out.println(t));

    return out;
  }

  // Find the name of the city that is connected to the most number of other cities within a single hop flight.
  // Return the result as a pair that consists of a String for the city name,
  // and an Integer for the total number of flights to the other cities.
  // Save the EMR output as q4.txt and add it to your repo. (25 points)
  // [Result Size: 1 row, 19 mins on EMR]
  public static Tuple2<String, Integer> Q4 (SparkSession spark, String dataFile) {

    JavaRDD<Row> d = spark.read().parquet(dataFile).javaRDD();

    List<Tuple2<String,Integer>> out = d.mapToPair(t -> new Tuple2<Tuple2<String, String>, Integer> (
            new Tuple2<String,String>((String)t.get(ORIGIN_CITY_NAME).toString(), (String)t.get(DEST_CITY_NAME).toString()), 1))
            .reduceByKey((x,y) -> 1)
            .mapToPair(t -> new Tuple2<String, Integer> (t._1()._1(), 1))
            .reduceByKey((x,y) -> x+y)
            .takeOrdered(1, MyTupleComparator.INSTANCE);

    System.out.println(out.get(0));

    return out.get(0);
  }

  // Compute the average delay from all departing flights for each city.
  // Flights with null delay values (due to cancellation or otherwise) should not be counted.
  // Return the results in a RDD where the key is a String for the city name,
  // and the value is a Double for the average delay in minutes.
  // Save the EMR output as q5.txt and add it to your repo. (25 points)
  // [Result Size: 383 rows (281 rows on the small dataset), 17 mins on EMR]
  public static JavaPairRDD<String, Double> Q5 (SparkSession spark, String dataFile) {

    JavaRDD<Row> d = spark.read().parquet(dataFile).javaRDD();

    JavaPairRDD<String, Double> out = d.filter(t -> t.get(DEP_DELAY) != null)
            .mapToPair(t -> new Tuple2<String, Tuple2<Double, Double>> (
                    (String)t.get(ORIGIN_CITY_NAME).toString(), new Tuple2<Double, Double> (1.0 * (Integer)t.get(DEP_DELAY), 1.0)))
            .reduceByKey((x, y) -> new Tuple2<Double, Double> ((x._1() + y._1()), (x._2() + y._2())))
            .mapToPair(y -> new Tuple2<String, Double> (y._1(), 1.0 * y._2()._1() / y._2()._2()));

    out.foreach(t -> System.out.println(t));

    return out;
  }

  /* We list all the fields in the input data file for your reference
  root
 |-- year: integer (nullable = true)   // index 0
 |-- quarter: integer (nullable = true)
 |-- month: integer (nullable = true)
 |-- dayofmonth: integer (nullable = true)
 |-- dayofweek: integer (nullable = true)
 |-- flightdate: string (nullable = true)
 |-- uniquecarrier: string (nullable = true)
 |-- airlineid: integer (nullable = true)
 |-- carrier: string (nullable = true)
 |-- tailnum: string (nullable = true)
 |-- flightnum: integer (nullable = true)
 |-- originairportid: integer (nullable = true)
 |-- originairportseqid: integer (nullable = true)
 |-- origincitymarketid: integer (nullable = true)
 |-- origin: string (nullable = true)   // airport short name
 |-- origincityname: string (nullable = true) // e.g., Seattle, WA
 |-- originstate: string (nullable = true)
 |-- originstatefips: integer (nullable = true)
 |-- originstatename: string (nullable = true)
 |-- originwac: integer (nullable = true)
 |-- destairportid: integer (nullable = true)
 |-- destairportseqid: integer (nullable = true)
 |-- destcitymarketid: integer (nullable = true)
 |-- dest: string (nullable = true)
 |-- destcityname: string (nullable = true)
 |-- deststate: string (nullable = true)
 |-- deststatefips: integer (nullable = true)
 |-- deststatename: string (nullable = true)
 |-- destwac: integer (nullable = true)
 |-- crsdeptime: integer (nullable = true)
 |-- deptime: integer (nullable = true)
 |-- depdelay: integer (nullable = true)
 |-- depdelayminutes: integer (nullable = true)
 |-- depdel15: integer (nullable = true)
 |-- departuredelaygroups: integer (nullable = true)
 |-- deptimeblk: integer (nullable = true)
 |-- taxiout: integer (nullable = true)
 |-- wheelsoff: integer (nullable = true)
 |-- wheelson: integer (nullable = true)
 |-- taxiin: integer (nullable = true)
 |-- crsarrtime: integer (nullable = true)
 |-- arrtime: integer (nullable = true)
 |-- arrdelay: integer (nullable = true)
 |-- arrdelayminutes: integer (nullable = true)
 |-- arrdel15: integer (nullable = true)
 |-- arrivaldelaygroups: integer (nullable = true)
 |-- arrtimeblk: string (nullable = true)
 |-- cancelled: integer (nullable = true)
 |-- cancellationcode: integer (nullable = true)
 |-- diverted: integer (nullable = true)
 |-- crselapsedtime: integer (nullable = true)
 |-- actualelapsedtime: integer (nullable = true)
 |-- airtime: integer (nullable = true)
 |-- flights: integer (nullable = true)
 |-- distance: integer (nullable = true)
 |-- distancegroup: integer (nullable = true)
 |-- carrierdelay: integer (nullable = true)
 |-- weatherdelay: integer (nullable = true)
 |-- nasdelay: integer (nullable = true)
 |-- securitydelay: integer (nullable = true)
 |-- lateaircraftdelay: integer (nullable = true)
 |-- firstdeptime: integer (nullable = true)
 |-- totaladdgtime: integer (nullable = true)
 |-- longestaddgtime: integer (nullable = true)
 |-- divairportlandings: integer (nullable = true)
 |-- divreacheddest: integer (nullable = true)
 |-- divactualelapsedtime: integer (nullable = true)
 |-- divarrdelay: integer (nullable = true)
 |-- divdistance: integer (nullable = true)
 |-- div1airport: integer (nullable = true)
 |-- div1airportid: integer (nullable = true)
 |-- div1airportseqid: integer (nullable = true)
 |-- div1wheelson: integer (nullable = true)
 |-- div1totalgtime: integer (nullable = true)
 |-- div1longestgtime: integer (nullable = true)
 |-- div1wheelsoff: integer (nullable = true)
 |-- div1tailnum: integer (nullable = true)
 |-- div2airport: integer (nullable = true)
 |-- div2airportid: integer (nullable = true)
 |-- div2airportseqid: integer (nullable = true)
 |-- div2wheelson: integer (nullable = true)
 |-- div2totalgtime: integer (nullable = true)
 |-- div2longestgtime: integer (nullable = true)
 |-- div2wheelsoff: integer (nullable = true)
 |-- div2tailnum: integer (nullable = true)
 |-- div3airport: integer (nullable = true)
 |-- div3airportid: integer (nullable = true)
 |-- div3airportseqid: integer (nullable = true)
 |-- div3wheelson: integer (nullable = true)
 |-- div3totalgtime: integer (nullable = true)
 |-- div3longestgtime: integer (nullable = true)
 |-- div3wheelsoff: integer (nullable = true)
 |-- div3tailnum: integer (nullable = true)
 |-- div4airport: integer (nullable = true)
 |-- div4airportid: integer (nullable = true)
 |-- div4airportseqid: integer (nullable = true)
 |-- div4wheelson: integer (nullable = true)
 |-- div4totalgtime: integer (nullable = true)
 |-- div4longestgtime: integer (nullable = true)
 |-- div4wheelsoff: integer (nullable = true)
 |-- div4tailnum: integer (nullable = true)
 |-- div5airport: integer (nullable = true)
 |-- div5airportid: integer (nullable = true)
 |-- div5airportseqid: integer (nullable = true)
 |-- div5wheelson: integer (nullable = true)
 |-- div5totalgtime: integer (nullable = true)
 |-- div5longestgtime: integer (nullable = true)
 |-- div5wheelsoff: integer (nullable = true)
 |-- div5tailnum: integer (nullable = true)
   */

  static class MyTupleComparator implements
          Comparator<Tuple2<String, Integer>>, Serializable {
    final static MyTupleComparator INSTANCE = new MyTupleComparator();
    // note that the comparison is performed on the key's frequency
    // assuming that the second field of Tuple2 is a count or frequency
    public int compare(Tuple2<String, Integer> t1,
                       Tuple2<String, Integer> t2) {
      return -t1._2.compareTo(t2._2);    // sort descending
      // return t1._2.compareTo(t2._2);  // sort ascending
    }
  }
}



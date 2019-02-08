import java.io.{BufferedReader, File, FileReader}
import java.util
import java.util.Properties

import com.lucidworks.gatling.solr.Predef._
import io.gatling.core.Predef._
import io.gatling.core.feeder.Feeder
import org.apache.solr.common.SolrInputDocument

import scala.concurrent.duration._


class IndexConstantUsersSimulation extends Simulation {

  object Config {

    import java.io.FileInputStream

    val prop: Properties = new Properties
    val propFile = new FileInputStream("/Users/apple/git_space/gatling-solr-new/gatling-solr/" +
      "src/test/resources/configs/index.config.properties");
    prop.load(propFile)

    val indexFilePath = prop.getProperty("indexFilePath", "/opt/gatling/user-files/" +
      "data/enwiki-20120502-lines-1k.txt")
    val numBatchesPerUser = prop.getProperty("numBatchesPerUser", "1")
    val maxNumUsers = prop.getProperty("maxNumUsers", "1")
    val minNumUsers = prop.getProperty("minNumUsers", "1")
    val totalTimeInMinutes = prop.getProperty("totalTimeInMinutes", "34")
    val indexBatchSize = prop.getProperty("indexBatchSize", "5000")
    val zkHost = prop.getProperty("zkHost", "localhost:9983")
    val solrUrl = prop.getProperty("solrUrl", "http://localhost:8983/solr")
    val defaultCollection = prop.getProperty("defaultCollection", "wiki")
    val header = prop.getProperty("header", "id,title,time,description")
    val headerSep = prop.getProperty("header.sep", ",")
    val fieldValuesSep = prop.getProperty("fieldValues.sep", ",")
    val numClients = prop.getProperty("numClients", "9")

  }

  val solrIndexV2Feeder = new Feeder[util.ArrayList[SolrInputDocument]] {

    private val indexFile = new File(Config.indexFilePath)
    private val fileReader = new FileReader(indexFile)
    private val reader = new BufferedReader(fileReader)

    private var hasNextLine = ""

    override def hasNext = hasNextLine != null

    override def next: Map[String, util.ArrayList[SolrInputDocument]] = {
      var batchSize = Config.indexBatchSize.toInt
      val records = new util.ArrayList[SolrInputDocument]()
      var record = reader.readLine()
      while (batchSize > 0 && record != null) {
        val doc = new SolrInputDocument()
        val fieldNames = Config.header.split(Config.headerSep) // default comma
        val fieldValues = record.split(Config.fieldValuesSep) // default comma

        for (i <- 0 until fieldNames.length) {
          if (fieldValues.length - 1 >= i) {
            doc.addField(fieldNames(i), fieldValues(i).trim);
          }
        }
        records.add(doc)
        batchSize = batchSize - 1
        record = reader.readLine()
      }

      hasNextLine = record

      Map(
        "record" -> records)
    }
  }

  object Index {
    // construct a feeder for content stored in CSV file
    val feeder = solrIndexV2Feeder

    // each user sends batches
    val search = repeat(Config.numBatchesPerUser) {
      feed(feeder).exec(solr("indexConstantUsersRequest")
        .indexV2(Config.header, feeder.next.get("record").get)) // provide appropriate header
    }
  }

  // pass zookeeper string, default collection to index, poolSize for CloudSolrClients
  val solrConf = solr.zkhost(Config.zkHost).collection(Config.defaultCollection)
    .numClients(Config.numClients.toInt).properties(Config.prop)

  // A scenario where users execute queries
  val users = scenario("Users").exec(Index.search)

  setUp(
    users.inject(
      constantUsersPerSec(Config.maxNumUsers.toDouble) during (Config.totalTimeInMinutes.toDouble minutes))
  ).protocols(solrConf)

}
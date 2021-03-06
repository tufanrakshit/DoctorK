package com.pinterest.doctork.servlet;

import com.pinterest.doctork.BrokerStats;
import com.pinterest.doctork.DoctorKMain;
import com.pinterest.doctork.KafkaBroker;
import com.pinterest.doctork.KafkaCluster;
import com.pinterest.doctork.KafkaClusterManager;
import com.pinterest.doctork.ReplicaStat;
import com.pinterest.doctork.util.KafkaUtils;
import com.pinterest.doctork.errors.ClusterInfoError;

import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Date;
import java.lang.Integer;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DoctorKBrokerStatsServlet extends DoctorKServlet {

  private static final Logger LOG = LogManager.getLogger(DoctorKBrokerStatsServlet.class);
  private static final Gson gson = (new GsonBuilder()).serializeSpecialFloatingPointValues().create();

  public KafkaBroker getBroker(String clusterName, int brokerId) throws ClusterInfoError {
    KafkaClusterManager clusterMananger =
        DoctorKMain.doctorK.getClusterManager(clusterName);
    if (clusterMananger == null) {
      throw new ClusterInfoError("Failed to find cluster manager for {}", clusterName);
    }
    KafkaBroker broker = clusterMananger.getCluster().getBroker(brokerId);
    if (broker == null) {
      throw new ClusterInfoError(
          "Failed to find broker {} in cluster {}",
          Integer.toString(brokerId),
          clusterName
      );
    }

    return broker;
  }

  public BrokerStats getLatestStats(String clusterName, KafkaBroker broker)
    throws ClusterInfoError {
    BrokerStats latestStats = broker.getLatestStats();
    if (latestStats == null) {
      throw new ClusterInfoError("Failed to get latest stats from broker {} in cluster {}",
          Integer.toString(broker.getId()),
          clusterName
      );
    }
    return latestStats;
  }
  
  @Override
  public void renderJSON(PrintWriter writer, Map<String, String> params) {
    try {
      int brokerId = Integer.parseInt(params.get("brokerid"));
      String clusterName = params.get("cluster");
      KafkaClusterManager clusterMananger =
	DoctorKMain.doctorK.getClusterManager(clusterName);
      KafkaCluster cluster = clusterMananger.getCluster();
      KafkaBroker broker = cluster.brokers.get(brokerId);
      writer.print(broker.toJson());
    } catch (Exception e) {
      LOG.error("Unable to find cluster : {}", e.toString());
      writer.print(gson.toJson(e));
      return;
    }
  }

  @Override
  public void renderHTML(PrintWriter writer, Map<String, String> params) {
    int brokerId = Integer.parseInt(params.get("brokerid"));
    String clusterName = params.get("cluster");
    printHeader(writer);

    writer.print("<div> <p><a href=\"/\">Home</a> > "
		 + "<a href=\"/servlet/clusterinfo?name=" + clusterName + "\"> " + clusterName
		 + "</a> > broker " + brokerId + "</p> </div>");

    writer.print("<table class=\"table table-hover\"> ");
    writer.print("<th class=\"active\"> Timestamp </th> ");
    writer.print("<th class=\"active\"> Stats </th>");
    writer.print("<tbody>");

    try {
      generateBrokerHtml(writer, clusterName, brokerId);
      writer.print("</tbody></table>");
      writer.print("</td> </tr>");
      writer.print("</tbody> </table>");
    } catch (Exception e) {
      LOG.error("Unexpected exception : ", e);
      e.printStackTrace(writer);
    }
    printFooter(writer);
  }

  private void generateBrokerHtml(PrintWriter writer, String clusterName, int brokerId)
      throws ClusterInfoError{
    KafkaBroker broker = getBroker(clusterName, brokerId);
    BrokerStats stats = getLatestStats(clusterName, broker);

    writer.print("<tr> <td> " + new Date(stats.getTimestamp()) + "</td>");
    writer.print("<td>");
    writer.print("<table class=\"table\"><tbody>");
    printHtmlTableRow(writer, "BrokerId", stats.getId());
    printHtmlTableRow(writer, "IsDecommissioned", broker.isDecommissioned());
    printHtmlTableRow(writer, "Name", stats.getName());
    printHtmlTableRow(writer, "HasFailure", stats.getHasFailure());
    printHtmlTableRow(writer, "KafkaVersion", stats.getKafkaVersion());
    printHtmlTableRow(writer, "KafkaStatsVersion", stats.getStatsVersion());
    printHtmlTableRow(writer, "LeadersIn1MinRate",
        NumberFormat.getNumberInstance(Locale.US).format(stats.getLeadersBytesIn1MinRate()));
    printHtmlTableRow(writer, "BytesInOneMinuteRate", NumberFormat.
        getNumberInstance(Locale.US).format(stats.getLeadersBytesIn1MinRate()));
    printHtmlTableRow(writer, "NetworkOutboundOneMinuteRate", NumberFormat
        .getNumberInstance(Locale.US).format(stats.getLeadersBytesOut1MinRate()));
    printHtmlTableRow(writer, "NumTopicPartitionReplicas",
        NumberFormat.getNumberInstance(Locale.US).format(stats.getNumReplicas()));
    printHtmlTableRow(writer, "NumLeaderPartitions", stats.getNumLeaders());

    Map<TopicPartition, ReplicaStat> replicaStats =
        new TreeMap(new KafkaUtils.TopicPartitionComparator());
    stats.getLeaderReplicaStats().stream()
        .forEach(
            rs -> replicaStats.put(new TopicPartition(rs.getTopic(), rs.getPartition()), rs));
    for (Map.Entry<TopicPartition, ReplicaStat> entry : replicaStats.entrySet()) {
      printHtmlTableRow(writer, entry.getKey(), entry.getValue());
    }
  }

  private void printHtmlTableRow(PrintWriter writer, Object col1, Object col2) {
    writer.print("<tr><td>" + col1 + "</td> <td>" + col2 + "</td> </tr>");
  }

}

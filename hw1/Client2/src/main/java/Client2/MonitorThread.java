package Client2;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class MonitorThread extends Thread {

  private MultiThreadedClient multiThreadedClient;
  private int[] latencies;
  private int numOfRequests;
  private int counter;
  private int lastProcessedRequests;
  private long start;
  private int secondsElapsed;
  private long totalResponseTime;
  private int minResponseTime;
  private int maxResponseTime;

  public MonitorThread(MultiThreadedClient multiThreadedClient, int numOfRequests) {
    this.multiThreadedClient = multiThreadedClient;
    this.numOfRequests = numOfRequests;
    this.minResponseTime = Integer.MAX_VALUE;
    latencies = new int[numOfRequests];
  }

  public void setStart(long start) {
    this.start = start;
  }

  public double getMeanResponseTime() {
    return totalResponseTime * 1.0 / numOfRequests;
  }

  public int getMinResponseTime() {
    return minResponseTime;
  }

  public int getMaxResponseTime() {
    return maxResponseTime;
  }

  public double getMedianResponseTime() {
    if (numOfRequests % 2 == 0) {
      return (latencies[numOfRequests / 2] + latencies[numOfRequests / 2 - 1]) / 2.0;
    }
    return latencies[numOfRequests / 2];
  }

  public int getPercentileResponseTime(int percentile) {
    int idx = (int) Math.ceil(percentile / 100.0 * (numOfRequests));
    return latencies[idx - 1];
  }

  @Override
  public void run() {
    File recordFile = new File("./src/main/java/Client2/records.csv");
    FileWriter recordFileWriter;
    CSVWriter recordCSVWriter;
    String[] headerIndividual = new String[]{"Start Time", "Request Type", "Latency", "Response Code"};


    File throughputFile = new File("./src/main/java/Client2/throughput.csv");
    FileWriter throughputFileWriter;
    CSVWriter throughputCSVWriter;
    String[] headerThroughput = new String[] {"Second", "Throughput"};
    try {
      recordFileWriter = new FileWriter(recordFile);
      recordCSVWriter = new CSVWriter(recordFileWriter);
      recordCSVWriter.writeNext(headerIndividual);

      throughputFileWriter = new FileWriter(throughputFile);
      throughputCSVWriter = new CSVWriter(throughputFileWriter);
      throughputCSVWriter.writeNext(headerThroughput);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    while (counter < numOfRequests) {
      if ((System.currentTimeMillis() - start) / 1000 > secondsElapsed) {
        secondsElapsed++;
        int temp = multiThreadedClient.getProcessedRequests();
        int currProcessedRequests = temp - lastProcessedRequests;
        lastProcessedRequests = temp;
        String[] throughputData = new String[] {String.valueOf(secondsElapsed), String.valueOf(currProcessedRequests)};
        throughputCSVWriter.writeNext(throughputData);
      }

      Record curr = multiThreadedClient.pullFromRecord();
      if (curr == null) {
        continue;
      }
      int responseTime = curr.getLatency();
      totalResponseTime += responseTime;
      minResponseTime = Math.min(minResponseTime, responseTime);
      maxResponseTime = Math.max(maxResponseTime, responseTime);
      latencies[counter] = responseTime;
      String[] data = new String[]{
          String.valueOf(curr.getStart()),
          curr.getRequestType(),
          String.valueOf(responseTime),
          String.valueOf(curr.getResponseCode())};
      recordCSVWriter.writeNext(data);
      counter++;
    }

    try {
      recordCSVWriter.close();
      throughputCSVWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Arrays.sort(latencies);
  }
}

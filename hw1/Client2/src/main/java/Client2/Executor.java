package Client2;

public class Executor {
  private static MultiThreadedClient multiThreadedClient;
  private final static int NUM_OF_REQUESTS = 500000;
  private static final int NUM_OF_THREADS = 100;
  public static void main(String[] args) {

    multiThreadedClient = new MultiThreadedClient(NUM_OF_THREADS, NUM_OF_REQUESTS);
    try {
      multiThreadedClient.start();
    } catch (InterruptedException e) {
      System.out.println("Execution interrupted");
      e.printStackTrace();
    }
    printResults();
  }

  public static void printResults() {
    long timeElapsed = multiThreadedClient.getTimeElapsed();
    int successfulRequests = multiThreadedClient.getSuccessfulRequests();
    int failedRequests = multiThreadedClient.getFailedRequests();
    int throughput = (int) (multiThreadedClient.getNumOfRequests() * 1000.0 / timeElapsed);
    MonitorThread monitorThread = multiThreadedClient.getMonitorThread();
    System.out.println("Task3:");
    System.out.println("Number of successful requests sent: " + successfulRequests);
    System.out.println("Number of unsuccessful requests sent: " + failedRequests);
    System.out.println("Total run time (wall time) for all threads to complete: " + timeElapsed);
    System.out.println("Total throughput in requests per second :" + throughput);
    System.out.println("Mean response time (milliseconds) :" + monitorThread.getMeanResponseTime());
    System.out.println("Median response time (milliseconds) :" + monitorThread.getMedianResponseTime());
    System.out.println("P99 (99th percentile) response time :" + monitorThread.getPercentileResponseTime(99));
    System.out.println("Min response time (milliseconds) :" +  monitorThread.getMinResponseTime());
    System.out.println("Max response time (milliseconds) :" +  monitorThread.getMaxResponseTime());
  }
}
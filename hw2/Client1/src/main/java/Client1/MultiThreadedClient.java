package Client1;

import java.util.concurrent.atomic.AtomicInteger;
public class MultiThreadedClient {
  private long timeElapsed;
  private int numOfThreads;
  private int numOfRequests;
  private int processedRequests;
  private AtomicInteger successfulRequests;
  private AtomicInteger failedRequests;
  private int throughput;
  private Thread[] threads;

  public MultiThreadedClient(int numOfThreads, int numOfRequests) {
    this.numOfThreads = numOfThreads;
    this.numOfRequests = numOfRequests;
    this.successfulRequests = new AtomicInteger(0);
    this.failedRequests = new AtomicInteger(0);
    threads = new SwipeThread[numOfThreads];

    for (int i = 0; i < numOfThreads; i++) {
      threads[i] = new SwipeThread(this, i);
    }
  }

  public synchronized boolean increaseProcessedRequest() {
    if (processedRequests < numOfRequests) {
      processedRequests++;
      return true;
    }
    return false;
  }

  public void increaseSuccessRequests() {
    successfulRequests.incrementAndGet();
  }

  public void increaseFailedRequests() {
    failedRequests.incrementAndGet();
  }

  public void start() throws InterruptedException {
    long start = System.currentTimeMillis();
    int numOfThreads = getNumOfThreads();
    for (int i = 0; i < numOfThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numOfThreads; i++) {
      threads[i].join();
    }

    setTimeElapsed(System.currentTimeMillis() - start);
    setThroughput((int) (getNumOfRequests() * 1000 / getTimeElapsed()));
  }

  public long getTimeElapsed() {
    return timeElapsed;
  }

  public void setTimeElapsed(long timeElapsed) {
    this.timeElapsed = timeElapsed;
  }

  public int getNumOfRequests() {
    return numOfRequests;
  }

  public int getSuccessfulRequests() {
    return successfulRequests.get();
  }

  public int getFailedRequests() {
    return failedRequests.get();
  }

  public int getNumOfThreads() {
    return numOfThreads;
  }

  public int getThroughput() {
    return throughput;
  }

  public void setThroughput(int throughput) {
    this.throughput = throughput;
  }
}

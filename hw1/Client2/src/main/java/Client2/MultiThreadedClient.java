package Client2;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedClient {

  private long timeElapsed;
  private int numOfThreads;
  private int numOfRequests;
  private int processedRequests;
  private AtomicInteger successfulRequests;
  private AtomicInteger failedRequests;
  private Thread[] threads;
  private MonitorThread monitorThread;
  private Queue<Record> recordQueue;

  public MultiThreadedClient(int numOfThreads, int numOfRequests) {
    this.numOfThreads = numOfThreads;
    this.numOfRequests = numOfRequests;
    this.successfulRequests = new AtomicInteger(0);
    this.failedRequests = new AtomicInteger(0);
    threads = new RecordSwipeThread[numOfThreads];
    this.recordQueue = new LinkedList<>();
    monitorThread = new MonitorThread(this, numOfRequests);

    for (int i = 0; i < numOfThreads; i++) {
      threads[i] = new RecordSwipeThread(this, recordQueue, i);
    }
  }

  public synchronized boolean IncreaseProcessedRequest() {
    if (processedRequests < numOfRequests) {
      processedRequests++;
      return true;
    }
    return false;
  }

  public int getProcessedRequests() {
    return processedRequests;
  }

  public void increaseSuccessRequests() {
    successfulRequests.incrementAndGet();
  }

  public void increaseFailedRequests() {
    failedRequests.incrementAndGet();
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
  public synchronized void addToRecord(Record record) {
    recordQueue.add(record);
  }

  public synchronized Record pullFromRecord() {
    if (recordQueue.isEmpty()) {
      return null;
    }
    return recordQueue.poll();
  }
  public void start() throws InterruptedException {

    monitorThread.start();
    int numOfThreads = getNumOfThreads();

    long start = System.currentTimeMillis();
    monitorThread.setStart(start);
    for (int i = 0; i < numOfThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numOfThreads; i++) {
      threads[i].join();
    }

    setTimeElapsed(System.currentTimeMillis() - start);

    monitorThread.join();
  }
  public MonitorThread getMonitorThread() {
    return monitorThread;
  }
}

package Client2;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import java.util.Queue;
import com.squareup.okhttp.ConnectionPool;
import io.swagger.client.ApiClient;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.Random;

public class RecordSwipeThread extends Thread {

  private static final String SERVER_BASE_PATH = "http://52.43.229.195:8080/Server_war/";
  private static final int MAX_IDLE_CONNECTION = 200;
  private static final long KEEP_ALIVE_DURATION_MS = 300000L;
  private static final int SWIPER_ID_UPPER_BOUND = 5000;
  private static final int SWIPEE_ID_UPPER_BOUND = 1000000;
  private static final int MAX_RETRIES = 5;
  private static final String POST = "POST";

  private MultiThreadedClient multiThreadedClient;
  private SwipeApi swipeApi;
  private Random random = new Random();
  private SwipeDetails swipeDetails;
  private String leftOrRight;
  private int num;
  private Queue<Record> recordQueue;

  public RecordSwipeThread(MultiThreadedClient multiThreadedClient, Queue<Record> recordQueue, int num) {
    this.multiThreadedClient = multiThreadedClient;
    this.num = num;
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(SERVER_BASE_PATH);
    apiClient.setConnectTimeout(0);
    apiClient.setReadTimeout(0);
    apiClient.getHttpClient().setConnectionPool(new ConnectionPool(MAX_IDLE_CONNECTION, KEEP_ALIVE_DURATION_MS));
    swipeApi = new SwipeApi(apiClient);
    swipeDetails = new SwipeDetails();
    this.recordQueue = recordQueue;
  }

  public ApiResponse<Void> sendRequest() throws ApiException {
    return swipeApi.swipeWithHttpInfo(swipeDetails, leftOrRight);
  }

  @Override
  public void run() {
    while (true) {
      boolean increase = multiThreadedClient.IncreaseProcessedRequest();
      if (!increase) {
        break;
      }
      generateRandomValues();
      boolean success = false;
      int retries = MAX_RETRIES;

      while (!success && retries > 0) {
        try {
          long start = System.currentTimeMillis();
          ApiResponse<Void> response = sendRequest();

          long end = System.currentTimeMillis();
          int latency = (int) (end - start);

          Record record = new Record(start, end, latency, POST, response.getStatusCode());
          multiThreadedClient.addToRecord(record);
          success = true;
        } catch (ApiException e) {
          int code = e.getCode();
          if (code / 100 == 5 || code / 100 == 4) {
            retries--;
          }
          else {
            System.out.println(e.getCode());
            e.printStackTrace();
            break;
          }
        }
      }
      if (success) {
        multiThreadedClient.increaseSuccessRequests();
      }
      else {
        multiThreadedClient.increaseFailedRequests();
      }
    }
  }
  public void generateRandomValues() {
    String swiper = String.valueOf(random.nextInt(SWIPER_ID_UPPER_BOUND) + 1);
    String swipee = String.valueOf(random.nextInt(SWIPEE_ID_UPPER_BOUND) + 1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 256; i ++) {
      int randomIndex = new Random().nextInt(26);
      sb.append((char) (randomIndex + 'A'));
    }
    String comment = sb.toString();
    swipeDetails.setSwiper(swiper);
    swipeDetails.setSwipee(swipee);
    swipeDetails.setComment(comment);
    leftOrRight = random.nextInt(2) == 0 ? "right" : "left";
  }
}

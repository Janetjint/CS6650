package Client1;

import com.squareup.okhttp.ConnectionPool;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.Random;

public class SwipeThread extends Thread {
//  private static final String SERVER_BASE_PATH = "http://localhost:8080/Server2_war_exploded/";
//  private static final String SERVER_BASE_PATH = "http://54.245.207.247:8080/Server2_war/";
//  private static final String SERVER_BASE_PATH = "http://18.237.101.22:8080/Server2_war/";
private static final String SERVER_BASE_PATH = "http://A2lb-1530265666.us-west-2.elb.amazonaws.com:8080/Server2_war/";

  private static final int MAX_IDLE_CONNECTION = 200;
  private static final long KEEP_ALIVE_DURATION_MS = 300000L;
  private static final int SWIPER_ID_UPPER_BOUND = 5000;
  private static final int SWIPEE_ID_UPPER_BOUND = 1000000;
  private static final int MAX_RETRIES = 5;
  private MultiThreadedClient multiThreadedClient;
  private SwipeApi swipeApi;
  private Random random = new Random();
  private SwipeDetails swipeDetails;
  private String leftOrRight;
  private int num;

  public SwipeThread(MultiThreadedClient multiThreadedClient, int num) {
    this.multiThreadedClient = multiThreadedClient;
    this.num = num;
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(SERVER_BASE_PATH);
    apiClient.setConnectTimeout(0);
    apiClient.setReadTimeout(0);
    apiClient.getHttpClient().setConnectionPool(new ConnectionPool(MAX_IDLE_CONNECTION, KEEP_ALIVE_DURATION_MS));
    swipeApi = new SwipeApi(apiClient);
    swipeDetails = new SwipeDetails();
  }

  public ApiResponse<Void> sendRequest() throws ApiException {
    return swipeApi.swipeWithHttpInfo(swipeDetails, leftOrRight);
  }

  @Override
  public void run() {
    while (true) {
      boolean increase = multiThreadedClient.increaseProcessedRequest();
      if (!increase) {
        break;
      }
      generateRandomValues();
      boolean success = false;
      int retries = MAX_RETRIES;

      while (!success && retries > 0) {
        try {
          ApiResponse<Void> response = sendRequest();
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
      } else {
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

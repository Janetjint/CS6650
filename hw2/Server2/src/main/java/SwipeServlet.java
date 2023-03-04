import Bean.ResponseMsg;
import Bean.SwipeDetails;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.impl.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {

    private final static String LEFT = "left";
    private final static String RIGHT = "right";
    private static final int MAX_SWIPER_ID = 5000;
    private static final int MAX_SWIPEE_ID = 1000000;
    private final static int MAX_COMMENT_LEN = 256;

    // Number of channels to add to pools
    private static final int NUM_CHANS = 50;
    // RMQ broker machine
    private static final String SERVER = "35.86.125.106";
//     private static final String SERVER = "localhost";
    private static final String VIRTUAL_HOST = "cherry_broker";
    private static final String USER_NAME = "admin";
//    private static final String USER_NAME = "guest";
    private static final String PASSWORD = "password";
//    private static final String PASSWORD = "guest";
    private static final String FIRST_QUEUE = "queue1";
    private static final String SECOND_QUEUE = "queue2";
    // the duration in seconds a client waits for a channel to be available in the pool
    // Tune value to meet request load and pass to config.setMaxWait(...) method
    private static final int WAIT_TIME_SECS = 1;

    private GenericObjectPool<Channel> pool;

    @Override
    public void init() {
        // we use this object to tailor the behavior of the GenericObjectPool
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        // limit the pool size
        config.setMaxTotal(NUM_CHANS);
        // clients will block when pool is exhausted, for a maximum duration of WAIT_TIME_SECS
        config.setBlockWhenExhausted(true);
        // tune WAIT_TIME_SECS to meet your workload/demand
        config.setMaxWaitMillis(Duration.ofSeconds(WAIT_TIME_SECS).toMillis());
        // config factory
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(SERVER);
//        connectionFactory.setVirtualHost(VIRTUAL_HOST);
        connectionFactory.setUsername(USER_NAME);
        connectionFactory.setPassword(PASSWORD);

        try {
            // create a new connection
            final Connection conn = connectionFactory.newConnection();
            // The channel factory generates new channels on demand, as needed by the GenericObjectPool
            RMQChannelFactory chanFactory = new RMQChannelFactory (conn);
            // create the pool
            pool = new GenericObjectPool<>(chanFactory, config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String urlPath = request.getPathInfo();
        ResponseMsg responseMsg = new ResponseMsg();

        Gson gson = new Gson();

        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            responseMsg.setMessage("missing parameters");
            response.getWriter().write(gson.toJson(responseMsg));
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            responseMsg.setMessage("Invalid path parameter: should be " + LEFT + " or " + RIGHT);
            response.getWriter().write(gson.toJson(responseMsg));
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = request.getReader().readLine()) != null) {
                sb.append(s);
            }

            SwipeDetails swipeDetails = (SwipeDetails) gson.fromJson(sb.toString(), SwipeDetails.class);
            String swiper = swipeDetails.getSwiper();
            String swipee = swipeDetails.getSwipee();
            String comment = swipeDetails.getComment();

            if (swiper == null || swipee == null) {
                throw new Exception("Missing parameters!");
            }

            if (!(1 <= Integer.parseInt(swiper) && Integer.parseInt(swiper) <= MAX_SWIPER_ID)){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMsg.setMessage("User not found: invalid swiper id: "+ swiper);
            } else if (!(1 <= Integer.parseInt(swipee) && Integer.parseInt(swipee) <= MAX_SWIPEE_ID)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMsg.setMessage("User not found: invalid swipee id: " + swipee);
            } else if (!(1 <= comment.length() && comment.length() <= MAX_COMMENT_LEN)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMsg.setMessage("Invalid inputs: comment cannot exceed 256 characters");
            } else {
                response.setStatus(HttpServletResponse.SC_CREATED);
                String action = urlParts[1].equals("right") ? "liked" : "disliked";
                responseMsg.setMessage(String.format("Write Successfully: User %s %s user %s.", swiper, action, swipee));
            }

            // create and convert payload to json string
            Payload payload = new Payload(swipeDetails.getSwiper(),
                    swipeDetails.getSwipee(), urlParts[1]);

            String payloadString = gson.toJson(payload);

            try {
                Channel channel;
                // get a channel from the pool
                channel = pool.borrowObject();

                // publish a message
                channel.queueDeclare(FIRST_QUEUE, false, false, false, null);
                channel.queueDeclare(SECOND_QUEUE, false, false, false, null);
                byte[] message = payloadString.getBytes();
                channel.basicPublish("", FIRST_QUEUE, null, message);
                channel.basicPublish("", SECOND_QUEUE, null, message);
                // return the channel to the pool
                pool.returnObject(channel);

            } catch (IOException ex) {
                Logger.getLogger(SwipeServlet.class.getName()).log(Level.INFO, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(SwipeServlet.class.getName()).log(Level.INFO, null, ex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            responseMsg.setMessage(ex.getMessage());
        } finally {
            response.getWriter().write(gson.toJson(responseMsg));
        }
    }

    private boolean isUrlValid(String[] urlParts) {
        return urlParts.length == 2 && (urlParts[1].equals(LEFT) || urlParts[1].equals(RIGHT));
    }
}

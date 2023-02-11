import Bean.ResponseMsg;
import Bean.SwipeDetails;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {

  private final static String LEFT = "left";
  private final static String RIGHT = "right";
  private static final int MAX_SWIPER_ID = 5000;
  private static final int MAX_SWIPEE_ID = 1000000;
  private final static int MAX_COMMENT_LEN = 256;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing paramterers");
      return;
    }

    String[] urlParts = urlPath.split("/");
    // and now validate url path and return the response status code
    // (and maybe also some value if input is valid)

    if (!isUrlValid1(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      // do any sophisticated processing with urlParts which contains all the url params
      // TODO: process url params in `urlParts`
      res.getWriter().write("It works!");
    }
  }

  private boolean isUrlValid1(String[] urlPath) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    return true;
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
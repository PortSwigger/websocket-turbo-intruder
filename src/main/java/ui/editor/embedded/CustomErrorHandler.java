package ui.editor.embedded;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomErrorHandler extends ErrorHandler {
    private static final String message = """
            <html>
              <head>
                <title>Internal Server Error</title>
                <style>
                  body {
                    font-family: sans-serif;
                    background-color: #f8f9fa;
                    color: #333;
                    padding: 40px;
                  }
                  .error-container {
                    max-width: 800px;
                    margin: auto;
                    background-color: #fff;
                    border: 1px solid #ccc;
                    padding: 30px;
                    border-radius: 8px;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
                  }
                  h1 {
                    color: #c0392b;
                  }
                  ul {
                    padding-left: 20px;
                  }
                </style>
              </head>
              <body>
                <div class="error-container">
                  <h1>WebSocket Turbo Intruder Embedded Server - Internal Error</h1>
                  <p>
                    The server failed to receive a response from the remote WebSocket server within the expected time.
                    This is an internal error and is not related to the target being tested.
                  </p>
                  <p>To resolve the issue, please check the following:</p>
                  <ul>
                    <li>Ensure the remote WebSocket server is reachable and responsive.</li>
                    <li>Verify that your Turbo Intruder script is functioning correctly.</li>
                    <li>
                      Consider sending periodic <strong>ping</strong> and <strong>pong</strong> WebSocket messages
                      to keep the connection alive, or use the <strong>TURBO</strong> engine instead.
                    </li>
                  </ul>
                </div>
              </body>
            </html>""";

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {

        // Only handle if it's an error response
        if (response.getStatus() >= 400) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(response.getStatus());

            response.getWriter().write(message);

            baseRequest.setHandled(true);
        }
    }
}

package embedded;

import logger.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class ProxyServlet extends HttpServlet {

    private static final int TIMEOUT = 5;
    private Map<String, ConnectionWrapper> clients;
    private Logger logger;

    public ProxyServlet(Logger logger, Map<String, ConnectionWrapper> clients) {
        this.clients = clients;
        this.logger = logger;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Read the HTTP request body
        String target = req.getParameter("url");
        byte[] body = readRequestBody(req);

        try {
            if (clients.containsKey(target)) {
                ByteBuffer wsResponse = clients.get(target).sendMessage(body, TIMEOUT);
                ServletOutputStream outputStream = resp.getOutputStream();
                outputStream.write(wsResponse.array());
            }
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No response from WebSocket server");
        }
    }

    private byte[] readRequestBody(HttpServletRequest req) throws IOException {
        InputStream inputStream = req.getInputStream();  // Get raw binary stream
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] temp = new byte[1024]; // Read in 1KB chunks
        int bytesRead;

        while ((bytesRead = inputStream.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }

        return buffer.toByteArray();
    }
}

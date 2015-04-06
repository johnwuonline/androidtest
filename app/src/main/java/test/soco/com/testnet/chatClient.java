package test.soco.com.testnet;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Crystal on 15/3/31.
 */
public class chatClient {

    private static SSLSocketFactory clientFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

    /**
     * Does a number of HTTPS requests on some host and consumes the response.
     * We don't use the HttpsUrlConnection class, but do this on our own
     * with the SSLSocket class. This gives us a chance to test the basic
     * behavior of SSL.
     *
     * @param host      The host name the request is being sent to.
     * @param port      The port the request is being sent to.
     * @param path      The path being requested (e.g. "/index.html").
     * @param outerLoop The number of times we reconnect and do the request.
     * @param innerLoop The number of times we do the request for each
     *                  connection (using HTTP keep-alive).
     * @param delay     The delay after each request (in seconds).
     * @throws IOException When a problem occurs.
     */
    private void fetch(SSLSocketFactory socketFactory, String host, int port,
                       boolean secure, String path, int outerLoop, int innerLoop,
                       int delay, int timeout) throws IOException {
        InetSocketAddress address = new InetSocketAddress(host, port);

        for (int i = 0; i < outerLoop; i++) {
            // Connect to the remote host
            Socket socket = secure ? socketFactory.createSocket() : new Socket();
            if (timeout >= 0) {
                socket.setKeepAlive(true);
                socket.setSoTimeout(timeout * 1000);
            }
            socket.connect(address);

            // Get the streams
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output);

            try {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                try {
                    for (int j = 0; j < innerLoop; j++) {
                        android.util.Log.d("SSLSocketTest", "GET https://" + host + path + " HTTP/1.1");

                        // Send a request
                        writer.println("hi\r");
                        writer.println("\r");
                        writer.flush();

                        int length = -1;
                        boolean chunked = false;

                        String line = input.readLine();

                        if (line == null) {
                            throw new IOException("No response from server");
                            // android.util.Log.d("SSLSocketTest", "No response from server");
                        }

                        // Consume the headers, check content length and encoding type
                        while (line != null && line.length() != 0) {
                            /*
                            int dot = line.indexOf(':');
                            if (dot != -1) {
                                String key = line.substring(0, dot).trim();
                                String value = line.substring(dot + 1).trim();

                                if ("Content-Length".equalsIgnoreCase(key)) {
                                    length = Integer.valueOf(value);
                                } else if ("Transfer-Encoding".equalsIgnoreCase(key)) {
                                    chunked = "Chunked".equalsIgnoreCase(value);
                                }

                            }
                            */
                            System.out.println("received:"+line);
                            line = input.readLine();
                        }

                        // Consume the content itself
                        /*
                        if (chunked) {
                            length = Integer.parseInt(input.readLine(), 16);
                            while (length != 0) {
                                byte[] buffer = new byte[length];
                                input.readFully(buffer);
                                input.readLine();
                                length = Integer.parseInt(input.readLine(), 16);
                            }
                            input.readLine();
                        } else {
                            byte[] buffer = new byte[length];
                            input.readFully(buffer);
                        }
*/
                        // Sleep for the given number of seconds
                        try {
                            Thread.sleep(delay * 1000);
                        } catch (InterruptedException ex) {
                            // Shut up!
                        }
                    }
                } finally {
                    input.close();
                }
            } finally {
                writer.close();
            }
            // Close the connection
            socket.close();
        }
    }

    /**
     * Invokes fetch() with the default socket factory.
     */
    private void fetch(String host, int port, boolean secure, String path,
                       int outerLoop, int innerLoop,
                       int delay, int timeout) throws IOException {
        fetch(clientFactory, host, port, secure, path, outerLoop, innerLoop,
                delay, timeout);
    }

    public void sslConnectTest(String host, int port, String msg) throws IOException {
        fetch(host, port, true, msg, 1, 1, 0, 60);
    }

    /**
     * Does a single request for each of the hosts. Consumes the response.
     *
     * @throws IOException If a problem occurs.
     */
    public void testSimple() throws IOException {
        fetch("www.fortify.net", 443, true, "/sslcheck.html", 1, 1, 0, 60);
        fetch("mail.google.com", 443, true, "/mail/", 1, 1, 0, 60);
        fetch("www.paypal.com", 443, true, "/", 1, 1, 0, 60);
        fetch("www.yellownet.ch", 443, true, "/", 1, 1, 0, 60);
    }

    /**
     * Does repeated requests for each of the hosts, with the connection being
     * closed in between.
     *
     * @throws IOException If a problem occurs.
     */
    public void testRepeatedClose() throws IOException {
        fetch("www.fortify.net", 443, true, "/sslcheck.html", 10, 1, 0, 60);
        fetch("mail.google.com", 443, true, "/mail/", 10, 1, 0, 60);
        fetch("www.paypal.com", 443, true, "/", 10, 1, 0, 60);
        fetch("www.yellownet.ch", 443, true, "/", 10, 1, 0, 60);
    }

    /**
     * Does repeated requests for each of the hosts, with the connection being
     * kept alive in between.
     *
     * @throws IOException If a problem occurs.
     */
    public void testRepeatedKeepAlive() throws IOException {
        fetch("www.fortify.net", 443, true, "/sslcheck.html", 1, 10, 0, 60);
        fetch("mail.google.com", 443, true, "/mail/", 1, 10, 0, 60);

        // These two don't accept keep-alive
        // fetch("www.paypal.com", 443, "/", 1, 10);
        // fetch("www.yellownet.ch", 443, "/", 1, 10);
    }

    /**
     * Does repeated requests for each of the hosts, with the connection being
     * closed in between. Waits a couple of seconds after each request, but
     * stays within a reasonable timeout. Expectation is that the connection
     * stays open.
     *
     * @throws IOException If a problem occurs.
     */
    public void testShortTimeout() throws IOException {
        fetch("www.fortify.net", 443, true, "/sslcheck.html", 1, 10, 5, 60);
        fetch("mail.google.com", 443, true, "/mail/", 1, 10, 5, 60);

        // These two don't accept keep-alive
        // fetch("www.paypal.com", 443, "/", 1, 10);
        // fetch("www.yellownet.ch", 443, "/", 1, 10);
    }
}

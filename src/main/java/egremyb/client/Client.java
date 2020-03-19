package egremyb.client;

import egremyb.common.Protocol;
import egremyb.server.Server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    final static Logger LOG = Logger.getLogger(Server.class.getName());

    private final static String NO_CONNECTION_OPENED = "There is no connection opened to the server.\n";
    private final static String UNEXPECTED_RESPONSE  = "The response of the server was unexpected.\n";
    private final static String BAD_REQUEST          = "The operation must have been badly written.\n";
    private final static String UNABLE_TO_CONNECT    = "Unable to connect to server";

    private Socket       clientSocket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private String       ip;
    private int          port;
    private boolean      connected;

    /**
     * Send a String to the server
     * @param s String to send
     * @throws IOException if an error occurred while writing
     */
    private void sendRequest(String s) throws IOException {
        writer.write(s + '\n');
        writer.flush();
        LOG.info("Sending a new request to server : " + s);
    }

    /**
     * Get the response of the server
     * @return Response as a String
     * @throws IOException if an error occurred while reading
     */
    private String getResponse() throws IOException{
        return getResponse(null);
    }

    /**
     * Get the response of the server
     * @param expected Expected response of th server
     * @return Response as a String
     * @throws IOException if an error occurred while reading
     *                     or if the response wasn't the one expected
     */
    private String getResponse(String expected) throws IOException{
        String response = reader.readLine();
        // return response
        if (expected == null || response.equals(expected)) {
            LOG.info("Received response from server : " + response);
            return response;
        } else {
            String error = UNEXPECTED_RESPONSE + " : received \"" + response + "\", expected \"" + expected + "\"";
            LOG.info(error);
            throw new IOException(error);
        }
    }

    /**
     * Set ip and port to default
     */
    public Client() {
        this("localhost");
    }

    /**
     * Set given ip and the default port
     * @param ip address of server
     */
    public Client(String ip) {
        this(ip, Protocol.DEFAULT_PORT);
    }

    /**
     * Set given ip and port
     * @param ip address of server
     * @param port port of server
     */
    public Client(String ip, int port) {
        clientSocket   = null;
        writer         = null;
        reader         = null;
        this.ip        = ip;
        this.port      = port;
        this.connected = false;
        LOG.info("Created a new client, ip=\"" + ip + "\", port=" + port);
    }

    /**
     * Open a connection with to the server
     */
    public void openConnection() {
        // close open connection if there is one
        if (connected) {
            LOG.info("Closing old connection and opening a new connection");
            closeConnection();
        }
        // try to connect to the server
        try {
            // init the socket and streams
            clientSocket = new Socket(ip, port);
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            // send HELLO message
            sendRequest(Protocol.CMD_HELLO);
            // get response expected as a WELCOME message
            getResponse(Protocol.CMD_WELCOME);
            connected = true;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, UNABLE_TO_CONNECT + " : {0}", ex.getMessage());
            System.out.println(UNABLE_TO_CONNECT + ".");
        } finally {
            closeConnection();
        }
    }

    /**
     * Close the connection to the server
     */
    public void closeConnection() {
        // check if a connection was open
        if (connected) {
            LOG.info(NO_CONNECTION_OPENED);
            return;
        }
        // close Input Stream
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        // close Output Stream
        try {
            if (writer != null) {
                sendRequest(Protocol.CMD_BYE);
                writer.close();
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        // close Socket
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Send a calculation for the server to compute
     * @param operand1 First operand
     * @param operator Operator
     * @param operand2 Second operand
     * @return Result of calculation
     */
    public Double sendCalculationToCompute(final Double operand1, final String operator, final Double operand2) {
        String response;
        if (connected) {
            try {
                // send request
                sendRequest(Double.toString(operand1) + Protocol.SEPARATOR + operator + Protocol.SEPARATOR + operand2);
                // check the response of the server
                response = getResponse();
                if (response.equals(Protocol.CMD_WRONG)) {
                    System.out.println(BAD_REQUEST);
                    LOG.info(BAD_REQUEST);
                    return null;
                }
                // return the response as an int
                return Double.parseDouble(response);
            } catch (IOException | NumberFormatException ex) {
                System.out.println(ex.getMessage());
            }
        } else {
            System.out.println(NO_CONNECTION_OPENED);
            LOG.info(NO_CONNECTION_OPENED);
        }

        return null;
    }
}
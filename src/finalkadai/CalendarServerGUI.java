package finalkadai;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CalendarServerGUI {
    private JTextArea eventArea;
    private JFrame frame;

    public CalendarServerGUI() {
        frame = new JFrame("Calendar Server");
        eventArea = new JTextArea(20, 40);
        eventArea.setEditable(false);
        frame.add(new JScrollPane(eventArea), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void startServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            eventArea.append("Server is listening on port " + port + "\n");
            while (true) {
                try (Socket socket = serverSocket.accept();
                        InputStream input = socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

                    String event;
                    while ((event = reader.readLine()) != null) {
                        eventArea.append("Received event: " + event + "\n");
                    }
                } catch (IOException e) {
                    eventArea.append("Server exception: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            eventArea.append("Could not listen on port " + port + "\n");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CalendarServerGUI serverGUI = new CalendarServerGUI();
        serverGUI.startServer(12346); // ポート番号を12346に変更
    }
}
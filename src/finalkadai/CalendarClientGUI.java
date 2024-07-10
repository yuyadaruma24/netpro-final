package finalkadai;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class CalendarClientGUI {
    private JFrame frame;
    private JTextField eventField;
    private JTextArea logArea;
    private PrintWriter writer;
    private Socket socket;

    public CalendarClientGUI() {
        frame = new JFrame("Calendar Client");
        eventField = new JTextField(30);
        logArea = new JTextArea(20, 40);
        logArea.setEditable(false);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendEvent();
            }
        });

        JPanel panel = new JPanel();
        panel.add(new JLabel("Event:"));
        panel.add(eventField);
        panel.add(sendButton);

        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        connectToServer("localhost", 12346); // サーバーに接続
    }

    private void connectToServer(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);
            logArea.append("Connected to the server\n");
        } catch (IOException e) {
            logArea.append("Client exception: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    private void sendEvent() {
        String event = eventField.getText();
        if (event.isEmpty()) {
            return;
        }
        writer.println(event);
        logArea.append("Sent event: " + event + "\n");
        eventField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarClientGUI());
    }
}

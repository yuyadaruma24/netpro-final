package finalkadai;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CalendarServer {

    private static final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();

    public static void main(String arg[]) {
        ArrayList<TerminalInput> dataList = new ArrayList<>();

        try {
            System.out.print("ポートを入力してください(5000など) → ");
            int port = 5000;
            System.out.println("localhostの" + port + "番ポートで待機します");
            ServerSocket server = new ServerSocket(port);

            while (true) {
                Socket socket = server.accept();
                System.out.println("接続しました。相手の入力を待っています......");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                clientOutputStreams.add(oos);
                new ClientHandler(socket, dataList, oos).start();
            }

        } catch (BindException be) {
            be.printStackTrace();
            System.out.println("ポート番号が不正、ポートが使用中です");
            System.err.println("別のポート番号を指定してください(6000など)");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("エラーが発生したのでプログラムを終了します");
            throw new RuntimeException(e);
        }
    }

    private static void broadcast(List<TerminalInput> dataList) {
        synchronized (clientOutputStreams) {
            for (ObjectOutputStream oos : clientOutputStreams) {
                try {
                    oos.writeObject(new ArrayList<>(dataList));
                    oos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private ArrayList<TerminalInput> dataList;
        private ObjectOutputStream oos;

        public ClientHandler(Socket socket, ArrayList<TerminalInput> dataList, ObjectOutputStream oos) {
            this.socket = socket;
            this.dataList = dataList;
            this.oos = oos;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                
                // クライアントが接続されたら現在のデータを送信
                oos.writeObject(new ArrayList<>(dataList));
                oos.flush();
                
                while (true) {
                    TerminalInput input = (TerminalInput) ois.readObject();
                    String date = input.getDate();
                    if (date.equals("exit")) {
                        break;
                    }
                    String task = input.getTask();
                    String[] rgba = input.getRgba();
                    String name = input.getName();
                    String detail = input.getDetail();

                    synchronized (dataList) {
                        dataList.add(input);
                    }

                    // 新しいデータを全てのクライアントにブロードキャスト
                    broadcast(dataList);
                }

                ois.close();
                oos.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("クライアントとの通信中にエラーが発生しました");
            }
        }
    }
}

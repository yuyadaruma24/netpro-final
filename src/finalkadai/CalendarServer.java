package finalkadai;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

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
                    String name = input.getName();
                    System.out.println("名前は: " + name);
                    String date = input.getDate();
                    System.out.println("日付は: " + date);
                    if (date.equals("exit")) {
                        break;
                    }
                    String task = input.getTask();
                    System.out.println("内容は: " + task);
                    String detail = input.getDetail();
                    System.out.println("詳細は: " + detail);
                    String[] rgba = input.getRgba();
                    System.out.print("色は");
                    for (String color : rgba) {
                        System.out.print(color + ",");
                    }
                    System.out.println();

                    synchronized (dataList) {
                        boolean remove = false;
                        Iterator<TerminalInput> iterator = dataList.iterator();
                        while (iterator.hasNext()) {
                            TerminalInput data = iterator.next();
                            if (data.getDate().equals(date) && data.getName().equals(name) && data.getTask().equals(task) && data.getDetail().equals(detail) && Arrays.equals(data.getRgba(), rgba)) {
                                iterator.remove();  // Iteratorを使用して要素を削除
                                remove = true;
                                System.out.println("削除成功");
                                break;  // 要素を見つけて削除したのでループを終了
                            }
                        }
                    
                        if (!remove) {
                            dataList.add(input);
                        }
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

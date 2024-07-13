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
        int calendarID = 0;
        System.out.print("ポートを入力してください(5000など) → ");
        int port = 5000;
        System.out.println("localhostの" + port + "番ポートで待機します");
        try(ServerSocket server = new ServerSocket(port);) {
            while (true) {
                Socket socket = server.accept();
                System.out.println("接続しました。相手の入力を待っています......");
                calendarID++;
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeInt(calendarID);
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
                    String method = input.getMethod();
                    System.out.println("method: " + method);
                    String name = input.getName();
                    System.out.println("名前は: " + name);
                    String date = input.getDate();
                    System.out.println("日付は: " + date);
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
                    int ID = input.getCalendarID();
                    int num = input.getCalendarNum();
                    System.out.println("ID: " + ID);
                    System.out.println("num: " + num);

                    synchronized (dataList) {
                        if (method.equals("add")) {
                            dataList.add(input);
                        }else if(method.equals("delete")){
                            for(int i = 0; i < dataList.size(); i++){
                                if (ID == dataList.get(i).getCalendarID() && num == dataList.get(i).getCalendarNum()) {
                                    dataList.remove(i);
                                }
                            }
                        }else if(method.equals("save") || method.equals("change")){
                            for(int i = 0;i < dataList.size(); i++){
                                if (ID == dataList.get(i).getCalendarID() && num == dataList.get(i).getCalendarNum()) {
                                    dataList.set(i, input);
                                }
                            }
                        }else if(method.equals("exit")){
                            break;
                        }
                    }
                    
                    // 新しいデータを全てのクライアントにブロードキャスト
                    broadcast(dataList);
                }
                //ois.close();
                //oos.close();
                //socket.close();
                System.out.println("close");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("クライアントとの通信中にエラーが発生しました");
            }
        }
    }
}

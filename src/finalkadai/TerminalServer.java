package finalkadai;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class TerminalServer {

    public static void main(String arg[]) {
        ScheduleBook scheduleBook = new ScheduleBook();
        scheduleBook.setVisible(true);
        ArrayList<TerminalInput> dataList= new ArrayList<>();
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("ポートを入力してください(5000など) → ");
            int port = scanner.nextInt();
            System.out.println("localhostの" + port + "番ポートで待機します");
            ServerSocket server = new ServerSocket(port);

            Socket socket = server.accept();
            System.out.println("接続しました。相手の入力を待っています......");

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                TerminalInput input = (TerminalInput) ois.readObject();
                String date = input.getDate();
                System.out.println("日付は: " + date);
                if (date.equals("exit")) {
                    break;
                }
                String task = input.getTask();
                System.out.println("内容は: " + task);
                String[] rgba = input.getRgba();
                System.out.println("色は");
                for (String color : rgba) {
                    System.out.print(color + ",");
                }
                System.out.println();

                dataList.add(input);

                scheduleBook.addDataList(dataList);

                TerminalInput response = new TerminalInput();
                response.setDate("サーバーからの返事: " + date);
                response.setTask(task);

                oos.writeObject(response);
                oos.flush();
            }
            scanner.close();
            ois.close();
            oos.close();
            socket.close();
            server.close();

        } catch (BindException be) {
            be.printStackTrace();
            System.out.println("ポート番号が不正、ポートが使用中です");
            System.err.println("別のポート番号を指定してください(6000など)");
        } catch (Exception e) {
            System.err.println("エラーが発生したのでプログラムを終了します");
            throw new RuntimeException(e);
        }
    }
}

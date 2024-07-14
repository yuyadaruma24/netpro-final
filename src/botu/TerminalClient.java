package botu;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.Socket;
import java.util.Scanner;

import finalkadai.CalendarInput;

public class TerminalClient {

    public static void main(String arg[]) {
        try {
            Scanner scanner = new Scanner(System.in, "Shift-JIS");
            System.out.print("ポートを入力してください(5000など) → ");
            //int port = scanner.nextInt();
            int port = 5000;
            System.out.println("localhostの" + port + "番ポートに接続を要求します");
            Socket socket = new Socket("localhost", port);
            System.out.println("接続されました");

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            while (true) {
                System.out.println("日付を yyyy-MM-dd 形式で入力してください（終了するには 'exit'）::");
                String date = scanner.next();
                if (date.equals("exit")) {
                    break;
                }
                System.out.println(date + " の予定を追加または編集します。タスク名を入力してください");
                String task = scanner.next();
                System.out.println("タスクの色をRGB形式で入力してください (例: 255,255,255):");
                String[] rgb = scanner.next().split(",");

                CalendarInput input = new CalendarInput();
                input.setDate(date);
                input.setTask(task);
                input.setRgba(rgb);

                oos.writeObject(input);
                oos.flush();

                CalendarInput response = (CalendarInput) ois.readObject();

                String replyMsg = response.getDate();
                System.out.println("サーバからのメッセージ: " + replyMsg);
                String replyContent = response.getTask();
                System.out.println("内容: " + replyContent);
                
            }
            scanner.close();
            ois.close();
            oos.close();
            socket.close();

        } catch (BindException be) {
            be.printStackTrace();
            System.err.println("ポート番号が不正か、サーバが起動していません");
            System.err.println("サーバが起動しているか確認してください");
            System.err.println("別のポート番号を指定してください(6000など)");
        } catch (Exception e) {
            System.err.println("エラーが発生したのでプログラムを終了します");
            throw new RuntimeException(e);
        }
    }
}

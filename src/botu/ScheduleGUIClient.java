package botu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ScheduleGUIClient extends JFrame {
    private final JPanel calendarPanel;
    private final JComboBox<String> monthComboBox;
    private final Map<String, Color> taskColorMap = new HashMap<>();
    private final Map<LocalDate, Map<String, Color>> scheduleMap = new HashMap<>();
    // private final Map<LocalDate, Map<String, String>> scheduleDetails = new
    // HashMap<>();
    private final Map<LocalDate, Map<String, String[]>> scheduleDetails = new HashMap<>();
    private final Map<LocalDate, Map<String, Integer>> schduleID = new HashMap<>();
    private int currentYear = LocalDate.now().getYear(); // 現在の年
    private final DefaultListModel<String> taskListModel = new DefaultListModel<>();
    private LocalDate selectedDate;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int CalendarID;

    public ScheduleGUIClient() {
        setTitle("Schedule Book");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        monthComboBox = new JComboBox<>(getMonths());
        monthComboBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        monthComboBox.addActionListener(e -> updateCalendar());
        add(monthComboBox, BorderLayout.NORTH);

        calendarPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(calendarPanel, BorderLayout.CENTER);

        updateCalendar();

        // サーバ接続を初期化
        initializeConnection();

        // サーバからのデータを受信するスレッドを開始
        new Thread(this::receiveFromServer).start();
    }

    private String[] getMonths() {
        return new String[] { "1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月" };
    }

    private void updateCalendar() {
        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(0, 7, 5, 5)); // 曜日表示のために再設定

        String[] daysOfWeek = { "日", "月", "火", "水", "木", "金", "土" };
        for (int i = 0; i < daysOfWeek.length; i++) {
            JLabel dayLabel = new JLabel(daysOfWeek[i], SwingConstants.CENTER);
            dayLabel.setOpaque(true);
            dayLabel.setFont(dayLabel.getFont().deriveFont(16.0f)); // フォントサイズを16に設定
            if (i == 6) { // 土曜日
                dayLabel.setBackground(Color.LIGHT_GRAY);
                dayLabel.setForeground(Color.BLUE);
            } else if (i == 0) { // 日曜日
                dayLabel.setBackground(Color.LIGHT_GRAY);
                dayLabel.setForeground(Color.RED);
            } else {
                dayLabel.setBackground(Color.LIGHT_GRAY);
            }
            dayLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            calendarPanel.add(dayLabel);
        }

        YearMonth yearMonth = YearMonth.of(currentYear, monthComboBox.getSelectedIndex() + 1);
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        Color lightRedColor = new Color(255, 208, 208);
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        // 日曜日を0に変更（日本のカレンダー形式に合わせる）
        if (startDayOfWeek == 7) {
            startDayOfWeek = 0;
        }

        // 空白のパネルを追加して、月の最初の日を適切な位置に配置
        for (int i = 0; i < startDayOfWeek; i++) {
            calendarPanel.add(new JPanel());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(currentYear, monthComboBox.getSelectedIndex() + 1, day);
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
            dayPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            if (date.equals(today)) {
                dayPanel.setBackground(lightRedColor);
            } else {
                dayPanel.setBackground(Color.WHITE);
            }

            JLabel dateLabel = new JLabel(Integer.toString(day));
            dateLabel.setHorizontalAlignment(JLabel.CENTER);
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek == 6) { // 土曜日
                dateLabel.setForeground(Color.BLUE);
            } else if (dayOfWeek == 7 || dayOfWeek == 0) { // 日曜日
                dateLabel.setForeground(Color.RED);
            } else {
                dateLabel.setForeground(Color.BLACK);
            }

            dayPanel.add(dateLabel);

            Map<String, Color> tasks = scheduleMap.getOrDefault(date, new HashMap<>());
            for (Map.Entry<String, Color> taskEntry : tasks.entrySet()) {
                JLabel taskLabel = createTaskLabel(taskEntry, date);
                dayPanel.add(taskLabel);
            }

            dayPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedDate = date;
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        e.consume();
                        showTaskDialog();
                    }
                }
            });

            calendarPanel.add(dayPanel);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private JLabel createTaskLabel(Map.Entry<String, Color> taskEntry, LocalDate date) {
        Map<String, String[]> detailsMap = scheduleDetails.computeIfAbsent(date, k -> new HashMap<>());
        String[] details = detailsMap.get(taskEntry.getKey());
        //System.out.println(details.length);
        JLabel taskLabel = new JLabel(details[0] + ": " + taskEntry.getKey(), SwingConstants.CENTER);
        taskLabel.setOpaque(true);
        taskLabel.setForeground(Color.WHITE);
        taskLabel.setBackground(taskEntry.getValue());
        taskLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        taskLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    e.consume();
                    showEditTaskDialog(taskEntry, date, detailsMap);
                }
            }
        });
        return taskLabel;
    }

    private void showTaskDialog() {
        List<Object> options = new ArrayList<>(Arrays.asList(taskListModel.toArray()));
        String addNewTaskOption = "新しいタスクを追加...";
        options.add(addNewTaskOption);

        String selectedTask = (String) JOptionPane.showInputDialog(
                this,
                "予定を選択するか、新しい予定を追加してください:",
                "予定の追加/編集",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options.toArray(),
                null);
        String userName = null;
        String taskDetails = null;

        if (addNewTaskOption.equals(selectedTask)) {
            JTextField taskNameField = new JTextField();
            JTextField userNameField = new JTextField();
            JTextArea taskDetailsArea = new JTextArea(3, 20);
            taskDetailsArea.setLineWrap(true);
            taskDetailsArea.setWrapStyleWord(true);

            JPanel panel = new JPanel(new BorderLayout());

            JPanel userPanel = new JPanel(new BorderLayout());
            userPanel.add(new JLabel("ユーザー名:"), BorderLayout.NORTH);
            userPanel.add(userNameField, BorderLayout.CENTER);

            JPanel taskPanel = new JPanel(new BorderLayout());
            taskPanel.add(new JLabel("タスク名:"), BorderLayout.NORTH);
            taskPanel.add(taskNameField, BorderLayout.CENTER);

            JPanel detailsPanel = new JPanel(new BorderLayout());
            detailsPanel.add(new JLabel("詳細:"), BorderLayout.NORTH);
            detailsPanel.add(new JScrollPane(taskDetailsArea), BorderLayout.CENTER);

            panel.add(userPanel, BorderLayout.NORTH);
            panel.add(taskPanel, BorderLayout.CENTER);
            panel.add(detailsPanel, BorderLayout.SOUTH);

            int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "新しいタスクを入力してください",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String taskName = taskNameField.getText().trim();
                userName = userNameField.getText().trim();
                taskDetails = taskDetailsArea.getText().trim();

                if (!taskName.isEmpty() && !userName.isEmpty()) {
                    taskListModel.addElement(taskName);
                    selectedTask = taskName;
                    // scheduleDetails.computeIfAbsent(selectedDate, k -> new
                    // HashMap<>()).put(taskName,"ユーザー: " + userName + "\n" + "詳細: " + taskDetails);
                    scheduleDetails.computeIfAbsent(selectedDate, k -> new HashMap<>()).put(taskName,
                            new String[] { userName, taskDetails });
                }
            }
        }

        if (selectedTask != null && !selectedTask.trim().isEmpty() && !addNewTaskOption.equals(selectedTask)) {
            Color taskColor = taskColorMap.computeIfAbsent(selectedTask, k -> JColorChooser.showDialog(
                    this,
                    "カラーの選択",
                    Color.WHITE));

            scheduleMap.computeIfAbsent(selectedDate, k -> new HashMap<>()).put(selectedTask, taskColor);
            updateCalendar();
            sendTaskToServer(selectedDate.toString(), userName, selectedTask, taskColor, taskDetails);
        }
    }

    private void showEditTaskDialog(Entry<String, Color> taskEntry, LocalDate date, Map<String, String[]> detailsMap) {
        String[] detailArray = detailsMap.get(taskEntry.getKey());

        JTextArea detailsArea = new JTextArea(detailArray[1]);
        detailsArea.setRows(10);
        detailsArea.setColumns(30);
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        JTextField taskNameField = new JTextField(taskEntry.getKey());
        JTextField userNameField = new JTextField(detailArray[0]);
        Object[] message = {
                "ユーザー名:", userNameField,
                "予定名:", taskNameField,
                "予定の詳細:", scrollPane
        };
        int action = JOptionPane.showOptionDialog(
                this,
                message,
                "予定の編集: " + taskEntry,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[] { "保存", "削除", "キャンセル", "色変更" },
                "保存");

        switch (action) {
            case 0: // 保存
                String userName = userNameField.getText().trim();
                String newTaskName = taskNameField.getText().trim();
                String newDetails = detailsArea.getText();
                String[] newDetailArray = new String[]{userName, newDetails};
                if (!newTaskName.isEmpty() && !newTaskName.equals(taskEntry)) {
                    Color color = scheduleMap.get(date).remove(taskEntry);
                    scheduleMap.get(date).put(newTaskName, color);
                    detailsMap.remove(taskEntry);
                    detailsMap.put(newTaskName, newDetailArray);
                    sendTaskToServer(date.toString(),userName, newTaskName, color, newDetails);
                } else {
                    detailsMap.put(taskEntry.getKey(), newDetailArray);
                }
                break;
            case 1: // 削除
                scheduleMap.get(date).remove(taskEntry.getKey());
                detailsMap.remove(taskEntry.getKey());
                sendTaskToServer(date.toString(),detailArray[0], taskEntry.getKey(), taskEntry.getValue(), detailArray[1]);
                break;
            case 2: // キャンセル
                // 何もしない
                break;
            case 3: // 色変更
                Color newColor = JColorChooser.showDialog(this, "色を選択",
                        scheduleMap.get(date).getOrDefault(taskEntry, Color.WHITE));
                if (newColor != null) {
                    //scheduleMap.get(date).put(taskEntry.getKey(), newColor);
                    sendTaskToServer(date.toString(),detailArray[0], taskEntry.getKey(), newColor, detailArray[1]);
                }
                break;
        }
        updateCalendar();
    }

    private void sendTaskToServer(String date,String name ,String task, Color color, String detail) {
        try {
            TerminalInput input = new TerminalInput();
            input.setDate(date);
            input.setName(name);
            input.setTask(task);
            
            if (color != null) {
                input.setRgba(ColorExtractor(color.toString()));
            }
            input.setDetail(detail);

            oos.writeObject(input);
            oos.writeObject(CalendarID);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "サーバへの接続に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receiveFromServer() {
        try {
            while (true) {
                List<TerminalInput> dataList = (List<TerminalInput>) ois.readObject();
                CalendarID = ois.readInt();
                scheduleMap.clear();

                System.out.println(CalendarID);

                for (TerminalInput input : dataList) {
                    String date = input.getDate();
                    String name = input.getName();
                    String task = input.getTask();
                    String[] rgba = input.getRgba();
                    String detail = input.getDetail();
                    int id = input.getCalendarID();
                    int num = input.getCalendarNum();

                    if (date != null && task != null && rgba != null) {
                        Color color = new Color(Integer.parseInt(rgba[0]), Integer.parseInt(rgba[1]),
                                Integer.parseInt(rgba[2]));
                        LocalDate localDate = LocalDate.parse(date);

                        scheduleMap.computeIfAbsent(localDate, k -> new HashMap<>()).put(task, color);
                        scheduleDetails.computeIfAbsent(localDate, k -> new HashMap<>()).put(task, new String[]{name, detail});
                        scheduleID.computeIfAbsent(localDate, k -> new HashMap<>()).put(task, new int[]{id, num});
                        taskListModel.addElement(task);
                    }
                }
                System.out.println("cccc");
                updateCalendar();
                System.out.println("ddddd");

                updateCalendar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeConnection() {
        try {
            System.out.print("ポートを入力してください(5000など) → ");
            int port = 5000;
            System.out.println("localhostの" + port + "番ポートに接続を要求します");
            socket = new Socket("localhost", port);
            System.out.println("接続されました");

            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("サーバへの接続に失敗しました。");
        }
    }

    public static String[] ColorExtractor(String str) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(str);

        List<Integer> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(Integer.parseInt(matcher.group()));
        }

        String[] result = new String[3];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).toString();
        }
        return result;
    }

    public static void main(String[] args) {
        ScheduleGUIClient scheduleGUIClient = new ScheduleGUIClient();
        scheduleGUIClient.setVisible(true);
    }
}

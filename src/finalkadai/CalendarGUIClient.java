package finalkadai;

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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CalendarGUIClient extends JFrame {
    private final JPanel calendarPanel;
    private final JComboBox<String> monthComboBox;
    //private final String[] task = new String[7];
    private final Map<String, Color> taskColorMap = new HashMap<>();
    private final Map<LocalDate, Map<String, Color>> scheduleMap = new HashMap<>();
    private final Map<LocalDate, Map<String, String[]>> scheduleDetails = new HashMap<>();
    private final Map<LocalDate, Map<String, int[]>> scheduleID = new HashMap<>();
    private int currentYear = LocalDate.now().getYear();
    private final DefaultListModel<String> taskListModel = new DefaultListModel<>();
    private LocalDate selectedDate;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int calendarID;
    private int calendarNum;

    public CalendarGUIClient() {
        setTitle("Schedule Book");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        setLayout(new BorderLayout());

        monthComboBox = new JComboBox<>(getMonths());
        monthComboBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        monthComboBox.addActionListener(e -> updateCalendar());
        add(monthComboBox, BorderLayout.NORTH);

        calendarPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(calendarPanel, BorderLayout.CENTER);

        updateCalendar();

        initializeConnection();

        new Thread(this::receiveFromServer).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirmed = JOptionPane.showConfirmDialog(null,
                        "終了します。", "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                    try {
                        sendTaskToServer(null, null, null, null, null, 0, 0, "exit");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    dispose(); // ウィンドウを閉じる
                }
            }
        });

        setVisible(true);
    }

    private String[] getMonths() {
        return new String[] { "1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月" };
    }

    private void updateCalendar() {
        SwingUtilities.invokeLater(() -> {
            calendarPanel.removeAll();
            calendarPanel.setLayout(new GridLayout(0, 7, 5, 5));

            String[] daysOfWeek = { "日", "月", "火", "水", "木", "金", "土" };
            for (int i = 0; i < daysOfWeek.length; i++) {
                JLabel dayLabel = new JLabel(daysOfWeek[i], SwingConstants.CENTER);
                dayLabel.setOpaque(true);
                dayLabel.setFont(dayLabel.getFont().deriveFont(16.0f));
                if (i == 6) {
                    dayLabel.setBackground(Color.LIGHT_GRAY);
                    dayLabel.setForeground(Color.BLUE);
                } else if (i == 0) {
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
            if (startDayOfWeek == 7) {
                startDayOfWeek = 0;
            }

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
                if (dayOfWeek == 6) {
                    dateLabel.setForeground(Color.BLUE);
                } else if (dayOfWeek == 7 || dayOfWeek == 0) {
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
                            showTaskDialog(date);
                        }
                    }
                });

                calendarPanel.add(dayPanel);
            }

            calendarPanel.revalidate();
            calendarPanel.repaint();
        });
    }

    private JLabel createTaskLabel(Map.Entry<String, Color> taskEntry, LocalDate date) {
        Map<String, String[]> detailTask = scheduleDetails.computeIfAbsent(date, k -> new HashMap<>());
        Map<String, int[]> id = scheduleID.computeIfAbsent(date, k -> new HashMap<>());
        String[] details = detailTask.get(taskEntry.getKey());
        if (details == null) {
            details = new String[] { "不明なユーザー", "詳細なし" };
        }
        JLabel taskLabel = new JLabel(details[0] + ": " + taskEntry.getKey().split("_")[0], SwingConstants.CENTER);
        taskLabel.setOpaque(true);
        taskLabel.setForeground(Color.WHITE);
        taskLabel.setBackground(taskEntry.getValue());
        taskLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        taskLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    e.consume();
                    showEditTaskDialog(taskEntry, date, detailTask, id);
                }
            }
        });
        return taskLabel;
    }

    private void showTaskDialog(LocalDate date) {
        //Map<String, String[]> detailsMap = scheduleDetails.computeIfAbsent(date, k -> new HashMap<>());
        //Map<String, int[]> id = scheduleID.computeIfAbsent(date, k -> new HashMap<>());
        List<Object> options = new ArrayList<>();
        String addNewTaskOption = "新しいタスクを追加...";
        options.add(addNewTaskOption);
        String userName = null;
        String taskDetail = null;
        for (int i = 0; i < taskListModel.size(); i++) {
            options.add(taskListModel.elementAt(i).split("_")[0]);
        }

        String selectedTask = (String) JOptionPane.showInputDialog(
                this,
                "予定を選択するか、新しい予定を追加してください:",
                "予定の追加/編集",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options.toArray(),
                null);

        if (selectedTask == null) {
            return;
        }
        JTextField taskNameField;
        if (selectedTask.equals(addNewTaskOption)) {
            taskNameField = new JTextField();
        } else {
            taskNameField = new JTextField(selectedTask);
        }
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
            taskDetail = taskDetailsArea.getText().trim();
            calendarNum++;

            if (!taskName.isEmpty() && !userName.isEmpty()) {
                selectedTask = taskName;
                //detailsMap.put(taskName + "_" + calendarID + "_" + calendarNum, new String[]{userName, taskDetail});
                //id.put(taskName + "_" + calendarID + "_" + calendarNum, new int[]{calendarID, calendarNum});
                Color taskColor = JColorChooser.showDialog(
                        this,
                        "カラーの選択",
                        Color.WHITE);
                /*if (taskColor != null) {
                    taskColorMap.put(taskName + "_" + calendarID + "_" + calendarNum, taskColor);
                }
                taskColor = taskColorMap.get(taskName + "_" + calendarID + "_" + calendarNum);*/

                taskListModel.addElement(taskName);
                //scheduleMap.computeIfAbsent(selectedDate, k -> new HashMap<>()).put(taskName + "_" + calendarID + "_" + calendarNum, taskColor);
                //updateCalendar();
                sendTaskToServer(selectedDate.toString(), userName, selectedTask, taskColor, taskDetail, calendarID,
                        calendarNum, "add");
            }
        }
    }

    private void showEditTaskDialog(Map.Entry<String, Color> taskEntry, LocalDate date,Map<String, String[]> detailTask, Map<String, int[]> taskInt) {
        System.out.println(calendarID);
        String[] detailArray = detailTask.get(taskEntry.getKey());
        int[] taskID = taskInt.get(taskEntry.getKey());
        if (detailArray == null) {
            detailArray = new String[]{"不明なユーザー", "詳細なし"};
        }

        JTextArea detailsArea = new JTextArea(detailArray[1]);
        detailsArea.setRows(10);
        detailsArea.setColumns(30);
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        JTextField taskNameField = new JTextField(taskEntry.getKey().split("_")[0]);
        JTextField userNameField = new JTextField(detailArray[0]);
        Object[] message = {
                "ユーザー名:", userNameField,
                "予定名:", taskNameField,
                "予定の詳細:", scrollPane
        };
        int action = JOptionPane.showOptionDialog(
                this,
                message,
                "予定の編集: " + taskEntry.getKey(),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"保存", "削除", "キャンセル", "色変更"},
                "保存");

        switch (action) {
            case 0: // 保存
                String userName = userNameField.getText().trim();
                String newTaskName = taskNameField.getText().trim();
                String newDetails = detailsArea.getText();
                //String[] newDetailArray = new String[]{userName, newDetails};
                //int calendarID = taskID[0];
                int calendarNum = taskID[1];
                if (!newTaskName.isEmpty() && !newTaskName.equals(taskEntry.getKey())) {
                    Color color = scheduleMap.get(date).remove(taskEntry.getKey());
                    //scheduleMap.get(date).put(newTaskName + "_" + calendarID + "_" + calendarNum, color);
                    //detailTask.remove(taskEntry.getKey());
                    //detailTask.put(newTaskName + "_" + calendarID + "_" + calendarNum, newDetailArray);
                    //taskInt.remove(taskEntry.getKey());
                    //taskInt.put(newTaskName + "_" + calendarID + "_" + calendarNum, new int[]{calendarID, calendarNum});
                    sendTaskToServer(date.toString(), userName, newTaskName, color, newDetails.split("_")[0], calendarID, calendarNum,
                            "save");
                    taskListModel.removeElement(taskEntry.getKey());
                    taskListModel.addElement(newTaskName);
                } else {
                    //detailTask.put(taskEntry.getKey(), newDetailArray);
                    sendTaskToServer(date.toString(), userName, taskEntry.getKey().split("_")[0], taskEntry.getValue(), newDetails.split("_")[0],
                            calendarID, calendarNum, "save");
                }
                break;
            case 1: // 削除
                //scheduleMap.get(date).remove(taskEntry.getKey());
                //detailTask.remove(taskEntry.getKey());
                //taskInt.remove(taskEntry.getKey());
                sendTaskToServer(date.toString(), detailArray[0], taskEntry.getKey().split("_")[0], taskEntry.getValue(),
                        detailArray[1], calendarID, taskID[1], "delete");
                taskListModel.removeElement(taskEntry.getKey());
                break;
            case 2: // キャンセル
                // 何もしない
                break;
            case 3: // 色変更
                Color newColor = JColorChooser.showDialog(this, "色を選択", scheduleMap.get(date).getOrDefault(taskEntry.getKey(), Color.WHITE));
                if (newColor != null) {
                    //scheduleMap.get(date).put(taskEntry.getKey(), newColor);
                    sendTaskToServer(date.toString(), detailArray[0], taskEntry.getKey().split("_")[0], newColor, detailArray[1],
                            calendarID, taskID[1], "change");
                }
                break;
        }
        //updateCalendar();
    }

    private void sendTaskToServer(String date, String name, String task, Color color, String detail, int calendarID, int calendarNum, String method) {
        try {
            CalendarInput input = new CalendarInput();
            input.setDate(date);
            input.setName(name);
            input.setTask(task);
            if (color != null) {
                input.setRgba(ColorExtractor(color.toString()));
            } else {
                input.setRgba(new String[3]);
            }
            input.setDetail(detail);
            input.setCalendarID(calendarID);
            input.setCalendarNum(calendarNum);
            input.setMethod(method);
            oos.writeObject(input);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "サーバへの接続に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receiveFromServer() {
        try {
            calendarID = ois.readInt();
            input: while (true) {
                Object response = ois.readObject();
                if (response instanceof CalendarInput) {
                    CalendarInput input = (CalendarInput) response;
                    if ("error".equals(input.getMethod())) {
                        JOptionPane.showMessageDialog(this, input.getDetail(), "エラー", JOptionPane.ERROR_MESSAGE);
                        continue;
                    }
                }
                List<CalendarInput> dataList = (List<CalendarInput>) response;
                scheduleMap.clear();
                scheduleDetails.clear();
                scheduleID.clear();
                taskListModel.clear();
    
                for (CalendarInput input : dataList) {
                    String date = input.getDate();
                    String name = input.getName();
                    String task = input.getTask();
                    String[] rgba = input.getRgba();
                    String detail = input.getDetail();
                    int id = input.getCalendarID();
                    int num = input.getCalendarNum();
                    String uniqueTaskName = task + "_" + id + "_" + num;
    
                    if (input.getMethod().equals("exit")) {
                        break input;
                    }
    
                    if (date != null && task != null && rgba != null) {
                        Color color = new Color(Integer.parseInt(rgba[0]), Integer.parseInt(rgba[1]),
                                Integer.parseInt(rgba[2]));
                        LocalDate localDate = LocalDate.parse(date);
    
                        scheduleMap.computeIfAbsent(localDate, k -> new HashMap<>()).put(uniqueTaskName, color);
                        scheduleDetails.computeIfAbsent(localDate, k -> new HashMap<>()).put(uniqueTaskName,
                                new String[]{name, detail});
                        scheduleID.computeIfAbsent(localDate, k -> new HashMap<>()).put(uniqueTaskName, new int[]{id, num});
                        taskColorMap.put(uniqueTaskName, color);
                        taskListModel.addElement(task);
                    }
                }
                updateCalendar();
            }
            ois.close();
            oos.close();
            socket.close();
            
            System.out.println("接続を終了します。");
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
        CalendarGUIClient scheduleGUIClient = new CalendarGUIClient();
        scheduleGUIClient.setVisible(true);
    }
}

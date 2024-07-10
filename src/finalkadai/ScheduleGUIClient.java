package finalkadai;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<LocalDate, Map<String, String>> scheduleDetails = new HashMap<>();
    private int currentYear = LocalDate.now().getYear(); // 現在の年
    private final DefaultListModel<String> taskListModel = new DefaultListModel<>();
    private LocalDate selectedDate;

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
    }

    private String[] getMonths() {
        return new String[]{"1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"};
    }

    private void updateCalendar() {
        calendarPanel.removeAll(); 
        YearMonth yearMonth = YearMonth.of(currentYear, monthComboBox.getSelectedIndex() + 1);
        int daysInMonth = yearMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(currentYear, monthComboBox.getSelectedIndex() + 1, day);
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
            dayPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            dayPanel.setBackground(Color.WHITE);

            JLabel dateLabel = new JLabel(Integer.toString(day));
            dateLabel.setOpaque(true);
            dateLabel.setBackground(Color.LIGHT_GRAY);
            dateLabel.setHorizontalAlignment(JLabel.CENTER);
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
        JLabel taskLabel = new JLabel(taskEntry.getKey(), SwingConstants.CENTER);
        taskLabel.setOpaque(true);
        taskLabel.setForeground(Color.WHITE);
        taskLabel.setBackground(taskEntry.getValue());
        taskLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        taskLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    e.consume(); 
                    showEditTaskDialog(taskEntry.getKey(), date); 
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

        if (addNewTaskOption.equals(selectedTask)) {
            String newTask = JOptionPane.showInputDialog(this, "新しい予定を入力してください:");
            if (newTask != null && !newTask.trim().isEmpty()) {
                taskListModel.addElement(newTask);
                selectedTask = newTask;
            }
        }
        if (selectedTask != null && !selectedTask.trim().isEmpty() && !addNewTaskOption.equals(selectedTask)) {
            Color taskColor = taskColorMap.computeIfAbsent(selectedTask, k -> JColorChooser.showDialog(
                    this,
                    "カラーの選択",
                    Color.WHITE));

            scheduleMap.computeIfAbsent(selectedDate, k -> new HashMap<>()).put(selectedTask, taskColor);
            updateCalendar();
        }
    }

    private void showEditTaskDialog(String task, LocalDate date) {
        Map<String, String> detailsMap = scheduleDetails.computeIfAbsent(date, k -> new HashMap<>());
        String details = detailsMap.getOrDefault(task, "");

        JTextArea detailsArea = new JTextArea(details);
        detailsArea.setRows(10);
        detailsArea.setColumns(30);
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        JTextField taskNameField = new JTextField(task);
        Object[] message = {
            "予定の詳細:", scrollPane,
            "予定名:", taskNameField
        };
        int action = JOptionPane.showOptionDialog(
                this,
                message,
                "予定の編集: " + task,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"保存", "削除", "キャンセル", "色変更"},
                "保存");

        switch (action) {
            case 0: // 保存
                String newTaskName = taskNameField.getText().trim();
                String newDetails = detailsArea.getText();
                if (!newTaskName.isEmpty() && !newTaskName.equals(task)) {
                    Color color = scheduleMap.get(date).remove(task);
                    scheduleMap.get(date).put(newTaskName, color);
                    detailsMap.remove(task);
                    detailsMap.put(newTaskName, newDetails);
                } else {
                    detailsMap.put(task, newDetails);
                }
                break;
            case 1: // 削除
                scheduleMap.get(date).remove(task);
                detailsMap.remove(task);
                break;
            case 2: // キャンセル
                // 何もしない
                break;
            case 3: // 色変更
                Color newColor = JColorChooser.showDialog(this, "色を選択", scheduleMap.get(date).getOrDefault(task, Color.WHITE));
                if (newColor != null) {
                    scheduleMap.get(date).put(task, newColor);
                }
                break;
        }
        updateCalendar();
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScheduleGUIClient().setVisible(true));
    }
}
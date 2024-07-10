package finalkadai;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ScheduleBook extends JFrame {
    private final JPanel calendarPanel;
    private final JComboBox<String> monthComboBox;
    private final Map<LocalDate, Map<String, Color>> scheduleMap = new HashMap<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private int currentYear = LocalDate.now().getYear();

    public ScheduleBook() {
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
        return new String[] { "1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月" };
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

            Map<String, Color> dayTasks = scheduleMap.getOrDefault(date, new HashMap<>());
            dayTasks.forEach((task, color) -> {
                JLabel taskLabel = new JLabel(task);
                taskLabel.setOpaque(true);
                taskLabel.setBackground(color);
                taskLabel.setForeground(Color.WHITE);
                dayPanel.add(taskLabel);
            });

            calendarPanel.add(dayPanel);
        }
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    public void consoleManageTasks(LocalDate date, Scanner scanner) {
        System.out.println(date.toString() + " の予定を追加または編集します。タスク名を入力してください（終了するには 'exit'）:");
        String task = scanner.nextLine();
        System.out.println("タスクの色をRGB形式で入力してください (例: 255,255,255):");
        String[] rgb = scanner.nextLine().split(",");
        Color color = new Color(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()),
                Integer.parseInt(rgb[2].trim()));
        scheduleMap.computeIfAbsent(date, k -> new HashMap<>()).put(task, color);
        updateCalendar();
    }

    public void addTask(String day, String task, String[] rgb){
        LocalDate date = LocalDate.parse(day, dateFormatter);
        monthComboBox.setSelectedIndex(date.getMonthValue()-1);
        updateCalendar();
        Color color = new Color(Integer.parseInt(rgb[0].trim()),Integer.parseInt(rgb[1].trim()),Integer.parseInt(rgb[2].trim()));
        scheduleMap.computeIfAbsent(date, k -> new HashMap<>()).put(task,color);
        updateCalendar();
    }

    public void addDataList(ArrayList<TerminalInput> dataList){
        for (TerminalInput input : dataList) {
            addTask(input.getDate(), input.getTask(), input.getRgba());
        }
    }

    public static void main(String[] args) {
        ScheduleBook scheduleBook = new ScheduleBook();
        scheduleBook.setVisible(true);
        Scanner scanner = new Scanner(System.in, "Shift-JIS");
        while (true) {
            System.out.println("日付を yyyy-MM-dd 形式で入力してください:");
            
            String input = scanner.nextLine();
            LocalDate date = LocalDate.parse(input, scheduleBook.dateFormatter);
            scheduleBook.monthComboBox.setSelectedIndex(date.getMonthValue() - 1);
            scheduleBook.updateCalendar();
            
            scheduleBook.consoleManageTasks(date, scanner);
            if (scanner.nextLine().equals("exit")) {
                break;
            }
        }
        scanner.close();
    }
}
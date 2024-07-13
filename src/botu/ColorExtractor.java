package botu;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorExtractor {
    public static void main(String[] args) {
        String colorString = "java.awt.Color[r=255,g=0,b=0]";
        int[] colorComponents = extractColorComponents(colorString);

        // 結果の表示
        System.out.println("Extracted Color Components:");
        for (int component : colorComponents) {
            System.out.println(component);
        }
    }

    public static int[] extractColorComponents(String colorString) {
        // 正規表現パターンを定義
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(colorString);

        // 一時的に数値を格納するリスト
        java.util.List<Integer> list = new java.util.ArrayList<>();

        // 数字を抽出してリストに追加
        while (matcher.find()) {
            list.add(Integer.parseInt(matcher.group()));
        }

        // リストを配列に変換
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }

        return result;
    }
}
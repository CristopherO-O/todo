package src;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CalendarUI {

    // Imprime o mes atual + qualquer mes futuro que tenha eventos marcados
    public static void printCLICalendar(DataSystem sistema) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        // Mapa: mes -> dias que tem evento naquele mes (ordenado por mes e por dia)
        Map<YearMonth, Set<Integer>> eventsByMonth = new TreeMap<>();

        for (Agenda a : sistema.agenda) {
            try {
                LocalDate date = LocalDate.parse(a.date);
                YearMonth ym = YearMonth.from(date);

                if (ym.isBefore(currentMonth)) continue; // ignora eventos de meses passados

                eventsByMonth
                    .computeIfAbsent(ym, k -> new TreeSet<>())
                    .add(date.getDayOfMonth());
            } catch (Exception e) {
                // data invalida, ignora
            }
        }

        // Garante que o mes atual sempre aparece, mesmo sem eventos
        eventsByMonth.putIfAbsent(currentMonth, new TreeSet<>());

        boolean first = true;
        for (Map.Entry<YearMonth, Set<Integer>> entry : eventsByMonth.entrySet()) {
            if (!first) System.out.println();
            first = false;

            YearMonth ym = entry.getKey();
            int todayDay = ym.equals(currentMonth) ? today.getDayOfMonth() : -1;

            printMonth(ym, entry.getValue(), todayDay);
        }
    }

    private static void printMonth(YearMonth month, Set<Integer> markedDays, int todayDay) {
        String monthName = capitalize(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        String header = monthName + " " + month.getYear();
        String weekHeader = "Sun Mon Tue Wed Thu Fri Sat";

        int pad = Math.max(0, (weekHeader.length() - header.length()) / 2);

        System.out.println(ConsoleUI.YELLOW + " ".repeat(pad) + header + ConsoleUI.RESET);
        System.out.println(ConsoleUI.CYAN + weekHeader + ConsoleUI.RESET);

        LocalDate firstOfMonth = month.atDay(1);
        // getDayOfWeek(): SEGUNDA=1 ... DOMINGO=7. Queremos DOM=0, SEG=1, ..., SAB=6
        int startOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = month.lengthOfMonth();

        String[] cells = new String[7];
        int col = 0;

        for (int i = 0; i < startOffset; i++) {
            cells[col++] = "   ";
        }

        for (int day = 1; day <= daysInMonth; day++) {
            String dayStr = String.format("%3d", day);

            if (markedDays.contains(day)) {
                dayStr = ConsoleUI.RED + dayStr + ConsoleUI.RESET;
            } else if (day == todayDay) {
                dayStr = "\u001B[47;30m" + dayStr + ConsoleUI.RESET;
            }

            cells[col++] = dayStr;

            if (col == 7) {
                System.out.println(String.join(" ", cells));
                cells = new String[7];
                col = 0;
            }
        }

        if (col != 0) {
            for (int i = col; i < 7; i++) cells[i] = "   ";
            System.out.println(String.join(" ", cells));
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
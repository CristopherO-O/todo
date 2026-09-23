package src;

import java.time.LocalDate;
import java.util.Scanner;

public class AgendaUI {

    public static void listAgenda(DataSystem sistema){
        if(sistema.agenda.isEmpty()){
            System.out.println("No events.");
            return;
        }
        System.out.println(ConsoleUI.YELLOW + "\n===== AGENDA =====" + ConsoleUI.RESET);
        for(Agenda a : sistema.agenda){
            String remindFlag = a.remind ? "🔔" : "  ";
            System.out.println(ConsoleUI.CYAN + a.id + " | " + a.date + " " + a.time + " | " + a.title + " " + remindFlag + ConsoleUI.RESET);
        }
        System.out.println();
    }

    public static void printMenu(Data data, DataSystem sistema, Scanner scanner){
        String[] options = {"Voltar", "Adicionar Evento", "Remover Evento"};

        while(true){
            int choice = MenuEngine.menuInterativo(options, () -> {
                ConsoleUI.printTitle();
                listAgenda(sistema);
            });

            switch (choice) {
                case -1:
                case 0: return;
                case 1:
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Date (YYYY-MM-DD): ");
                    String date = InputValidation.validDate(scanner);
                    System.out.print("Time (HH:MM): ");
                    String time = InputValidation.validTime(scanner);

                    System.out.print("Remind? (y/n): ");
                    boolean remind = scanner.nextLine().equalsIgnoreCase("y");

                    sistema.addAgenda(title, date, time, remind);
                    data.save(sistema);
                    ConsoleUI.success("Event added.");
                    ConsoleUI.pause(scanner);
                    break;
                case 2:
                    System.out.print("Enter ID to remove: ");
                    int id = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    if(sistema.rmAgenda(id)){
                        ConsoleUI.success("Event removed.");
                        data.save(sistema);
                    }else{
                        ConsoleUI.error("Event not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
            }
        }
    }

    public static void printCLIReminder(DataSystem sistema){
        LocalDate today = LocalDate.now();
        boolean hasReminder = false;

        for(Agenda a : sistema.agenda){
            if(!a.remind) continue;
            try{
                LocalDate eventDate = LocalDate.parse(a.date);
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);

                if(days >= 0 && days <= 7){
                    if(!hasReminder){
                        System.out.println(ConsoleUI.YELLOW + "===== REMINDERS =====" + ConsoleUI.RESET);
                        hasReminder = true;
                    }
                    String label = (days == 0) ? "today" : (days == 1 ? "1 day" : days + " days");
                    String color = (days <= 1) ? ConsoleUI.RED : (days <= 3 ? ConsoleUI.YELLOW : ConsoleUI.CYAN);
                    System.out.println(color + "🔔 " + label + " | " + a.title + ConsoleUI.RESET);
                }
            }catch (Exception e){}
        }
        if(hasReminder) System.out.println();
    }

    public static void printReminder(DataSystem sistema){
        LocalDate today = LocalDate.now();
        boolean hasReminder = false;

        for(Agenda a : sistema.agenda){
            if(!a.remind) continue;
            try{
                LocalDate eventDate = LocalDate.parse(a.date);
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);

                if(days >= 0 && days <= 7){
                    if(!hasReminder){
                        System.out.println(ConsoleUI.YELLOW + "===== REMINDERS =====" + ConsoleUI.RESET);
                        hasReminder = true;
                    }
                    String color = (days <= 1) ? ConsoleUI.RED : (days <= 3 ? ConsoleUI.YELLOW : ConsoleUI.CYAN);
                    System.out.println(color + "🔔 " + a.date + " " + a.time + " | " + a.title + ConsoleUI.RESET);
                }
            }catch (Exception e){}
        }
        if(hasReminder) System.out.println();
    }
}
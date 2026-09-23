package src;
import java.util.Scanner;

public class Main {
    
    private static boolean isRunning = true;
    
    public static void main(String[] args){
        
        Scanner scanner = new Scanner(System.in);
        Data data = new Data();

        // ===== LOAD =====
        DataSystem sistema = data.load();
        sistema.syncIds(); // MUITO IMPORTANTE

        // Lida com Argumentos via CLI
        if(args.length > 0){
            if (args[0].equalsIgnoreCase("reminders")) {
                AgendaUI.printCLIReminder(sistema);
                return;
            }else if (args[0].equalsIgnoreCase("addreminder")) {
                System.out.print("Title: ");
                String title = scanner.nextLine();
                System.out.print("Date (YYYY-MM-DD): ");
                String date = InputValidation.validDate(scanner);
                System.out.print("Time (HH:MM): ");
                String time = InputValidation.validTime(scanner);

                sistema.addAgenda(title, date, time, true);
                data.save(sistema);

                ConsoleUI.success("Event added.");
                return;
            }else if(args[0].equalsIgnoreCase("todo")){
                TodoUI.printTasksCLI(sistema);
                return;
            }else if(args[0].equalsIgnoreCase("note")){
                AgendaUI.printCLIReminder(sistema);
                TodoUI.printTasksCLI(sistema);
                return;
            }else if(args[0].equalsIgnoreCase("cal")){
                CalendarUI.printCLICalendar(sistema);
                return;
            }

        }

        // Loop Principal do Menu
        while(isRunning){
            int choice = MainUI.printMainMenu(scanner, sistema);

            switch(choice){
                case 0:
                    MainUI.saveQuit(sistema, data, scanner);
                    break;
                case 1:
                    TodoUI.printMenu(data, sistema, scanner);
                    break;
                case 2:
                    AgendaUI.printMenu(data, sistema, scanner);
                    break;
                case 3:
                    NotesUI.printMenu(data, sistema, scanner);
                    break;
                case 4:
                    WishListUI.printMenu(data, sistema, scanner);
                    break;
                default:
                    ConsoleUI.error("Invalid option");
                    ConsoleUI.pause(scanner);
            }
        }
    }   
}
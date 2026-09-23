package src;

import java.util.Scanner;

public class MainUI {

    public static int printMainMenu(Scanner scanner, DataSystem sistema){
        String[] options = {
            "Sair",      // Index 0
            "Todo",      // Index 1
            "Agenda",    // Index 2
            "Notes",     // Index 3
            "Wishlist"   // Index 4
        };

        int choice = MenuEngine.menuInterativo(options, () -> {
            ConsoleUI.printTitle();
            AgendaUI.printReminder(sistema);
            TodoUI.printTasksCLI(sistema);
            System.out.println(ConsoleUI.YELLOW + "\n===========================================" + ConsoleUI.RESET);
        });

        if (choice == -1) {
            return 0; // Trata backspace no menu principal como exit
        }
        return choice;
    }

    public static void saveQuit(DataSystem sistema, Data data, Scanner scanner){
        while(true){
            System.out.print("Are you sure you want to exit? (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if(input.equals("y") || input.equals("yes") || input.equals("")){
                data.save(sistema);
                ConsoleUI.success("Data saved. Exiting...");
                System.exit(0);
            }else if(input.equals("n") || input.equals("no")){
                System.out.println("Cancelled.");
                return;
            }else{
                ConsoleUI.error("Please enter 'y' or 'n'.");
            }
        }
    }
}
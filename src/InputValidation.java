package src;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class InputValidation {

    public static String validTime(Scanner scanner){
        while(true){
            String input = scanner.nextLine();
            if(!input.matches("\\d{2}:\\d{2}")){
                ConsoleUI.error("Invalid format. Use HH:MM");
                System.out.print("> ");
                continue;
            }
            try{
                LocalTime.parse(input);
                return input;
            }catch (Exception e){
                ConsoleUI.error("Invalid time");
            }
        }
    }

    public static String validDate(Scanner scanner){
        while(true){
            String input = scanner.nextLine();
            if(!input.matches("\\d{4}-\\d{2}-\\d{2}")){
                ConsoleUI.error("Invalid format. Use YYYY-MM-DD");
                System.out.print("> ");
                continue;
            }
            try{
                LocalDate date = LocalDate.parse(input);
                if(date.isBefore(LocalDate.now())){
                    ConsoleUI.error("Date cannot be in the past");
                    System.out.print("> ");
                    continue;
                }
                return input;
            }catch (Exception e){
                ConsoleUI.error("Invalid date");
            }
        }
    }

    public static int getUserChoice(Scanner scanner, int max){
        while(true){
            try{
                int choice = Integer.parseInt(scanner.nextLine());
                if(choice >= 0 && choice <= max){
                    return choice;
                } else {
                    ConsoleUI.error("Choose between 0 and " + max);
                }
            }catch (Exception e){
                ConsoleUI.error("Invalid input. Enter a number.");
            }
            System.out.print("> ");
        }
    }
}
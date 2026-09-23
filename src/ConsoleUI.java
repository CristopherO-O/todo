package src;

import java.util.Scanner;

public class ConsoleUI {
    public static final String RESET  = "\u001B[0m";
    public static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    public static final String CYAN   = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String PURPLE = "\u001B[35m";

    public static void clearTerminal(){
        try{
            new ProcessBuilder("clear").inheritIO().start().waitFor();
        }catch (Exception e){
            for(int i = 0; i < 50; i++) System.out.println();
        }
    }

    public static void pause(Scanner scanner){
        System.out.println("\nPress ENTER to continue...");
        scanner.nextLine();
    }
    
    public static void success(String msg){
        System.out.println(GREEN + "\n[OK] " + msg + RESET);
    }

    public static void error(String msg){
        System.out.println(RED + "\n[ERRO] " + msg + RESET);
    }

    public static void printTitle(){
        System.out.println();
        System.out.println(YELLOW + " /$$$$$$$$ /$$$$$$      /$$$$$$$   /$$$$$$ ");
        System.out.println("|__  $$__//$$__  $$    | $$__  $$ /$$__  $$");
        System.out.println("   | $$  | $$    $$    | $$    $$| $$    $$");
        System.out.println("   | $$  | $$  | $$    | $$  | $$| $$  | $$");
        System.out.println("   | $$  | $$  | $$    | $$  | $$| $$  | $$");
        System.out.println("   | $$  | $$  | $$    | $$  | $$| $$  | $$");
        System.out.println("   | $$  |  $$$$$$/    | $$$$$$$/|  $$$$$$/");
        System.out.println("   |__/    ______/     |_______/   ______/ ");
        System.out.println();
        System.out.println("===========================================" + RESET);
        System.out.println();
    }
}
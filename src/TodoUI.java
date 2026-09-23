package src;

import java.util.Scanner;

public class TodoUI {

    public static void listTasks(DataSystem sistema){
        if(sistema.tasks.isEmpty()){
            System.out.println("No tasks.");
            return;
        }
        System.out.println(ConsoleUI.YELLOW + "\n=== TODO ===" + ConsoleUI.RESET);

        printPriority(sistema, 1);
        printPriority(sistema, 2);
        printPriority(sistema, 3);

        boolean hasDone = sistema.tasks.stream().anyMatch(t -> t.done);
        if(hasDone){
            System.out.println("\n======= DONE =======");
            for(Task t : sistema.tasks){
                if(t.done) System.out.println(ConsoleUI.GREEN + "#" + t.id + " | [✔] " + t.title + ConsoleUI.RESET);
            }
        }
        System.out.println();
    }

    private static void printPriority(DataSystem sistema, int p){
        boolean hasP = sistema.tasks.stream().anyMatch(t -> !t.done && t.priority == p);
        if(hasP){
            System.out.println("\n=== PRIORIDADE " + p + " ===");
            for(Task t : sistema.tasks){
                if(!t.done && t.priority == p)
                    System.out.println(ConsoleUI.CYAN + "#" + t.id + " | [ ] " + t.title + ConsoleUI.RESET);
            }
        }
    }

    public static void printTasksCLI(DataSystem sistema){
        boolean hasP1 = sistema.tasks.stream().anyMatch(t -> !t.done && t.priority == 1);
        if(!hasP1) return;

        System.out.println(ConsoleUI.YELLOW + "======= TODO ========" + ConsoleUI.RESET);
        for(Task t : sistema.tasks){
            if(!t.done && t.priority == 1)
                System.out.println(ConsoleUI.CYAN + "#" + t.id + "  [ ] " + t.title + ConsoleUI.RESET);
        }
    }

    public static void printMenu(Data data, DataSystem sistema, Scanner scanner){
        String[] options = {"Voltar", "Adicionar Tarefa", "Completar Tarefa", "Remover Tarefa"};

        while(true){
            int choice = MenuEngine.menuInterativo(options, () -> {
                ConsoleUI.printTitle();
                listTasks(sistema);
            });

            switch(choice){
                case -1:
                case 0: return;
                case 1:
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Description: ");
                    String desc = scanner.nextLine();
                    System.out.print("Priority (1=high, 2=mid, 3=low): ");
                    int priority = InputValidation.getUserChoice(scanner, 3);

                    sistema.addTask(priority, title, desc);
                    data.save(sistema);
                    ConsoleUI.success("Task added.");
                    ConsoleUI.pause(scanner);
                    break;
                case 2:
                    System.out.print("Task ID: ");
                    int idComplete = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    if(sistema.getTask(idComplete) != null){
                        sistema.completeTask(idComplete);
                        data.save(sistema);
                        ConsoleUI.success("Task completed.");
                    }else{
                        ConsoleUI.error("Task not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
                case 3:
                    System.out.print("Task ID: ");
                    int idRemove = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    if(sistema.rmTask(idRemove)){
                        data.save(sistema);
                        ConsoleUI.success("Task removed.");
                    }else{
                        ConsoleUI.error("Task not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
            }
        }
    }
}
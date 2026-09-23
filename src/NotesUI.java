package src;

import java.util.Scanner;

public class NotesUI {

    public static void listNotes(DataSystem sistema){
        if(sistema.notes.isEmpty()){
            System.out.println("No notes.");
            return;
        }
        System.out.println(ConsoleUI.YELLOW + "\n===== NOTES =====" + ConsoleUI.RESET);
        for(Notes n : sistema.notes){
            System.out.println(ConsoleUI.CYAN + n.id + " | " + n.title + ConsoleUI.RESET);
        }
        System.out.println();
    }

    private static void viewNote(Notes n){
        System.out.println(ConsoleUI.YELLOW + "\n── " + n.title + " ──" + ConsoleUI.RESET);
        System.out.println(n.content);
        System.out.println();
    }

    public static void printMenu(Data data, DataSystem sistema, Scanner scanner){
        String[] options = {"Voltar", "Adicionar Nota", "Visualizar", "Editar", "Remover"};

        while(true){
            int choice = MenuEngine.menuInterativo(options, () -> {
                ConsoleUI.printTitle();
                listNotes(sistema);
            });

            switch(choice){
                case -1:
                case 0: return;
                case 1:
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.println("Content (type END on a new line to finish):");
                    StringBuilder content = new StringBuilder();
                    String line;
                    while(!(line = scanner.nextLine()).equals("END")){
                        content.append(line).append("\n");
                    }
                    sistema.addNotes(title, content.toString().trim());
                    data.save(sistema);
                    ConsoleUI.success("Note added.");
                    ConsoleUI.pause(scanner);
                    break;
                case 2:
                    System.out.print("Note ID: ");
                    int idView = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    Notes found = sistema.getNote(idView);
                    if(found != null){
                        viewNote(found);
                    }else{
                        ConsoleUI.error("Note not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
                case 3:
                    System.out.print("Note ID: ");
                    int idEdit = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    Notes toEdit = sistema.getNote(idEdit);
                    if(toEdit == null){
                        ConsoleUI.error("Note not found.");
                        ConsoleUI.pause(scanner);
                        break;
                    }

                    System.out.println("Editing: " + ConsoleUI.CYAN + toEdit.title + ConsoleUI.RESET);
                    System.out.print("New title (ENTER to keep current): ");
                    String newTitle = scanner.nextLine();
                    if(!newTitle.isBlank()) toEdit.title = newTitle;

                    System.out.println("New content (type END to finish, ENTER to keep current):");
                    StringBuilder newContent = new StringBuilder();
                    String editLine = scanner.nextLine();

                    if(!editLine.equals("END") && !editLine.isBlank()){
                        newContent.append(editLine).append("\n");
                        while(!(editLine = scanner.nextLine()).equals("END")){
                            newContent.append(editLine).append("\n");
                        }
                        toEdit.content = newContent.toString().trim();
                    }

                    data.save(sistema);
                    ConsoleUI.success("Note updated.");
                    ConsoleUI.pause(scanner);
                    break;
                case 4:
                    System.out.print("Note ID: ");
                    int idRemove = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    if(sistema.rmNotes(idRemove)){
                        data.save(sistema);
                        ConsoleUI.success("Note removed.");
                    }else{
                        ConsoleUI.error("Note not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
            }
        }
    }
}
package src;

import java.util.Scanner;

public class WishListUI {

    public static void listWishList(DataSystem sistema){
        if(sistema.wishList.isEmpty()){
            System.out.println("No items in wishlist.");
            return;
        }
        System.out.println(ConsoleUI.YELLOW + "\n===== WISHLIST =====" + ConsoleUI.RESET);

        int total    = sistema.wishList.size();
        int acquired = (int) sistema.wishList.stream().filter(w -> w.acquired).count();

        for(WishList w : sistema.wishList){
            String status = w.acquired ? "[✓]" : "[ ]";
            String color  = w.acquired ? ConsoleUI.GREEN : ConsoleUI.PURPLE;
            System.out.println(color + w.id + " | " + status + " " + w.title + ConsoleUI.RESET);
        }

        System.out.println(ConsoleUI.YELLOW + "\n" + acquired + "/" + total + " acquired" + ConsoleUI.RESET);
        System.out.println();
    }

    public static void printMenu(Data data, DataSystem sistema, Scanner scanner){
        String[] options = {"Voltar", "Adicionar Item", "Visualizar", "Marcar como adquirido", "Remover"};

        while(true){
            int choice = MenuEngine.menuInterativo(options, () -> {
                ConsoleUI.printTitle();
                listWishList(sistema);
            });

            switch(choice){
                case -1:
                case 0: return;
                case 1:
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Notes/link (optional, ENTER to skip): ");
                    String content = scanner.nextLine();
                    sistema.addWishList(title, content);
                    data.save(sistema);
                    ConsoleUI.success("Item added to wishlist.");
                    ConsoleUI.pause(scanner);
                    break;
                case 2:
                    System.out.print("Item ID: ");
                    int idView = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    WishList item = sistema.getWishItem(idView);
                    if(item != null){
                        String status = item.acquired ? ConsoleUI.GREEN + "[✓] Acquired" + ConsoleUI.RESET : ConsoleUI.PURPLE + "[ ] Pending" + ConsoleUI.RESET;
                        System.out.println(ConsoleUI.YELLOW + "\n── " + item.title + " ──" + ConsoleUI.RESET);
                        System.out.println("Status : " + status);
                        if(item.content != null && !item.content.isBlank()){
                            System.out.println("Notes  : " + item.content);
                        }
                        System.out.println();
                    }else{
                        ConsoleUI.error("Item not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
                case 3:
                    System.out.print("Item ID: ");
                    int idAcq = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    WishList toAcquire = sistema.getWishItem(idAcq);
                    if(toAcquire != null){
                        if(toAcquire.acquired){
                            ConsoleUI.error("Already marked as acquired.");
                        }else{
                            sistema.markWishAsAcquired(idAcq);
                            data.save(sistema);
                            ConsoleUI.success("Marked as acquired!");
                        }
                    }else{
                        ConsoleUI.error("Item not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
                case 4:
                    System.out.print("Item ID: ");
                    int idRemove = InputValidation.getUserChoice(scanner, Integer.MAX_VALUE);
                    if(sistema.rmWishList(idRemove)){
                        data.save(sistema);
                        ConsoleUI.success("Item removed.");
                    }else{
                        ConsoleUI.error("Item not found.");
                    }
                    ConsoleUI.pause(scanner);
                    break;
            }
        }
    }
}
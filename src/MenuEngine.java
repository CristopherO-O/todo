package src;

public class MenuEngine {
    
    public static int menuInterativo(String[] options, Runnable drawHeader) {
        int selected = 0;
        
        try {
            String[] cmd = {"/bin/sh", "-c", "stty -icanon min 1 -echo < /dev/tty"};
            Runtime.getRuntime().exec(cmd).waitFor();

            while (true) {
                ConsoleUI.clearTerminal();
                
                if (drawHeader != null) {
                    drawHeader.run();
                }

                System.out.println(ConsoleUI.YELLOW + "Use as SETAS para navegar, ENTER selecionar e BACKSPACE para voltar.\n" + ConsoleUI.RESET);

                for (int i = 0; i < options.length; i++) {
                    if (i == selected) {
                        System.out.println("\u001B[47;30m > " + options[i] + " \u001B[0m");
                    } else {
                        System.out.println("   " + options[i]);
                    }
                }

                int ch = System.in.read();

                if (ch == 27) { 
                    if (System.in.read() == 91) { 
                        int arrow = System.in.read();
                        if (arrow == 65) { // CIMA
                            selected = (selected > 0) ? selected - 1 : options.length - 1;
                        } else if (arrow == 66) { // BAIXO
                            selected = (selected < options.length - 1) ? selected + 1 : 0;
                        }
                    }
                } else if (ch == 10 || ch == 13) { // ENTER
                    return selected;
                } else if (ch == 127 || ch == 8) { // BACKSPACE
                    return -1; 
                }
            }
        } catch (Exception e) {
            System.out.println("Erro no menu interativo: " + e.getMessage());
            return -1;
        } finally {
            try {
                String[] cmdRestore = {"/bin/sh", "-c", "stty sane < /dev/tty"};
                Runtime.getRuntime().exec(cmdRestore).waitFor();
            } catch (Exception ex) {}
        }
    }
}
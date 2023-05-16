package com.lisi4ka;

import com.lisi4ka.utils.PackagedCommand;
import com.lisi4ka.validation.*;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class ClientValidation {
    public static PackagedCommand[] validation() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            try {
                String[] commandText = scanner.nextLine().trim().split(" ");
                if ("".equals(commandText[0])){
                    continue;
                }
                    try {

                                System.out.println("Unknown command! Type \"help\" to open command list");
                    }catch (IllegalArgumentException ex){
                        System.out.printf(ex.getMessage());
                    }
            } catch (NoSuchElementException e) {
                System.out.println("Program termination");
                System.exit(0);
            }
        }
    }
}

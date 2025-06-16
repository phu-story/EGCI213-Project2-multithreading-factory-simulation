package project2;

import java.util.Scanner;
import java.io.File;
import java.io.IOException;

/* 
   Made with ♥ by,

   Yoswaris Lawpaiboon,  6681170
   Pasin Piyavej,        6681187
   Praepilai Phetsamsri, 6681374
   
*/

public class App {
    public static void main(String[] args) throws Exception {
        //  Verfied: Code work at dev path, change to src/main/java/project2_6681187/config_1.txt when deliver project
        File readingPath = new File("src/main/java/project2/config_1.txt");
        Scanner readConfig = null;
        Scanner keyIn = new Scanner(System.in);

        // Failsafe: if path invalid
        try {
            readConfig = new Scanner(readingPath);
        } catch(IOException e) {
            while (true) {
                System.out.println("Error: Config file path is invalid enter new path or enter 0 to exit");
                String temp = keyIn.next();
                System.out.println(temp);
                if (temp.equals("0")) {
                    keyIn.close();
                    System.exit(0);
                }
                try {
                    readConfig = new Scanner(new File(temp));
                    if (readConfig.hasNext()) {
                        break;
                    }
                } catch (IOException f) {
                    System.out.println("Error: Unable to open file. Please try again.");
                    continue;
                }
            }
        }

        printConfig(readConfig);
        keyIn.close();
        readConfig.close();
    }

    public static void printConfig(Scanner readConfig){
        while(readConfig.hasNext()) {
            System.out.println(readConfig.nextLine());
        }
    }
}

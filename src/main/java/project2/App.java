package project2;

import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

        // Data extration
        int inDay = Integer.parseInt(readConfig.nextLine().split(",")[1].trim());           // #Sim day

        ArrayList<Warehouse> warehouses = new ArrayList<>();
        int length = Integer.parseInt(readConfig.nextLine().split(",")[1].trim());
        for(int i = 0; i < length; i++) {
            String name = "Warehouse_" + i;
            warehouses.add(new Warehouse(name));
        }

        ArrayList<Freight> freights = new ArrayList<>();
        String[] freight = readConfig.nextLine().split(",");
        for(int i = 0; i < Integer.parseInt(freight[1].trim()); i++){
            String name ="Freight_" + i;
            freights.add(new Freight(name, Integer.parseInt(freight[2].trim())));
        }

        ArrayList<SupplierThread> supplierThreads = new ArrayList<>();
        String[] supplier = readConfig.nextLine().split(",");
        for(int i = 0; i < Integer.parseInt(supplier[1].trim()); i++){
            String name = "SupplierThread_" + i;
            supplierThreads.add(new SupplierThread(
                name, 
                Integer.parseInt(supplier[2].trim()), 
                Integer.parseInt(supplier[3].trim())
            ));
        }

        ArrayList<FactoryThread> factoryThreads = new ArrayList<>();
        String[] factory = readConfig.nextLine().split(",");
        for(int i = 0; i < Integer.parseInt(factory[1].trim()); i++){
            String name = "FactoryThread_" + i;
            factoryThreads.add(new FactoryThread(name, Integer.parseInt(factory[2].trim())));
        }

        System.out.printf("%20s  >>  ==================== Parameters ====================\n", "main");
        System.out.printf("%20s  >>  %-25s : %d\n", "main", "Days of simulation", inDay);
        System.out.printf("%20s  >>  %-25s : ", "main", "Warehouses");
        for(int i = 0; i < warehouses.size(); i++) {
            System.out.print(warehouses.get(i).getName());
            if (i < warehouses.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : ", "main", "Freights");
        for(int i = 0; i < freights.size(); i++) {
            System.out.print(freights.get(i).getName());
            if (i < freights.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : max = %d", "main", "Freights capacity", freights.get(0).getmaxCapacity());
        System.out.printf("\n%20s  >>  %-25s : ", "main", "SupplierThreads");
        for(int i = 0; i < supplierThreads.size(); i++) {
            System.out.print(supplierThreads.get(i).getName());
            if (i < supplierThreads.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : min = %-3d, max = %-3d", "main", "Daily supply", supplierThreads.get(0).getMinSupplier(), supplierThreads.get(0).getMaxSupplier());
        System.out.printf("\n%20s  >>  %-25s : ", "main", "FactoryThreads");
        for(int i = 0; i < factoryThreads.size(); i++) {
            System.out.print(factoryThreads.get(i).getName());
            if (i < factoryThreads.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : max = %-3d", "main", "Daily production", factoryThreads.get(0).getFactoryMaxProd());
        System.out.printf("\n%20s  >>  \n", "main");

        printBalance(1,warehouses,freights);

        keyIn.close();
        readConfig.close();
    }

    public static void printBalance(int goingDay, ArrayList<Warehouse> warehouseList, ArrayList<Freight> freightsList) {
        System.out.printf("%20s  >>  ====================================================\n", "main");
        System.out.printf("%20s  >>  Day %d\n", "main", goingDay);
        for(int i = 0; i < warehouseList.size(); i++) {
            System.out.printf("%20s  >>  %-25s = %5d\n", "main", warehouseList.get(i).getName(), warehouseList.get(i).getWarehouseBal());
        }
        for(int i = 0; i < freightsList.size(); i++) {
            System.out.printf("%20s  >>  %-25s = %5d\n", "main", freightsList.get(i).getName() + "  capacity", freightsList.get(i).getmaxCapacity());
        }
        System.out.printf("%20s  >>  \n", "main");
    }
}

class Warehouse {
    String name;
    int matBalance = 0;

    public Warehouse(String inName) {
        name = inName;
    }

    public String getName() {
        return name;
    }

    public int getWarehouseBal() {
        return matBalance;
    }
}

class Freight {
    String name;
    int maxCap;

    public Freight(String inName, int inCap) {
        name = inName;
        maxCap = inCap;
    }

    public String getName() {
        return name;
    }

    public int getmaxCapacity() {
        return maxCap;
    }
}

class SupplierThread {
    String name;
    int supplierMin = 0;
    int supplierMax = 0;

    public SupplierThread(String inName, int inMin, int inMax){
        name = inName;
        supplierMin = inMin;
        supplierMax = inMax;
    }

    public String getName() {
        return name;
    }

    public int getMinSupplier() {
        return supplierMin;
    }

    public int getMaxSupplier() {
        return supplierMax;
    }
}

class FactoryThread {
    String name;
    int maxProd;

    public FactoryThread(String inName, int inMaxProd) {
        name = inName;
        maxProd = inMaxProd;
    }

    public String getName() {
        return name;
    }

    public int getFactoryMaxProd() {
        return maxProd;
    }
}
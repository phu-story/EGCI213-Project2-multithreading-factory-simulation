package project2;

import java.util.Scanner;
import java.util.concurrent.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/* 
   Made with ♥ by,

   Pasin Piyavej,        6681187
   
*/

public class App {
    public static void main(String[] args) throws Exception {
        //  Path work at dev phase, change to src/main/java/project2_6681187/config_1.txt when deliver project
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

        Semaphore WarehouseSemaphore = new Semaphore(1);
        ArrayList<Warehouse> warehouses = new ArrayList<>();
        int length = Integer.parseInt(readConfig.nextLine().split(",")[1].trim());
        for(int i = 0; i < length; i++) {
            String name = "Warehouse_" + i;
            warehouses.add(new Warehouse(name, WarehouseSemaphore));
        }

        Semaphore FreightSemaphore = new Semaphore(1);
        ArrayList<Freight> freights = new ArrayList<>();    
        String[] freight = readConfig.nextLine().split(",");
        for(int i = 0; i < Integer.parseInt(freight[1].trim()); i++){
            String name ="Freight_" + i;
            freights.add(new Freight(name, Integer.parseInt(freight[2].trim()), FreightSemaphore));
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

        // First Line program's output
        App mainApp = new App();
        System.out.printf("%20s  >>  ==================== Parameters ====================\n", "main");
        System.out.printf("%20s  >>  %-25s : %d\n", "main", "Days of simulation", inDay);
        System.out.printf("%20s  >>  %-25s : %s", "main", "Warehouses", methodsPool.arrListToString(warehouses));
        System.out.printf("\n%20s  >>  %-25s : %s", "main", "Freights", methodsPool.arrListToString(freights));
        System.out.printf("\n%20s  >>  %-25s : max = %d", "main", "Freights capacity", freights.get(0).getmaxCapacity());
        System.out.printf("\n%20s  >>  %-25s : %s", "main", "SupplierThreads", methodsPool.arrListToString(supplierThreads));
        System.out.printf("\n%20s  >>  %-25s : min = %-3d, max = %-3d", "main", "Daily supply", supplierThreads.get(0).getMinSupplier(), supplierThreads.get(0).getMaxSupplier());
        System.out.printf("\n%20s  >>  %-25s : %s", "main", "FactoryThreads", methodsPool.arrListToString(factoryThreads));
        System.out.printf("\n%20s  >>  %-25s : max = %-3d", "main", "Daily production", factoryThreads.get(0).getFactoryMaxProd());
        System.out.printf("\n%20s  >>  \n", "main");

        mainApp.runSimulation(inDay, warehouses, freights, supplierThreads, factoryThreads);

        keyIn.close();
        readConfig.close();
    }

    public void runSimulation(int inDay, ArrayList<Warehouse> warehouses, ArrayList<Freight> freights, ArrayList<SupplierThread> supplierThreads, ArrayList<FactoryThread> factoryThreads) {
        CountDownLatch waitSupplyLatch = new CountDownLatch(supplierThreads.size());
        Semaphore SupplierSem = new Semaphore(1);
        Semaphore FactorySem = new Semaphore(1);
        CyclicBarrier FactoryBarrier = new CyclicBarrier(factoryThreads.size());
        
        for (int a = 1; a < inDay + 1; a++) {
            methodsPool.printBalance(a, warehouses, freights);

            
            ArrayList<Thread> supplierThreadInstances = new ArrayList<>();
            for(int i = 0; i < supplierThreads.size(); i++) {
                supplierThreads.get(i).setSupplierLatch(waitSupplyLatch);
                supplierThreads.get(i).setWarehouseList(warehouses);
                supplierThreads.get(i).setSupplierSemaphore(SupplierSem);

                Thread supplierThreadPool = new Thread(supplierThreads.get(i));
                supplierThreadInstances.add(supplierThreadPool);
                supplierThreadPool.start();
            }

            try {
                for (Thread supplierThreadPool : supplierThreadInstances) {
                    supplierThreadPool.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            ArrayList<Thread> factoryThreadInstances = new ArrayList<>();
            for(int i = 0; i < factoryThreads.size(); i++) {
                factoryThreads.get(i).setWarehouseList(warehouses);
                factoryThreads.get(i).setFactorySemaphore(FactorySem);
                factoryThreads.get(i).setFactoryFreight(freights);
                factoryThreads.get(i).setFactoryBarrier(FactoryBarrier);

                Thread factoryThreadPool = new Thread(factoryThreads.get(i));
                factoryThreadInstances.add(factoryThreadPool);
                factoryThreadPool.start();
            }
            
            try {
                for (Thread factoryThreadPool : factoryThreadInstances) {
                    factoryThreadPool.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.printf("%20s  >>  \n", "main");
        System.out.printf("%20s  >>  ==================== Summary ====================\n", "main");
        for(int i = 0; i < factoryThreads.size(); i++) {
            int produced  = factoryThreads.get(i).getProducedUnit();
            int unshipped = factoryThreads.get(i).getUnShippedUnit();
            int shipped   = produced - unshipped;

            int percent;
            if (produced == 0) {
                percent = 0;
            } else {
                percent = (int) Math.round((shipped * 100.0) / produced);
                percent = Math.max(0, Math.min(100, percent));
            }

            System.out.printf("%20s  >>  %-15s  Total product = %5d\tshipped = %5d (%3d%%)%n",
                    "main",
                    factoryThreads.get(i).getName(),
                    produced,
                    shipped,   // <-- use the shipped variable here
                    percent);
        }
    }

}
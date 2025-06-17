package project2;

import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/* 
   Made with ♥ by,

   Pasin Piyavej,        6681187
   
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
            System.out.print(supplierThreads.get(i).getSupplierName());
            if (i < supplierThreads.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : min = %-3d, max = %-3d", "main", "Daily supply", supplierThreads.get(0).getMinSupplier(), supplierThreads.get(0).getMaxSupplier());
        System.out.printf("\n%20s  >>  %-25s : ", "main", "FactoryThreads");
        for(int i = 0; i < factoryThreads.size(); i++) {
            System.out.print(factoryThreads.get(i).getFactoryName());
            if (i < factoryThreads.size() - 1) System.out.print(", ");
        }
        System.out.printf("\n%20s  >>  %-25s : max = %-3d", "main", "Daily production", factoryThreads.get(0).getFactoryMaxProd());
        System.out.printf("\n%20s  >>  \n", "main");

        printBalance(1,warehouses,freights);

        App mainApp = new App();
        mainApp.runSimulation(inDay, warehouses, freights, supplierThreads, factoryThreads);

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

    public void runSimulation(int inDay, ArrayList<Warehouse> warehouses, ArrayList<Freight> freights, ArrayList<SupplierThread> supplierThreads, ArrayList<FactoryThread> factoryThreads) {
        CountDownLatch waitSupplyLatch = new CountDownLatch(supplierThreads.size());
        CountDownLatch waitFactoryLatch = new CountDownLatch(factoryThreads.size());
        Semaphore SupplierSem = new Semaphore(1);
        Semaphore FactorySem = new Semaphore(1);
        CyclicBarrier FactoryBarrier = new CyclicBarrier(factoryThreads.size());
        // CyclicBarrier allBarrier = new CyclicBarrier(supplierThreads.size() + factoryThreads.size());
        

        for(int i = 0; i < supplierThreads.size(); i++) {
            supplierThreads.get(i).setSupplierLatch(waitSupplyLatch);
            supplierThreads.get(i).setWarehouseList(warehouses);
            supplierThreads.get(i).setSupplierSumaphore(SupplierSem);
            supplierThreads.get(i).start();
        }

        try{
            waitSupplyLatch.await();
        } catch(InterruptedException e) {
            System.err.println(e);
        }
        
        for(int i = 0; i < factoryThreads.size(); i++) {
            factoryThreads.get(i).setWarehouseList(warehouses);
            factoryThreads.get(i).setFactorySemaphore(FactorySem);
            factoryThreads.get(i).setFactoryFreight(freights);
            factoryThreads.get(i).setFactoryLatch(waitFactoryLatch);
            factoryThreads.get(i).setFactoryBarrier(FactoryBarrier);
            factoryThreads.get(i).start();
        }

        try{
            Thread.sleep(50);
        } catch(InterruptedException e) {
            System.err.println(e);
        }

        for (int i = 2; i < inDay + 1; i++) {
            waitSupplyLatch = new CountDownLatch(supplierThreads.size());

            printBalance(i, warehouses, freights);
            for(int j = 0; j < freights.size(); j++) {
                freights.get(j).resetFreight();
            }

            for(int j = 0; j < supplierThreads.size(); j++) {
                SupplierThread oldSupplierThread = supplierThreads.get(j);
                SupplierThread newSupplierThread = new SupplierThread(oldSupplierThread.getSupplierName(), oldSupplierThread.supplierMin, oldSupplierThread.supplierMax);
                newSupplierThread.setSupplierLatch(waitSupplyLatch);
                newSupplierThread.setWarehouseList(warehouses);
                newSupplierThread.setSupplierSumaphore(SupplierSem);
                newSupplierThread.start();
            }

            try{
                waitSupplyLatch.await();
            } catch(InterruptedException e) {
                System.err.println(e);
            }

            for(int j = 0; j < factoryThreads.size(); j++){
                FactoryThread oldFactoryThread = factoryThreads.get(j);
                FactoryThread newFactoryThread = new FactoryThread(oldFactoryThread.getFactoryName(), oldFactoryThread.maxProd);
                newFactoryThread.setWarehouseList(warehouses);
                newFactoryThread.setFactorySemaphore(FactorySem);
                newFactoryThread.setFactoryFreight(freights);

                waitFactoryLatch = new CountDownLatch(factoryThreads.size());
                newFactoryThread.setFactoryLatch(waitFactoryLatch);
                
                newFactoryThread.setFactoryBarrier(FactoryBarrier);
                newFactoryThread.start();
            }

            try{
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(e);
            }
        }
    }

}

class Warehouse {
    String name;
    int matBalance = 0;
    Semaphore sem = null;
    int finalAmount = 0;

    public Warehouse(String inName, Semaphore inSem) {
        name = inName;
        sem = inSem;
    }

    public String getName() {
        return name;
    }

    public int getWarehouseBal() {
        return finalAmount;
    }

    public void putWarehouse(int takingInMat) {
        try{
            sem.acquire();
            matBalance += takingInMat;
            System.out.printf("%-4s %5d materials %15s balance = %5d\n", "put",takingInMat, name, matBalance);
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }

        finalAmount += takingInMat;
    }

    public int getMaterial(int inGet) {
        try{
            sem.acquire();
            if (matBalance <= inGet) {
                inGet = matBalance;
                matBalance = 0;
                System.out.printf("%-4s %5d meterials %15s balance = %5d\n", "get",inGet, name, matBalance);
            } else {
                inGet = matBalance - inGet;
                matBalance -= inGet;
                System.out.printf("%-4s %5d meterials %15s balance = %5d\n", "get",inGet, name, matBalance);
            }
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }
        return inGet;
    }
}

class Freight {
    String name;
    int maxCap;
    int holdingUnit = 0;
    Semaphore sem = null;

    public Freight(String inName, int inCap, Semaphore inSem) {
        name = inName;
        maxCap = inCap;
        sem = inSem;
    }

    public String getName() {
        return name;
    }

    public int getmaxCapacity() {
        return maxCap;
    }

    public void resetFreight() {
        holdingUnit = maxCap;
    }

    public int freightShip(int takingIn) {
        int returningVal = 0;
        try {
            sem.acquire();
            if (holdingUnit + takingIn >= maxCap) {
                takingIn = maxCap - holdingUnit;
                holdingUnit = maxCap;
                returningVal = (holdingUnit + takingIn) - maxCap;
            } else {
                holdingUnit += takingIn;
            }
            System.out.printf("ship %5d meterials %13s remaining capacity = %5d\n", takingIn, name, (maxCap-holdingUnit));
    
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }
        return returningVal;
    }
}

class SupplierThread extends Thread{
    String name;
    int supplierMin = 0;
    int supplierMax = 0;
    CountDownLatch Latch = null;
    ArrayList<Warehouse> warehouseList;
    Semaphore sem = null;

    public SupplierThread(String inName, int inMin, int inMax){
        name = inName;
        supplierMin = inMin;
        supplierMax = inMax;
    }

    public void setSupplierSumaphore(Semaphore inSem){
        sem = inSem;
    }

    public void setWarehouseList(ArrayList<Warehouse> inList){
        warehouseList = inList;
    }

    public void setSupplierLatch(CountDownLatch waitSize) {
        Latch = waitSize;
    }

    public String getSupplierName() {
        return name;
    }

    public int getMinSupplier() {
        return supplierMin;
    }

    public int getMaxSupplier() {
        return supplierMax;
    }

    public void run() {
        double rand = Math.random();
        try{
            sem.acquire();
            System.out.printf("%20s  >>  ", name);
            warehouseList.get((int) (rand * warehouseList.size())).putWarehouse((int) (rand*(supplierMax-supplierMin + 1) + supplierMin));
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
            Latch.countDown();
        }
    }
}

class FactoryThread extends Thread{
    String name;
    int maxProd;
    CountDownLatch Latch = null;
    int ThreadSize = 0;
    ArrayList<Warehouse> warehouseList;
    ArrayList<Freight> freightList;
    Semaphore sem = null;
    CyclicBarrier barrier = null;
    
    public FactoryThread(String inName, int inMaxProd) {
        name = inName;
        maxProd = inMaxProd;
    }

    public void setFactoryBarrier(CyclicBarrier inBarrier) {
        barrier = inBarrier;
    }

    public void setFactorySemaphore(Semaphore inSem) {
        sem = inSem;
    }
    
    public void setFactoryFreight(ArrayList<Freight> inList) {
        freightList = inList;
    }
    public void setWarehouseList(ArrayList<Warehouse> inList){
        warehouseList = inList;
    }

    public void setFactoryLatch(CountDownLatch waitSize) {
        Latch = waitSize;
        ThreadSize = (int) Latch.getCount();
    }
    
    public String getFactoryName() {
        return name;
    }

    public int getFactoryMaxProd() {
        return maxProd;
    }

    public void run() {
        double rand = Math.random();
        int holdingMaterial = 0;
        int unShipped = 0;

        try{
            sem.acquire();
            System.out.printf("%20s  >>  ", name);
            holdingMaterial = warehouseList.get((int) (rand * warehouseList.size())).getMaterial(maxProd);
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }

        try{
            barrier.await();
            System.out.printf("%20s  >>  \n", name);
            Thread.sleep(15);
            System.out.printf("%20s  >>  total product to ship = %5d\n", name, holdingMaterial);
        } catch(InterruptedException | BrokenBarrierException e) {
            System.err.println(e);
        }

        rand = Math.random();

        try{
            barrier.await();
            sem.acquire();
            System.out.printf("%20s  >>  ", name);
            unShipped = freightList.get((int) (rand * freightList.size())).freightShip(holdingMaterial);
        } catch(InterruptedException | BrokenBarrierException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }
        
        try{
            Thread.sleep(5);
            System.out.printf("%20s  >>  unshipped product = %5d\n", name, unShipped);
        } catch(InterruptedException e) {
            System.err.println(e);
        }

    }
}
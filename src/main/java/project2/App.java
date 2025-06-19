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

        // First Line program's output
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

        App mainApp = new App();
        mainApp.runSimulation(inDay, warehouses, freights, supplierThreads, factoryThreads);

        keyIn.close();
        readConfig.close();
    }

    public static void printBalance(int goingDay, ArrayList<Warehouse> warehouseList, ArrayList<Freight> freightsList) {
        System.out.printf("%20s  >>  \n", "main");
        System.out.printf("%20s  >>  ====================================================\n", "main");
        System.out.printf("%20s  >>  Day %d\n", "main", goingDay);
        for(int i = 0; i < warehouseList.size(); i++) {
            System.out.printf("%20s  >>  %-25s = %5d\n", "main", warehouseList.get(i).getName(), warehouseList.get(i).getWarehouseBal());
        }
        for(int i = 0; i < freightsList.size(); i++) {
            System.out.printf("%20s  >>  %-25s = %5d\n", "main", freightsList.get(i).getName() + "  capacity", freightsList.get(i).getmaxCapacity());
        }
    }

    public void runSimulation(int inDay, ArrayList<Warehouse> warehouses, ArrayList<Freight> freights, ArrayList<SupplierThread> supplierThreads, ArrayList<FactoryThread> factoryThreads) {
        CountDownLatch waitSupplyLatch = new CountDownLatch(supplierThreads.size());
        Semaphore SupplierSem = new Semaphore(1);
        Semaphore FactorySem = new Semaphore(1);
        CyclicBarrier FactoryBarrier = new CyclicBarrier(factoryThreads.size());
        
        for (int a = 1; a < inDay + 1; a++) {
            printBalance(a, warehouses, freights);

            
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
    }

}

class Warehouse {
    protected String name;
    protected Semaphore sem = null;
    private int matBalance = 0;
    private int finalAmount = 0;

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
    protected String name;
    protected int maxCap;
    private int holdingUnit = 0;
    private static Semaphore sem = null;
    private int unShippedUnit = 0;

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
        holdingUnit = 0;
        unShippedUnit = 0;
    }

    public int freightShip(int takingIn) {
        try {
            sem.acquire();
            if (holdingUnit + takingIn >= maxCap) {
                int shipped = maxCap - holdingUnit;
                unShippedUnit = takingIn - shipped;
                holdingUnit = maxCap;
                System.out.printf("ship %5d meterials %13s remaining capacity = %5d\n", shipped, name, (maxCap - holdingUnit));
            } else {
                holdingUnit += takingIn;
                System.out.printf("ship %5d meterials %13s remaining capacity = %5d\n", takingIn, name, (maxCap - holdingUnit));
            }
    
        } catch(InterruptedException e) {
            System.err.println(e);
        } finally {
            sem.release();
        }
        return unShippedUnit;
    }
}

class SupplierThread implements Runnable{
    protected String name;
    protected int supplierMin = 0;
    protected int supplierMax = 0;
    private CountDownLatch Latch = null;
    private ArrayList<Warehouse> warehouseList;
    private Semaphore sem = null;

    public SupplierThread(String inName, int inMin, int inMax){
        name = inName;
        supplierMin = inMin;
        supplierMax = inMax;
    }

    public void setSupplierSemaphore(Semaphore inSem){
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

class FactoryThread implements Runnable{
    protected String name;
    protected int maxProd;
    private ArrayList<Warehouse> warehouseList;
    private ArrayList<Freight> freightList;
    private Semaphore sem = null;
    private CyclicBarrier barrier = null;
    
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
        } catch(InterruptedException | BrokenBarrierException e) { // 
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
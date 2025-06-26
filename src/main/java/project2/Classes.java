package project2;

import java.util.ArrayList;
import java.util.concurrent.*;

interface methodsPool {
    public static String arrListToString(ArrayList<?> inArr) {
        String[] returnArr = new String[inArr.size()];
        for (int i = 0; i < inArr.size(); i++) {
            Object obj = inArr.get(i);
            try {
                returnArr[i] = (String) obj.getClass().getMethod("getName").invoke(obj);
            } catch (Exception e) {
                returnArr[i] = "";
            }
        }
        return "[" + String.join(", ", returnArr) + "]";
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

    public String getName() {
        return name;
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

    public String getName() {
        return name;
    }
    
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
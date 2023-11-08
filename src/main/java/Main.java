import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        String path = "src/main/java/";
        String filename = "config.txt";
        CountDownLatch mainLatch = new CountDownLatch(1); // mainLatch is used for signal other thread to start after mainThread prints day

        boolean openSuccess = false;

        while (!openSuccess) {
            try {
                Scanner scanner = new Scanner(new File(path + filename));
                System.out.printf("%-16s>>  read configs from %s\n", Thread.currentThread().getName(), path+filename);
                openSuccess = true;

                String line;
                String[] col;

                ArrayList<Material> Materials = new ArrayList<Material>();
                ArrayList<SupplierThread> Suppliers = new ArrayList<SupplierThread>();
                for(int i = 0; scanner.hasNext(); i++) {
                    line = scanner.nextLine();
                    col = line.split(",");
                    String type = col[0].trim();
                    int days = 0;
                    String m1, m2, sup_name, fac_name, product_name; // materials 1 & 2, supplier name, factory name, product name
                    int s1, s2, f_lot, f_1, f_2; // supply materials 1 & 2, factory daily lot, materials require

                    if(i < 2) {
                        if(type.equals("D")) {
                            days = Integer.parseInt(col[1].trim());
                            System.out.printf("%-16s>>  simulation days = %d\n", Thread.currentThread().getName(),days);
                        } else if(type.equals("M")) {
                            for(int j = 1; j < col.length; j++) {
                                Materials.add(new Material(col[j].trim()));
                            }
                        }
                    } else {
                        if(type.equals("S")) {
                            System.out.printf("%-16s>>  %s daily supply rates =", Thread.currentThread().getName(), col[1].trim());
                            for(int j = 0; j < Materials.size(); j++) {
                                int balance = Integer.parseInt(col[j+2].trim());
                                System.out.printf(" %3d %s", balance, Materials.get(j).getName());
                                Materials.get(j).setBalance(balance);
                            }
                            System.out.println();
                            Suppliers.add(new SupplierThread(col[1].trim(), Materials));
                        } else if(type.equals("F")) {
                            fac_name = col[1].trim();
                            product_name = col[2].trim();
                            f_lot = Integer.parseInt(col[3].trim());
                            f_1 = Integer.parseInt(col[4].trim());
                            f_2 = Integer.parseInt(col[5].trim());
                        }
                    }
                }
                for(Material m: Materials) System.out.println(m.getName() + " " + m.balance);
                for(SupplierThread s: Suppliers) System.out.println(s.getName() + ">>>");
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}

class Material {
    int balance;
    private String name;

    public Material (String matName) {
        name = matName;
    }

    public int getBalance() {
        return balance;
    }

    public String getName() {
        return name;
    }

    public void addBalance(int ba) {
        balance += ba;
    }

    public void useBalance(int ba) {

    }
}

class SupplierThread extends Thread {
    private CountDownLatch mainLatch;
    private CyclicBarrier barrier;
//    private String material1, material2;
    private ArrayList<Material> ML;

//    public SupplierThread(String name, CountDownLatch mainLatch, CyclicBarrier barrier, ArrayList<Material> M) {
    public SupplierThread(String name,  ArrayList<Material> M) {
        super(name);
        ML = M;
//        this.mainLatch = mainLatch;
//        this.barrier = barrier;
    }

    public void run() {
        try {
            mainLatch.await(); // wait for main thread print day
            // rate and balance acquire from class Material
//            System.out.printf("%s >> Put %d %s balance = %d %s", Thread.currentThread().getName(), material1_Rate, material1, material1_Balance, material1);
//            System.out.printf("%s >> Put %d %s balance = %d %s", Thread.currentThread().getName(), material2_Rate, material2, material2_Balance, material2);
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}

class FactoryThread extends Thread {

}
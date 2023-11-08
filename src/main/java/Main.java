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
                int days = 0;
                int sup_parties = 0;
                int fac_parties = 0;

                for(int i = 0; scanner.hasNext(); i++) {
                    line = scanner.nextLine();
                    col = line.split(",");
                    String type = col[0].trim();
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
                            ArrayList<Integer> sup_rate = new ArrayList<Integer>();
                            System.out.printf("%-16s>>  %s daily supply rates =", Thread.currentThread().getName(), col[1].trim());
                            for(int j = 0; j < Materials.size(); j++) {
                                int balance = Integer.parseInt(col[j+2].trim());
                                System.out.printf(" %3d %s", balance, Materials.get(j).getName());
                                sup_rate.add(balance);
                            }
                            System.out.println();
                            Suppliers.add(new SupplierThread(col[1].trim(), Materials, sup_rate));
                            sup_parties++;
                        } else if(type.equals("F")) {
                            fac_name = col[1].trim();
                            product_name = col[2].trim();
                            f_lot = Integer.parseInt(col[3].trim());
                            f_1 = Integer.parseInt(col[4].trim());
                            f_2 = Integer.parseInt(col[5].trim());
                        }
                    }
                }

                for(SupplierThread s: Suppliers) s.setBarrier(sup_parties);

//                for(int i = 1; i <= days; i++) {
                    System.out.printf("%-16s>>  %s \n", Thread.currentThread().getName(), "-".repeat(60));
//                    System.out.printf("%-16s>>  Day %d \n", Thread.currentThread().getName(), i);
                    for(SupplierThread s: Suppliers) {
                        s.start();
                    }
//                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}

class Material extends Semaphore {
    int balance;
    private String name;

    public Material (String matName) {
        super(1, true);
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
  
    private ArrayList<Material> ML;
    private ArrayList<Integer> sup_rate;
    public SupplierThread(String name,  ArrayList<Material> M, ArrayList<Integer> sr) {
        super(name);
        ML = M;
        sup_rate = sr;
//        this.mainLatch = mainLatch;
//        barrier = ba;
//
//     public SupplierThread(String name, CountDownLatch mainLatch, CyclicBarrier barrier) {
//        super(name);
//        this.mainLatch = mainLatch;
//        this.barrier = barrier;
    }

    public void setBarrier(int parties) {
        barrier = new CyclicBarrier(parties);
    }

    public void run() {
        for(int i = 0; i < ML.size(); i++) {
            ML.get(i).addBalance(sup_rate.get(i));
            System.out.printf("%-16s>>  %-8s %3d %-10s balance = %3d %s\n", Thread.currentThread().getName(), "Put", sup_rate.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
        }
//        try {
//            barrier.await(); // signal for start factory thread
//        } catch (InterruptedException | BrokenBarrierException e) {
////        } catch (Exception e) {
//            throw new RuntimeException(e);
////            System.out.println(e);
//        }
    }
}

class FactoryThread extends Thread {

}
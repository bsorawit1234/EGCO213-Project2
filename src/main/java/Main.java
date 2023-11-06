import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

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
                        } else if(type.equals("M")) {
                            m1 = col[1].trim();
                            m2 = col[2].trim();
                        }
                    } else {
                        if(type.equals("S")) {
                            sup_name = col[1].trim();
                            s1 = Integer.parseInt(col[2].trim());
                            s2 = Integer.parseInt(col[3].trim());
                        } else if(type.equals("F")) {
                            fac_name = col[1].trim();
                            product_name = col[2].trim();
                            f_lot = Integer.parseInt(col[3].trim());
                            f_1 = Integer.parseInt(col[4].trim());
                            f_2 = Integer.parseInt(col[5].trim());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}

class SupplierThread extends Thread {
    private CountDownLatch mainLatch;
    private CyclicBarrier barrier;
    private ArrayList<Integer> materialRate;
    private ArrayList<Material> material; //each thread access same object

    public SupplierThread(String name, CountDownLatch mainLatch, CyclicBarrier barrier) {
        super(name);
        this.mainLatch = mainLatch;
        this.barrier = barrier;
    }

    public void addMaterial(Material mat) {
        //in main method create material from input file and then push it to ArrayList of each SupplierThread
        material.add(mat);
    }

    public void addRate(int rt) {
        materialRate.add(rt);
    }

    public void run() {
        try {
            mainLatch.await(); // wait for main thread print day
            for(int i = 0; i < material.size(); i++) {
                material.get(i).addBalance(materialRate.get(i));
                System.out.printf("%s >> Put %d %s balance = %d %s", Thread.currentThread().getName(), materialRate.get(i), material.get(i).getName(), material.get(i).getBalance(), material.get(i).getName());
            }
            barrier.await(); // signal for start factory thread
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}

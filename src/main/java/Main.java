import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        String path = "src/main/java/";
        String filename = "config1.txt";

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
                ArrayList<SupplierThread> Suppliers_copy = new ArrayList<SupplierThread>();
                ArrayList<FactoryThread> Factories = new ArrayList<FactoryThread>();
                ArrayList<FactoryThread> Factories_copy = new ArrayList<FactoryThread>();

                int days = 0;
                int sup_parties = 0;
                int fac_parties = 0;

                for(int i = 0; scanner.hasNext(); i++) {
                    line = scanner.nextLine();
                    col = line.split(",");
                    String type = col[0].trim();

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
                            System.out.printf("%-16s>>  %-12s daily supply rates =", Thread.currentThread().getName(), col[1].trim());
                            for(int j = 0; j < Materials.size(); j++) {
                                int balance = Integer.parseInt(col[j+2].trim());
                                System.out.printf(" %3d %-10s", balance, Materials.get(j).getName());
                                sup_rate.add(balance);
                            }
                            System.out.println();
                            Suppliers.add(new SupplierThread(col[1].trim(), Materials, sup_rate));
                            sup_parties++;
                        } else if(type.equals("F")) {
                            ArrayList<Integer> fac_rate = new ArrayList<Integer>();
                            int produce = Integer.parseInt(col[3].trim());
                            String product = col[2].trim();

                            System.out.printf("%-16s>>  %-12s daily use    rates =", Thread.currentThread().getName(), col[1].trim());
                            for(int j = 0; j < Materials.size(); j++) {
                                int rate = Integer.parseInt(col[j+4].trim()) * produce;
                                fac_rate.add(rate);
                                System.out.printf(" %3d %-10s", rate, Materials.get(j).getName());
                            }
                            System.out.printf("producing  %3d %s\n", produce, product);

                            Factories.add(new FactoryThread(col[1].trim(), Materials, produce, fac_rate, product));
                            fac_parties++;
                        }
                    }
                }

                    for(int i = 1; i <= days; i++) {
                        CyclicBarrier fac_barrier1 = new CyclicBarrier(fac_parties);
                        CyclicBarrier fac_barrier2 = new CyclicBarrier(fac_parties);
                        CyclicBarrier day_barrier = new CyclicBarrier(fac_parties + 1);
                        if(i > 1) {
                                Suppliers_copy = new ArrayList<>();
                                Factories_copy = new ArrayList<>();

                                for (SupplierThread s : Suppliers) {
                                    Suppliers_copy.add(new SupplierThread(s.getName(), Materials, s.getSup_rate()));
                                }
                                for (FactoryThread f : Factories) {
                                    Factories_copy.add(new FactoryThread(f.getName(), Materials, f.getF_lot(), f.getFac_rate(), f.getProduct_name()));
                                }

                                Suppliers = new ArrayList<>();
                                Factories = new ArrayList<>();

                                Suppliers.addAll(Suppliers_copy);
                                Factories.addAll(Factories_copy);
                            }

                            for(FactoryThread f: Factories) f.setBarrier(fac_barrier1, fac_barrier2, day_barrier);

                            System.out.printf("\n%-16s>>  .. \n", Thread.currentThread().getName()/*, "-".repeat(60)*/);
                            System.out.printf("%-16s>>  Day %d \n", Thread.currentThread().getName(), i);
                            for(SupplierThread s: Suppliers) {
                                s.start();
                                s.join();
                            }
                            for(FactoryThread f: Factories) f.start();


                            day_barrier.await();
                            if(day_barrier.getParties() == 0) {
                                day_barrier.reset();
                            }

                        }
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

    public void addBalance(int b) {
        balance += b;
    }

    public void useBalance(int b) {
        balance -= b;
    }
}

class SupplierThread extends Thread {
    private CyclicBarrier barrier;
    private ArrayList<Material> ML;
    private ArrayList<Integer> sup_rate;
    public SupplierThread(String name,  ArrayList<Material> M, ArrayList<Integer> sr) {
        super(name);
        ML = M;
        sup_rate = sr;
    }

    public ArrayList<Integer> getSup_rate() {
        return sup_rate;
    }

    public void setBarrier(CyclicBarrier ba) {
        barrier = ba;
    }

    public void run() {
        synchronized (this) {
            for(int i = 0; i < ML.size(); i++) {
                ML.get(i).addBalance(sup_rate.get(i));
                System.out.printf("%-16s>>  %-8s %3d %-10s balance = %3d %s\n", Thread.currentThread().getName(), "Put", sup_rate.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
            }
        }
    }
}

class FactoryThread extends Thread {
    private ArrayList<Material> ML;
    private ArrayList<Integer> f_balance = new ArrayList<Integer>();
    private ArrayList<Integer> fac_rate;
    private CyclicBarrier barrier1, barrier2, day_barrier;
    private int f_lot;
    private Exchanger<Material>  exchanger;
    private String product_name;
    private int lot = 0;

    public ArrayList<Integer> getFac_rate() {
        return fac_rate;
    }
    public int getF_lot() {
        return f_lot;
    }

    public ArrayList<Integer> getF_balance() {
        return f_balance;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setExchanger(Exchanger<Material> ex)     { exchanger = ex; }

    public FactoryThread(String name, ArrayList<Material> M, int lot, ArrayList<Integer> fac, String pn) {
        super(name);
        ML = M;
        f_lot = lot;
        fac_rate = fac;
        product_name = pn;

        for( int i = 0; i < ML.size(); i++ ) {
            f_balance.add(0);
        }
    }

    public void setBarrier(CyclicBarrier ba1, CyclicBarrier ba2, CyclicBarrier db) {
        barrier1 = ba1;
        barrier2 = ba2;
        day_barrier = db;

    }

    @Override
    public void run() {
        synchronized (this) {
            System.out.printf("%-16s>>  Holding", Thread.currentThread().getName());
            for (int i = 0; i < ML.size(); i++) {
                System.out.printf("%5d %s ", f_balance.get(i), ML.get(i).getName());
            }
            System.out.println();
            int delay = (int) (Math.random() * 123);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }

            try {
                barrier1.await();
                if(barrier1.getParties() == 0) barrier1.reset();
            } catch (Exception e) {
            }

//        for( int i = 0; i < ML.size(); i++ ) {
//            if( f_lot * f_1 < ML.get(0).getBalance() || f_lot * f_2 < ML.get(1).getBalance()) {
//                f_balance.add(0, f_lot * f_1);
//                System.out.printf("%-16s>>  Get %5s %s             balance = %4d %s\n", Thread.currentThread().getName(), f_balance.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
//            }
//        }


        for (int i = 0; i < ML.size(); i++) {
            int delay = (int) (Math.random() * 123);
            try { Thread.sleep(delay); } catch (InterruptedException e) { }

            if(ML.get(i).getBalance() >= fac_rate.get(i)){
                ML.get(i).useBalance(fac_rate.get(i));
                f_balance.set(i, fac_rate.get(i));
            } else {
                ML.get(i).useBalance(ML.get(i).getBalance());
                f_balance.set(i, ML.get(i).getBalance());
            }

            System.out.printf("%-16s>>  Get          %3d %s balance = %3d %s", Thread.currentThread().getName(), f_balance.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
            System.out.println();
        }

        try { barrier2.await(); if(barrier2.getParties() == 0) barrier2.reset(); } catch (Exception e) { }

        boolean check = true;
        for(int i = 0; i < ML.size(); i++) {
            if (!f_balance.get(i).equals(fac_rate.get(i))) {
                check = false;
                break;
            }
        }

        if(check) {
            for(int i = 0; i < f_balance.size(); i++) f_balance.set(i, 0);
            lot++;

            System.out.printf("%-16s>>  %-10s production succeed, lot %3d", Thread.currentThread().getName(), product_name, lot);
            System.out.println();
        } else {
            System.out.printf("%-16s>>  %-10s production fails\n", Thread.currentThread().getName(), product_name);

            int remain = 0;
            for(int fb: f_balance) if(fb == 0) remain++;

            if(remain < f_balance.size()){
                for (int i = 0; i < ML.size(); i++) {
                    ML.get(i).addBalance(f_balance.get(i));
                    if (f_balance.get(i) > 0) System.out.printf("%-16s>>  Put  %3d %s   balance = %3d %s\n", Thread.currentThread().getName(), f_balance.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
                    f_balance.set(i, 0);
                }

            }
        }
        try {
            day_barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

    }
}
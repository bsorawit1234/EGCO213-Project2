import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        String path = "src/main/java/";
        String filename = "config.txt";

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
                        } else if(type.equals("F")) {
                            ArrayList<Integer> fac_rate = new ArrayList<Integer>();
                            ArrayList<Integer> f_balance = new ArrayList<>();
                            String product = col[2].trim();
                            int produce = Integer.parseInt(col[3].trim());
                            int lot = 0; // not produce yet

                            System.out.printf("%-16s>>  %-12s daily use    rates =", Thread.currentThread().getName(), col[1].trim());
                            for(int j = 0; j < Materials.size(); j++) {
                                int rate = Integer.parseInt(col[j+4].trim()) * produce;
                                fac_rate.add(rate);
                                f_balance.add(0);
                                System.out.printf(" %3d %-10s", rate, Materials.get(j).getName());
                            }
                            System.out.printf("producing  %3d %s\n", produce, product);

                            Factories.add(new FactoryThread(col[1].trim(), Materials, produce, fac_rate, product, f_balance, lot));
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
                            Factories_copy.add(new FactoryThread(f.getName(), Materials, f.getF_lot(), f.getFac_rate(), f.getProduct_name(), f.getF_balance(), f.getLot()));
                        }

                        Suppliers = new ArrayList<>(Suppliers_copy);
                        Factories = new ArrayList<>(Factories_copy);
                    }

                    for(FactoryThread f: Factories) f.setBarrier(fac_barrier1, fac_barrier2, day_barrier);

                    System.out.printf("\n%-16s>>  %s \n", Thread.currentThread().getName(), "-".repeat(60));
                    System.out.printf("%-16s>>  Day %d \n", Thread.currentThread().getName(), i);
                    for(SupplierThread s: Suppliers) {
                        s.start();
                        s.join();
                    }
                    for(FactoryThread f: Factories) f.start();

                    day_barrier.await();
                }

                System.out.printf("\n%-16s>>  %s \n", Thread.currentThread().getName(), "-".repeat(60));
                System.out.printf("%-16s>>  Summary \n", Thread.currentThread().getName());

                Collections.sort(Factories);

                for(FactoryThread f: Factories) {
                    System.out.printf("%-16s>>  Total %-10s =  %3d lots \n", Thread.currentThread().getName(), f.getProduct_name(), f.getLot());
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
    public void addBalance(int b) { balance += b; }
    public void useBalance(int b) {
        balance -= b;
    }
}

class SupplierThread extends Thread {
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

    public void run() {
        synchronized (this) {
            for(int i = 0; i < ML.size(); i++) {
                ML.get(i).addBalance(sup_rate.get(i));
                System.out.printf("%-16s>>  %-8s %3d %-10s balance = %3d %s\n", Thread.currentThread().getName(), "Put", sup_rate.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
            }
        }
    }
}

class FactoryThread extends Thread implements Comparable<FactoryThread> {
    private ArrayList<Material> ML;
    private ArrayList<Integer> f_balance = new ArrayList<Integer>();
    private ArrayList<Integer> fac_rate;
    private CyclicBarrier barrier1, barrier2, day_barrier;
    private int f_lot, lot;
    private String product_name;

    public ArrayList<Integer> getFac_rate() {
        return fac_rate;
    }
    public int getF_lot() {
        return f_lot;
    }

    public ArrayList<Integer> getF_balance() {
        return f_balance;
    }

    public String getProduct_name() { return product_name;}
    public int getLot() { return lot; }

    public FactoryThread(String name, ArrayList<Material> M, int fl, ArrayList<Integer> fr, String pn, ArrayList<Integer> fb, int l) {
        super(name);
        ML = M;
        f_lot = fl;
        fac_rate = fr;
        product_name = pn;
        f_balance = fb;
        lot = l;
    }

    public void setBarrier(CyclicBarrier ba1, CyclicBarrier ba2, CyclicBarrier db) {
        barrier1 = ba1;
        barrier2 = ba2;
        day_barrier = db;

    }

    public int compareTo(FactoryThread other) {
        if(lot > other.lot) {
            return -1;
        } else {
            if(product_name.compareToIgnoreCase(other.product_name) > 0) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            int delay = (int) (Math.random() * 123);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }

            System.out.printf("%-16s>>  Holding", Thread.currentThread().getName());
            for (int i = 0; i < ML.size(); i++) {
                System.out.printf("%5d %s ", f_balance.get(i), ML.get(i).getName());
            }
            System.out.println();
        }

        try {
            barrier1.await();
        } catch (Exception e) { }

        for (int i = 0; i < ML.size(); i++) {
            int delay = (int) (Math.random() * 123);
            try { Thread.sleep(delay); } catch (InterruptedException e) { }

            if(ML.get(i).getBalance() >= fac_rate.get(i)){
                ML.get(i).useBalance(fac_rate.get(i));
                f_balance.set(i, fac_rate.get(i));
            } else {
                int balance = ML.get(i).getBalance();
                ML.get(i).useBalance(balance);
                f_balance.set(i, balance);
            }
            System.out.printf("%-16s>>  %-8s %3d %-10s balance = %3d %s", Thread.currentThread().getName(), "Get", f_balance.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
            System.out.println();
        }

        try { barrier2.await(); } catch (Exception e) { }

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

            for(int i = 0; i < f_balance.size(); i++) {
               if(f_balance.get(i) < fac_rate.get(i) && f_balance.get(i) > 0) {
                   ML.get(i).addBalance(f_balance.get(i));
                   System.out.printf("%-16s>>  Put  %3d %s   balance = %3d %s\n", Thread.currentThread().getName(), f_balance.get(i), ML.get(i).getName(), ML.get(i).getBalance(), ML.get(i).getName());
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
import java.io.*;
import java.util.*;
import java.lang.*;

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

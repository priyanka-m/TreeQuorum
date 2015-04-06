import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by priyanka on 4/2/15.
 */
class M {
    int i;
    int j;
 }

class Comp implements Comparator<M> {
  public int compare(M m1, M m2) {
    if(m1.i > m2.i){
      return 1;
    } else if (m1.i == m2.i) {
      if (m1.j > m2.j) {
        return 1;
      } else {
        return -1;
      }
    } else {
      return -1;
    }
  }
}
public class TestClass {

  public static void main(String[] args) {
    M m1 = new M();
    m1.i = 5;
    m1.j = 11;
    M m2 = new M();
    m2.i = 5;
    m2.j = 2;
    M m3 = new M();
    m3.i = 1;
    m3.j = 1;
    ArrayList<M> ar = new ArrayList<M>();
    ar.add(m1);
    ar.add(m2);
    ar.add(m3);
    Collections.sort(ar, new Comp());
    System.out.println("displaying sorted");
    for (int i = 0; i < ar.size(); i++) {
      System.out.println(ar.get(i).i + " " + ar.get(i).j);
    }
    int m = 7;
    int x = 4;
    if (x > (Math.ceil(m/2))) {
      System.out.println(true);
    }
  }
}

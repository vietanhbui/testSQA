import junit.framework.*;

public class TriangleTest extends TestCase {

  public static void main (String[] args) {
    junit.textui.TestRunner.run(TriangleTest.class);
  }

  @org.junit.jupiter.api.Test
  public void testCase1() {
    Triangle x1 = new Triangle();
    assertEquals("Is triangle scalene",String.valueOf(x1.checkTriangle(7, 6, 2)));
  }

  @org.junit.jupiter.api.Test
  public void testCase2() {
    Triangle x203 = new Triangle();
    assertEquals("Is triangle isosceles ",String.valueOf(x203.checkTriangle(4, 4, 3)));
  }

  @org.junit.jupiter.api.Test
  public void testCase3() {
    Triangle x597 = new Triangle();
    assertEquals("Is triangle equilateral ",String.valueOf(x597.checkTriangle(8, 8, 8)));
  }

  @org.junit.jupiter.api.Test
  public void testCase4() {
    Triangle x601 = new Triangle();
    assertEquals("Not a triangle",String.valueOf(x601.checkTriangle(0, 2, 0)));
  }

}

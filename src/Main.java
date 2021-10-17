import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Scanner;

public class Main {
    /**
     * The main meth of the program
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            calculateIntegrals();
            System.out.println("OPERATION SUCCEEDED!");
        } catch (Exception e) {
            System.out.println("OPERATION FAILED!");
        }
    }

    /**
     * The method takes as an argument a String that should contain 2 numbers separated by a comma
     * with the first number being necessarily less than a second number
     * and returns an array of 2 objects of class BigDecimal
     * @param line String object that has to contain those 2 numbers
     * @return BigDecimal array representing integration bounds
     */
    private static BigDecimal[] getBounds(String line) {
        final String numberRGX = "([+-]?(0|([1-9][0-9]*))(\\.[0-9]+)?)";
        final String commaRGX = "\\s*,\\s*";
        if (!line.matches("\\s*" + numberRGX + commaRGX + numberRGX + "\\s*")) {
            throw new IllegalArgumentException("Unable to find valid bounds within a String: " + line);
        }
        String[] tokens = line.split(commaRGX);
        BigDecimal[] bounds = new BigDecimal[]{new BigDecimal(tokens[0].trim()), new BigDecimal(tokens[1].trim())};
        if (bounds[0].compareTo(bounds[1]) >= 0) {
            throw new IllegalArgumentException("Lower bound should be less than upper bound");
        }
        return bounds;
    }

    /**
     * The method counts lines in the file "functions.txt"
     * @return number of lines in the file
     */
    private static int countLines() {
        File file = new File("files/functions.txt");
        int count = 0;
        try (Scanner input = new Scanner(file)) {
            while (input.hasNextLine()) {
                input.nextLine();
                count++;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
        return count;
    }

    /**
     * The method opens file "functions.txt" and takes values of integration bounds from the first line
     * and coefficients of polynomials from the following lines.
     * Then the method opens a file "integrals.txt", writes calculated integrals from polynomials
     * described together with integration bounds in the file "functions.txt".
     */
    private static void calculateIntegrals() {
        File inputFile = new File("files/functions.txt");
        File outputFile = new File("files/integrals.txt");
        BigDecimal[] bounds;
        Polynomial[] polynomials = new Polynomial[countLines() - 1];
        try (Scanner input = new Scanner(inputFile); PrintWriter output = new PrintWriter(outputFile)) {
            bounds = getBounds(input.nextLine());
            int i = 0;
            while (input.hasNextLine()) {
                polynomials[i++] = Polynomial.getPolynomialFromCSVLine(input.nextLine());
            }

            output.println("bounds: " + bounds[0].toPlainString() + ", " + bounds[1].toPlainString());
            for (Polynomial polynomial : polynomials) {
                output.print("integrated using rectangles: ");
                output.print(polynomial.integrateUsingRectangles(bounds[0], bounds[1]));
                output.print(", integrated using trapezoids: ");
                output.print(polynomial.integrateUsingTrapezoids(bounds[0], bounds[1]));
                output.println();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}

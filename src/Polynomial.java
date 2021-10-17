import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class Polynomial {
    private final BigDecimal[] coefficients;

    //The regexes used for parsing polynomial expressions
    private final static String INTEGER_RGX = "(0|([1-9][0-9]*))";
    private final static String SIGN_RGX = "[+-]";
    private final static String INNER_DECIMAL_RGX = "([+-](0|([1-9][0-9]*))(\\.[0-9]+)?)";
    private final static String OUTER_DECIMAL_RGX = "([+-]?(0|([1-9][0-9]*))(\\.[0-9]+)?)";
    private final static String COMMA_RGX = "\\s*,\\s*";
    private final static String COMMA_SEPARATED_NUMBERS_RGX = OUTER_DECIMAL_RGX + "(" + COMMA_RGX + OUTER_DECIMAL_RGX + ")*";
    private final static String X_RGX = "(\\*?x(\\^" + INTEGER_RGX + ")?)?";
    private final static String X_SPLITTING_RGX = "\\*?x\\^";
    private final static String OUTER_POLYNOMIAL_TERM_RGX = OUTER_DECIMAL_RGX + "?" + X_RGX;
    private final static String INNER_POLYNOMIAL_TERM_RGX = INNER_DECIMAL_RGX + "?" + X_RGX;
    private final static String POLYNOMIAL_RGX = OUTER_POLYNOMIAL_TERM_RGX + "(" + INNER_POLYNOMIAL_TERM_RGX + ")*";

    /**
     * The Polynomial constructor creating a polynomial which is a constant value of 0
     */
    public Polynomial() {
        this.coefficients = new BigDecimal[]{BigDecimal.ZERO};
    }

    /**
     * The Polynomial constructor creating a polynomial from an array of coefficients
     * @param coefficients array of coefficients starting from x^0, x^1 ... and so on
     */
    public Polynomial(BigDecimal[] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            throw new IllegalArgumentException("Array of BigDecimals has to be non-null and contain at least 1 element");
        }
        this.coefficients = new BigDecimal[coefficients.length];
        System.arraycopy(coefficients, 0, this.coefficients, 0, coefficients.length);
    }

    /**
     * The method parses an array of coefficients from a CSV line
     * @param terms String that should contain comma separated numbers representing polynomial coefficients
     * @return object of class Polynomial
     */
    public static Polynomial getPolynomialFromCSVLine(String terms) {
        if (terms == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        if (!terms.matches(COMMA_SEPARATED_NUMBERS_RGX)) {
            throw new IllegalArgumentException("String " + terms + " does not represent the line of CSNumbers");
        }
        String[] coefficientStrings = terms.split(COMMA_RGX);
        BigDecimal[] coefficients = new BigDecimal[coefficientStrings.length];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = new BigDecimal(coefficientStrings[i]);
        }
        return new Polynomial(coefficients);
    }

    /**
     * The method parses a polynomial from polynomial expressions like: 5x^3-8, 3x^2-x and so on 
     * @param terms the String that should contain a polynomial expression
     * @return object of class Polynomial
     */
    public static Polynomial getPolynomialFromExpression(String terms) {
        if (terms == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        if (!terms.matches(POLYNOMIAL_RGX)) {
            throw new IllegalArgumentException("String " + terms + " does not represent the polynomial expression");
        }
        BigDecimal[] coefficients;
        if (terms.matches(OUTER_POLYNOMIAL_TERM_RGX)) {
            if (terms.matches(OUTER_DECIMAL_RGX)) {
                coefficients = new BigDecimal[]{new BigDecimal(terms)};
                return new Polynomial(coefficients);
            }
            if (terms.matches("x\\^" + INTEGER_RGX)) {
                int degree = Integer.parseInt(terms.substring(2));
                coefficients = new BigDecimal[degree + 1];
                coefficients[degree] = new BigDecimal("1");
                return new Polynomial(coefficients);
            }
            if (terms.matches("x")) {
                coefficients = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ONE};
                return new Polynomial(coefficients);
            }
            String[] tokens = terms.split(X_SPLITTING_RGX);
            int degree = Integer.parseInt(tokens[1]);
            coefficients = new BigDecimal[degree + 1];
            coefficients[degree] = new BigDecimal(tokens[0]);
            return new Polynomial(coefficients);
        }
        String[] tokens = terms.split("(?=" + SIGN_RGX + ")");
        Polynomial result = new Polynomial();
        for (String token : tokens) {
            result = result.add(getPolynomialFromExpression(token));
        }
        return result;
    }

    /**
     * The method returns degree of this polynomial
     * @return degree of this polynomial
     */
    public int getDegree() {
        return coefficients.length - 1;
    }

    /**
     * The method returns coefficient of the power of x given as an argument nr
     * @param nr power of x
     * @return coefficient at power of x equal to nr
     */
    public BigDecimal getCoefficient(int nr) {
        if (nr < 0 || nr > getDegree()) {
            throw new IllegalArgumentException("Cannot access coefficient of degree " + nr);
        }
        return this.coefficients[nr];
    }

    /**
     * The method adds a polynomial to this polynomial and returns their sum
     * @param expression added polynomial
     * @return sum of this polynomial and the one given as a method argument
     */
    public Polynomial add(Polynomial expression) {
        int newLength = Math.max(this.coefficients.length, expression.coefficients.length);
        BigDecimal[] coefficients = new BigDecimal[newLength];
        BigDecimal temp = BigDecimal.ZERO;
        for (int i = 0; i < coefficients.length; i++, temp = BigDecimal.ZERO) {
            if (i < this.coefficients.length) {
                temp = temp.add(this.getCoefficient(i));
            }
            if (i < expression.coefficients.length) {
                temp = temp.add(expression.getCoefficient(i));
            }
            coefficients[i] = temp;
        }
        return new Polynomial(coefficients);
    }

    /**
     * The method returns value of this Polynomial for a given value that is substituted for x
     * @param value value substituted for x
     * @return the value of the polynomial
     */
    public BigDecimal evaluateFor(BigDecimal value) {
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < coefficients.length; i++) {
            result = result.add(coefficients[i].multiply(value.pow(i)));
        }
        return result;
    }

    /**
     * The method returns integral for this polynomial for given bounds using trapezoids
     * @param lowerBound lower bound of integration
     * @param upperBound upper bound of integration
     * @return the value of integral within given bounds
     */
    public BigDecimal integrateUsingTrapezoids(BigDecimal lowerBound, BigDecimal upperBound) {
        final BigDecimal divisions = new BigDecimal("1000");
        final BigDecimal two = new BigDecimal("2");
        BigDecimal deltaX = upperBound
                .subtract(lowerBound)
                .divide(divisions, 6, RoundingMode.HALF_UP);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal i = lowerBound; i.compareTo(upperBound) < 0; i = i.add(deltaX)) {
            sum = sum
                    .add(
                            evaluateFor(i)
                                    .add(
                                            evaluateFor(i
                                                    .add(deltaX))
                                    )
                                    .multiply(deltaX)
                                    .divide(two, 6, RoundingMode.HALF_UP)
                    );
        }
        return sum;
    }

    /**
     * The method returns integral for this polynomial for given bounds using rectangles
     * @param lowerBound lower bound of integration
     * @param upperBound upper bound of integration
     * @return the value of integral within given bounds
     */
    public BigDecimal integrateUsingRectangles(BigDecimal lowerBound, BigDecimal upperBound) {
        final BigDecimal divisions = new BigDecimal("1000");
        BigDecimal deltaX = upperBound.subtract(lowerBound).divide(divisions, 6, RoundingMode.HALF_UP);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal i = lowerBound; i.compareTo(upperBound) < 0; i = i.add(deltaX)) {
            sum = sum
                    .add(
                            evaluateFor(i)
                                    .multiply(deltaX)
                    );
        }
        return sum;
    }

    /**
     * The method returns this polynomial as a String
     * @return String representation of this Polynomial
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String coefficientString;
        String exponentString;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            if (i != coefficients.length - 1 && coefficients[i].signum() == 1) {
                builder.append('+');
            }
            if (coefficients[i].compareTo(BigDecimal.ZERO) == 0 || coefficients[i].compareTo(BigDecimal.ONE) == 0) {
                coefficientString = "";
            } else {
                coefficientString = coefficients[i].toPlainString();
            }
            if (i == 0) {
                exponentString = "";
            } else if (i == 1) {
                exponentString = "x";
            } else {
                exponentString = "x^" + i;
            }
            builder.append(coefficientString).append(exponentString);
        }
        return builder.toString();
    }

    /**
     * The method checks 2 objects for equality
     * @param o object tested for equality
     * @return true if both object are of class Polynomial and have the same coefficients
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polynomial that = (Polynomial) o;
        return Arrays.equals(coefficients, that.coefficients);
    }

    /**
     * The method returns hashcode of this object
     * @return hashcode of this object
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coefficients);
    }
}

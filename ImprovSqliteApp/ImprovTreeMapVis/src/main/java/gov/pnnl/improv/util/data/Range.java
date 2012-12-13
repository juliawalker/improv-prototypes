package gov.pnnl.improv.util.data;

/**
 * Class to represent a data range.
 *
 * @author Grant Nakamura, April 2009
 */
public class Range
{

    private double min, max;

    public Range()
    {
        this(Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    public Range(double min, double max)
    {
        this.min = min;
        this.max = max;
    }

    /**
     * Adds a value to the range, updating the min and max as needed.
     */
    public void add(double value)
    {
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public double getMin()
    {
        return min;
    }

    public double getMax()
    {
        return max;
    }

    public String toString()
    {
        return min + " to " + max;
    }

}

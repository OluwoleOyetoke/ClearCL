package clearcl.util;

/**
 * Utility class to measure execution time of a block of code (closure-based)
 *
 * @author royer
 */
public class ElapsedTime
{
  public static boolean sStandardOutput = false;

  /**
   * Measures the elapsed time of a Runnable.
   * 
   * @param pDescription
   * @param pRunnable
   */
  public static void measure(String pDescription, Runnable pRunnable)
  {
    measure(true, pDescription, pRunnable);
  }

  /**
   * Measures the elapsed time of a Runnable. An optional boolean flag can be
   * used to switch of the timing.
   * 
   * @param pActive true -> measure, false -> execute without measuring
   * @param pDescription description of the code (runnable)
   * @param pRunnable runnable
   */
  public static void measure(boolean pActive,
                             String pDescription,
                             Runnable pRunnable)
  {
    if (!pActive)
      pRunnable.run();

    long lNanosStart = System.nanoTime();
    pRunnable.run();
    long lNanosStop = System.nanoTime();

    long lElapsedNanos = lNanosStop - lNanosStart;
    double lElapsedTimeInMilliseconds = lElapsedNanos * 1e-6;

    if (sStandardOutput)
      System.out.format("%g ms for %s \n",
                        lElapsedTimeInMilliseconds,
                        pDescription);

  }
}
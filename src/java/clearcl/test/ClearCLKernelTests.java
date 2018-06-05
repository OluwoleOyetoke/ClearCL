package clearcl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import clearcl.backend.jocl.ClearCLBackendJOCL;
import clearcl.enums.BuildStatus;
import clearcl.enums.HostAccessType;
import clearcl.enums.KernelAccessType;
import clearcl.exceptions.ClearCLArgumentMissingException;
import clearcl.selector.BadDeviceSelector;
import clearcl.selector.DeviceTypeSelector;
import clearcl.selector.GlobalMemorySelector;
import coremem.enums.NativeTypeEnum;

import org.junit.Test;

/**
 * Basic Kernel tests
 *
 * @author royer
 */
public class ClearCLKernelTests
{

  private static final int cFloatArrayLength = 1024 * 1024;

  /**
   * Test with JOCL backend
   * 
   * @throws Exception
   *           NA
   */
  @Test
  public void testBackendJOCL() throws Exception
  {
    ClearCLBackendJOCL lClearCLJOCLBackend = new ClearCLBackendJOCL();

    testWithBackend(lClearCLJOCLBackend);

  }

  /**
   * Test with JavaCL backend
   * 
   * @throws Exception
   *           NA
   */
  @Test
  public void testBackendJavaCL() throws Exception
  {
    ClearCLBackendJavaCL lClearCLBackendJavaCL =
                                               new ClearCLBackendJavaCL();

    testWithBackend(lClearCLBackendJavaCL);

  }

  private void testWithBackend(ClearCLBackendInterface pClearCLBackendInterface) throws Exception
  {
    try (ClearCL lClearCL = new ClearCL(pClearCLBackendInterface))
    {
      ClearCLDevice lClearClDevice =
                                   lClearCL.getBestDevice(DeviceTypeSelector.GPU,
                                                          BadDeviceSelector.NotIntegratedIntel,
                                                          GlobalMemorySelector.MAX);

      // System.out.println(lClearClDevice.getInfoString());

      ClearCLContext lContext = lClearClDevice.createContext();

      ClearCLProgram lProgram =
                              lContext.createProgram(this.getClass(),
                                                     "test.cl");
      lProgram.addDefine("CONSTANT", "10");
      lProgram.addBuildOptionAllMathOpt();

      BuildStatus lBuildStatus = lProgram.buildAndLog();

      assertEquals(lBuildStatus, BuildStatus.Success);
      // assertTrue(lProgram.getBuildLog().isEmpty());

      ClearCLBuffer lBufferA =
                             lContext.createBuffer(HostAccessType.WriteOnly,
                                                   KernelAccessType.ReadOnly,
                                                   NativeTypeEnum.Float,
                                                   cFloatArrayLength);

      ClearCLBuffer lBufferB =
                             lContext.createBuffer(HostAccessType.WriteOnly,
                                                   KernelAccessType.ReadOnly,
                                                   NativeTypeEnum.Float,
                                                   cFloatArrayLength);

      ClearCLBuffer lBufferC =
                             lContext.createBuffer(HostAccessType.ReadOnly,
                                                   KernelAccessType.WriteOnly,
                                                   NativeTypeEnum.Float,
                                                   cFloatArrayLength);

      ClearCLKernel lKernel = lProgram.createKernel("buffersum");
      lKernel.setGlobalSizes(cFloatArrayLength);

      // System.out.println(lKernel.getSourceCode());

      // checking if include is, well , included:
      assertTrue(lKernel.getSourceCode()
                        .contains("inline float4 matrix_mult"));

      // checking if define is, well , defined:
      assertTrue(lKernel.getSourceCode().contains("CONSTANT"));

      // checking if define is, well , defined:
      assertTrue(lKernel.getSourceCode()
                        .contains("WARNING!! Could not resolve include"));

      // different ways to set arguments:
      lKernel.setArguments(11f, lBufferA, lBufferB, lBufferC);
      lKernel.run();

      lKernel.clearArguments();
      lKernel.setArgument(0, 11f);
      lKernel.setArgument(1, lBufferA);
      lKernel.setArgument(2, lBufferB);
      lKernel.setArgument(3, lBufferC);
      lKernel.run();

      //
      lKernel.clearArguments();
      lKernel.setArgument("p", 11f);
      lKernel.setArgument("a", lBufferA);
      lKernel.setArgument("b", lBufferB);
      lKernel.setArgument("c", lBufferC);
      lKernel.run();

      // what if a argument is missing but there is a default value defined?
      try
      {
        lKernel.clearArguments();
        lKernel.setArgument("a", lBufferA);
        lKernel.setArgument("b", lBufferB);
        lKernel.setArgument("c", lBufferB);
        lKernel.run();
        assertTrue(true);
      }
      catch (ClearCLArgumentMissingException e)
      {
        fail();
      }

      boolean lFailed = false;

      // what if a argument is missing?
      try
      {
        lKernel.clearArguments();
        lKernel.setArgument("p", 11f);
        lKernel.setArgument("a", lBufferA);
        lKernel.setArgument("b", lBufferB);
        lKernel.run();
        lFailed = true;
      }
      catch (RuntimeException e)
      {
        System.out.println("Caught as expected: " + e);
      }

      // what if an unknown argument is added?
      try
      {
        lKernel.clearArguments();
        lKernel.setArgument("p", 11f);
        lKernel.setArgument("a", lBufferA);
        lKernel.setArgument("b", lBufferB);
        lKernel.setArgument("c", lBufferC);
        lKernel.setArgument("z", 1.3f);
        lKernel.run();
        lFailed = true;
      }
      catch (RuntimeException e)
      {
        System.out.println("Caught as expected: " + e);
      }

      assertTrue(!lFailed);

    }
  }

}

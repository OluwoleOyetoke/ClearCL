package clearcl.viewer.demo;

import java.io.IOException;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.ImageChannelDataType;
import clearcl.ocllib.OCLlib;
import clearcl.test.ClearCLBasicTests;
import clearcl.viewer.ClearCLImageViewer;
import coremem.offheap.OffHeapMemory;

import org.junit.Test;

/**
 * View image demos
 *
 * @author royer
 */
public class ViewImageDemos
{

  /**
   * Demos 2D image viewing.
   * 
   * @throws InterruptedException
   *           NA
   * @throws IOException
   *           NA
   */
  @Test
  public void demoViewImage2DF() throws InterruptedException,
                                 IOException
  {

    ClearCLBackendInterface lClearCLBackendInterface =
                                                     ClearCLBackends.getBestBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
    {
      ClearCLDevice lFastestGPUDevice =
                                      lClearCL.getFastestGPUDeviceForImages();

      System.out.println(lFastestGPUDevice);

      ClearCLContext lContext = lFastestGPUDevice.createContext();

      ClearCLProgram lProgram =
                              lContext.createProgram(ClearCLBasicTests.class,
                                                     "test.cl");
      lProgram.addDefine("CONSTANT", "1");
      lProgram.build();

      ClearCLImage lImage =
                          lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                            512,
                                                            512,512);

      ClearCLKernel lKernel = lProgram.createKernel("fillimagexor");
      lKernel.setArgument("image", lImage);
      lKernel.setGlobalSizes(lImage);
      lKernel.run(true);

      ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);

      for (int i = 1000; i > 1 && lViewImage.isShowing(); i--)
      {

        if (i % 1000 == 0)
          System.out.println("i=" + i);
        lKernel.setArgument("u", 100.0f / i);
        lKernel.setArgument("dx", i);
        lKernel.setArgument("dy", i);

        lKernel.run(true);
        lImage.notifyListenersOfChange(lContext.getDefaultQueue());
        Thread.sleep(10);
      }

      lViewImage.waitWhileShowing();

    }

  }

  /**
   * Demos 2D image viewing.
   * 
   * @throws InterruptedException
   *           NA
   * @throws IOException
   *           NA
   */
  @Test
  public void demoViewImage2DUI() throws InterruptedException,
                                  IOException
  {

    ClearCLBackendInterface lClearCLBackendInterface =
                                                     ClearCLBackends.getBestBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
    {
      ClearCLDevice lFastestGPUDevice =
                                      lClearCL.getFastestGPUDeviceForImages();

      System.out.println(lFastestGPUDevice);

      ClearCLContext lContext = lFastestGPUDevice.createContext();

      int lSize = 213;

      ClearCLImage lImage =
                          lContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16,
                                                            lSize,
                                                            lSize);

      ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);

      OffHeapMemory lBuffer = OffHeapMemory.allocateShorts(lSize
                                                           * lSize);

      for (int i = 0; i < 10000 && lViewImage.isShowing(); i++)
      {

        for (int y = 0; y < lSize; y++)
          for (int x = 0; x < lSize; x++)
            lBuffer.setShortAligned(x + lSize * y,
                                    (short) ((x + i) ^ y));

        lImage.readFrom(lBuffer, true);

        lImage.notifyListenersOfChange(lContext.getDefaultQueue());

        Thread.sleep(10);
      }

      lViewImage.waitWhileShowing();
    }

  }

  /**
   * Demos 3D image viewing.
   * 
   * @throws InterruptedException
   *           NA
   * @throws IOException
   *           NA
   */
  @Test
  public void demoViewImage3DF() throws InterruptedException,
                                 IOException
  {

    ClearCLBackendInterface lClearCLBackendInterface =
                                                     ClearCLBackends.getBestBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
    {
      ClearCLDevice lFastestGPUDevice =
                                      lClearCL.getFastestGPUDeviceForImages();

      System.out.println(lFastestGPUDevice);

      ClearCLContext lContext = lFastestGPUDevice.createContext();

      ClearCLProgram lProgram =
                              lContext.createProgram(OCLlib.class,
                                                     "phantoms/phantoms.cl");
      lProgram.buildAndLog();

      int lSize = 213;

      ClearCLImage lImage =
                          lContext.createSingleChannelImage(ImageChannelDataType.Float,
                                                            lSize,
                                                            lSize,
                                                            lSize);

      ClearCLKernel lKernel = lProgram.createKernel("sphere");
      lKernel.setArgument("image", lImage);
      lKernel.setGlobalSizes(lImage);

      lKernel.setOptionalArgument("r", 0.25f);
      lKernel.setOptionalArgument("cx", lSize / 2);
      lKernel.setOptionalArgument("cy", lSize / 2);
      lKernel.setOptionalArgument("cz", lSize / 2);

      lKernel.setOptionalArgument("a", 1);
      lKernel.setOptionalArgument("b", 1);
      lKernel.setOptionalArgument("c", 1);
      lKernel.setOptionalArgument("d", 1);

      lKernel.run(true);
      lImage.notifyListenersOfChange(lContext.getDefaultQueue());

      ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);

      for (int i = 0; i < 10000 && lViewImage.isShowing(); i++)
      {
        int x = ((64 + (i)) % lSize);
        int y = ((64 + (int) (i * 1.2)) % lSize);
        int z = ((64 + (int) (i * 1.3)) % lSize);

        // System.out.format("x=%d, y=%d, z=%d \n",x,y,z);

        if (i % 1000 == 0)
          System.out.println("i=" + i);
        lKernel.setOptionalArgument("r", 0.25f);
        lKernel.setOptionalArgument("cx", x);
        lKernel.setOptionalArgument("cy", y);
        lKernel.setOptionalArgument("cz", z);

        lKernel.setOptionalArgument("a", 1);
        lKernel.setOptionalArgument("b", 1);
        lKernel.setOptionalArgument("c", 1);
        lKernel.setOptionalArgument("d", 1);

        lKernel.run(true);
        lImage.notifyListenersOfChange(lContext.getDefaultQueue());
        Thread.sleep(10);
      }

      lViewImage.waitWhileShowing();
    }

  }

  /**
   * Demos 3D image viewing - No Animation
   * 
   * @throws InterruptedException
   *           NA
   * @throws IOException
   *           NA
   */
  @Test
  public void demoViewImage3DUI() throws InterruptedException,
                                  IOException
  {

    ClearCLBackendInterface lClearCLBackendInterface =
                                                     ClearCLBackends.getBestBackend();
    try (ClearCL lClearCL = new ClearCL(lClearCLBackendInterface))
    {
      ClearCLDevice lFastestGPUDevice =
                                      lClearCL.getFastestGPUDeviceForImages();

      System.out.println(lFastestGPUDevice);

      ClearCLContext lContext = lFastestGPUDevice.createContext();

      int lSize = 213;

      ClearCLImage lImage =
                          lContext.createSingleChannelImage(ImageChannelDataType.UnsignedInt16,
                                                            lSize,
                                                            lSize,
                                                            lSize);

      ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImage);

      OffHeapMemory lBuffer =
                            OffHeapMemory.allocateShorts(lSize * lSize
                                                         * lSize);

      for (int i = 0; i < 10000 && lViewImage.isShowing(); i++)
      {
        for (int z = 0; z < lSize; z++)
          for (int y = 0; y < lSize; y++)
            for (int x = 0; x < lSize; x++)
              lBuffer.setShortAligned(x + lSize * y
                                      + lSize * lSize * z,
                                      (short) ((x + i) ^ y ^ z));

        lImage.readFrom(lBuffer, true);

        lImage.notifyListenersOfChange(lContext.getDefaultQueue());

        Thread.sleep(10);
      }

      lViewImage.waitWhileShowing();
    }

  }
}

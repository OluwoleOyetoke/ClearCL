/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clearcl.interview;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLHostImageBuffer;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.BuildStatus;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.exceptions.ClearCLException;
import clearcl.interview.ClearCLBioImage;
import clearcl.io.TiffWriter;
import clearcl.viewer.ClearCLImageViewer;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import static org.junit.Assert.assertEquals;

/**
 *
 * Painter class
 */
public class Painter {

    public int maxX;
    public int maxY;
    public int maxZ;

    public Painter(int maxX, int maxY, int maxZ) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxY;
    }

    /**
     * Used to create image
     *
     * @return image
     */
    public int[][][] createImage() throws ClearCLException {
        ClearCLBioImage img = new ClearCLBioImage(maxX, maxY, maxZ);
        for (int i = 0; i < maxX; i++) {
            for (int j = 0; j < maxY; j++) {
                for (int k = 0; k < maxZ; k++) {
                    img.paint(i, j, k);

                }
            }
        }
        return img.ImageArray;
    }

    /**
     * Used to save image as TIFF
     *
     * @param imageArray image array
     */
    public void saveImage(int[][][] imageArray) {

    }

    public void paint() throws Exception, Throwable {
        //Get Back End Interface (JOCL, JCL etc)
        ClearCLBackendInterface lClearCLBackend = ClearCLBackends.getBestBackend();

        //Create ClearCL object for OpenCL
        try (ClearCL lClearCL = new ClearCL(lClearCLBackend)) {

            //Get best available GPU device attached to platform
            ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();
            System.out.println("DETAILS OF BEST GPU ATTACHED TO PLATFORM: \n " + lBestGPUDevice.getInfoString());
            System.out.println("Max Work Group Size: " + lBestGPUDevice.getMaxWorkGroupSize());
            //create context for GPU device (command queues and memory objects will be attached to it)
            ClearCLContext lContext = lBestGPUDevice.createContext();

            //Creates 1D, 2D, or 3D image with a given memory allocation and access policy, channel order, channel data type, and dimensions.        
            final ClearCLImage lImageSrc
                    = lContext.createImage(HostAccessType.WriteOnly,
                            KernelAccessType.ReadWrite,
                            ImageChannelOrder.Intensity,
                            ImageChannelDataType.Float,
                            maxX,
                            maxY,
                            maxZ);
            System.out.println("Image Dimensions: " + Arrays.toString(lImageSrc.getDimensions()));

            final ClearCLProgram lProgram = lContext.createProgram(this.getClass(), "img_edit.cl");

            //Build program
            final BuildStatus lBuildStatus = lProgram.buildAndLog();
            System.out.println("Kernel Build Status: " + lBuildStatus);

            //Extract hyperboloid painter kernel
            final ClearCLKernel lKernel = lProgram.createKernel("painter");

            lKernel.setArgument("image", lImageSrc);
            lKernel.setArgument("oneD", maxX);
            lKernel.setArgument("twoD", maxX * maxY);
            lKernel.setGlobalSizes(maxX * maxY * maxZ); //set global size to the dimension of the image
            long maxWorkGroupSize = lBestGPUDevice.getMaxWorkGroupSize();
            int dimension = (int) Math.sqrt(maxWorkGroupSize);
            lKernel.setLocalSizes(dimension, dimension);
            if ((maxX * maxY * maxZ) % (dimension * dimension) != 0) {
                System.out.println("Please Check " + this.getClass().getSimpleName() + " And set appropriate work dimension for the chosen image resolution");
                return;
            }
            System.out.println("Global Size: " + Arrays.toString(lKernel.getGlobalSizes()));
            System.out.println("Local Size: " + Arrays.toString(lKernel.getLocalSizes()));
            lKernel.run();

            final ClearCLImage lImageDst = lContext.createImage(HostAccessType.ReadOnly,
                    KernelAccessType.WriteOnly,
                    ImageChannelOrder.Intensity,
                    ImageChannelDataType.Float,
                    maxX,
                    maxY,
                    maxZ);

            //Copy image from device into host memory
            lImageSrc.copyTo(lImageDst, true);

            //Display on screen
            ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImageDst);

            //write to TIFF
            TiffWriter lTiffWriter = new TiffWriter(NativeTypeEnum.Byte, 1f, 0f);
            File lFile8 = File.createTempFile(this.getClass().getSimpleName(), "Hyperboloid8");
            File lFile16 = File.createTempFile(this.getClass().getSimpleName(), "Hyperboloid16");
            File lFile32 = File.createTempFile(this.getClass().getSimpleName(), "Hyperboloid32");
            lFile8.deleteOnExit();
            lFile16.deleteOnExit();
            lFile32.deleteOnExit();

            lTiffWriter.setOverwrite(true);
            lTiffWriter.setBytesPerPixel(8);
            lTiffWriter.write(lImageDst, lFile8);
            lTiffWriter.setBytesPerPixel(16);
            lTiffWriter.write(lImageDst, lFile16);
            lTiffWriter.setBytesPerPixel(32);
            lTiffWriter.write(lImageDst, lFile32);
        }
    }

    /**
     * Filters and displays the image using a (3,3,3) box filter
     *
     * @param lImageSrc image to filter
     * @param lContext context to use
     */
    void filter(ClearCLImage lImageSrc, ClearCLContext lContext, int MAX_WORK_GROUP_SIZE) throws Exception {
        if (lImageSrc == null || lContext == null) {
            throw new ClearCLException("Empy image or context received by box filter function");
        }

        //Create program
        final ClearCLProgram lProgram = lContext.createProgram(this.getClass(), "img_edit.cl");

        //Build program
        final BuildStatus lBuildStatus = lProgram.buildAndLog();
        System.out.println("Kernel Build Status: " + lBuildStatus);

        //Extract box filter kernel
        final ClearCLKernel lKernel = lProgram.createKernel("box_filter");

        lKernel.setArgument("image", lImageSrc);
        lKernel.setArgument("oneD", maxX);
        lKernel.setArgument("twoD", maxX * maxY);
        lKernel.setGlobalSizes(maxX, maxY, maxZ); //set global size to the dimension of the image
        //Check feasible local dimension
        int neededWorkGroupSize = 3 * 3 * maxZ;
        if (neededWorkGroupSize < MAX_WORK_GROUP_SIZE) {
            System.out.println("Maximum work group size allowed by your device"
                    + " not sufficient enough to perform 3x3x" + maxZ + " convolution in one swoop"
                    + "Please use your device maxg work group size value to adjust function appropriately");
            return;
        }
        lKernel.setLocalSizes(3, 3);
        lKernel.run();
    }
}

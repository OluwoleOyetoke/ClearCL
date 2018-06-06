package clearcl.interview;

import clearcl.ClearCL;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.ClearCLKernel;
import clearcl.ClearCLProgram;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.ClearCLBackends;
import clearcl.enums.BuildStatus;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.KernelAccessType;
import clearcl.exceptions.ClearCLException;
import clearcl.io.TiffWriter;
import clearcl.viewer.ClearCLImageViewer;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * InterviewDemo class
 *
 * @author Oluwole Oyetoke
 */
public class InterviewDemo {

    private int width;
    private int height;
    private int depth;

    private ClearCL lClearCL;
    private ClearCLBackendInterface lClearCLBackend;
    private ClearCLDevice lBestGPUDevice;
    private ClearCLContext lContext;
    private ClearCLProgram lProgram;
    private final BuildStatus lBuildStatus;
    private long MAX_WORK_GROUP_SIZE;
    private String pathToSaveImageTo;

    /**
     * Default constructor. Gets backend, Cl object,device, context, program and
     * builds
     *
     * @param width width of images to be used for most of the demos
     * @param height height of images to be used for most of the demos
     * @param depth depth of images to be used for most of the demos
     */
    public InterviewDemo(int width, int height, int depth) throws Exception, Throwable {
        this.width = width;
        this.height = height;
        this.depth = height;

        //Get Back End Interface (JOCL, JCL etc)
        lClearCLBackend = ClearCLBackends.getBestBackend();

        //Create ClearCL object for OpenCL
        try {
            this.lClearCL = new ClearCL(lClearCLBackend);

            //Get best available GPU device attached to platform
            lBestGPUDevice = lClearCL.getBestGPUDevice();
            MAX_WORK_GROUP_SIZE = lBestGPUDevice.getMaxWorkGroupSize();

            System.out.println("DETAILS OF BEST GPU ATTACHED TO PLATFORM: \n "
                    + lBestGPUDevice.getInfoString());
            System.out.println("Max Work Group Size: "
                    + MAX_WORK_GROUP_SIZE);

            //create context for GPU device (command queues and memory objects will be attached to it)
            lContext = lBestGPUDevice.createContext();

            //Get and build program
            lProgram = lContext.createProgram(this.getClass(), "interview.cl");
            lBuildStatus = lProgram.buildAndLog();
            System.out.println("Kernel Build Status: " + lBuildStatus);
            pathToSaveImageTo = System.getProperty("java.io.tmpdir");
        } catch (Exception ex) {
            throw new Exception("Error encountered: ", ex);
        }
    }

    /**
     * Uses the hyperboloid kernel to paint an X,Y,Z image. Output image is
     * normalized to [0,255]
     */
    public void hyperboloidPaint() throws IOException {
        System.out.println("---------HYPERBOLOID PAINT DEMO------\n");
        //Creates 1D, 2D, or 3D image with a given memory allocation and access policy,
        //channel order, channel data type, and dimensions.        
        ClearCLImage hyperboloidImageDst
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite,
                        ImageChannelDataType.SignedInt32, width, height, depth);
        System.out.println("Hyperboloid Image Dimensions: " + Arrays.toString(hyperboloidImageDst.getDimensions()));

        //Extract hyperboloid painter kernel
        final ClearCLKernel lKernel = lProgram.createKernel("hyperboloid_paint");

        //Set Work Group Size
        lKernel.setGlobalSizes(width * height * depth); //set global size to the dimension of the image
        long maxWorkGroupSize = lBestGPUDevice.getMaxWorkGroupSize();
        long workGroupSizeToUse = ((width * height * depth) < maxWorkGroupSize) ? width : maxWorkGroupSize;
        if (width <= maxWorkGroupSize) {
            lKernel.setLocalSizes(workGroupSizeToUse);
        }
        System.out.println("Global Size: " + Arrays.toString(lKernel.getGlobalSizes()));
        System.out.println("Local Size: " + Arrays.toString(lKernel.getLocalSizes()));

        //Get normalization quotients
        float[] normalizationQuotients = getNormalizationQuotients(0, 255); //to help normalize the generated image to [0, 255]     

        //Set Kernel Arguments
        lKernel.setArgument("image", hyperboloidImageDst);
        lKernel.setArgument("oneD", width);
        lKernel.setArgument("twoD", width * height);
        lKernel.setArgument("actualLowerRange", normalizationQuotients[0]);
        lKernel.setArgument("desiredLowerRange", normalizationQuotients[1]);
        lKernel.setArgument("quotient", normalizationQuotients[2]);
        lKernel.run();

        try {
            //display normal and write to tiff
            show(hyperboloidImageDst, false);
            writeToTiff(hyperboloidImageDst, "hyperboloid",false, true);
        } catch (Throwable ex) {
            Logger.getLogger(InterviewDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //filter and display filtered
        boxFilter(hyperboloidImageDst, 3, 1); //filter size 3x3xdepth, stride 1
        System.out.println("\n---------END OF HYPERBOLOID PAINT DEOM------\n");
    }

    /* 
    SAMPLE DCT INPUT:
    int test[] = {
                99,   99,   99,   99,
                99,   99,   99,   99,
                99,   99,   99,   99,
                99,   99,   99,   255};
    
    SAMPLE DCT OUTPUT
    int dctResult[] ={435.000   -50.956    39.000   -21.107
                     -50.956    66.577   -50.956    27.577
                      39.000   -50.956    39.000   -21.107
                    -21.107    27.577   -21.107    11.423};
     */
    /**
     * Computes DCT2 for an entire image (Not 8x8) Reverts the image by
     * computing the idct of the dct output
     *
     * @throws Throwable exception
     */
    public void doDct2() throws Throwable {
        System.out.println("---------DCT/iDCT DEMO------\n");
        //Get image to perfrom dct on
        String path = getClass().getResource("lena.bmp").getPath();
        ClearCLImage2D bufferedImage = new ClearCLImage2D(path, true, 256, 256);
        bufferedImage.showImage(); //View Before DCT
        bufferedImage.save(pathToSaveImageTo+"saved_lena.png");
        System.out.println("Image saved to: "+pathToSaveImageTo+"saved_lena.png");
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        //dct src clear cl image
        ClearCLImage dctImageSrc
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite, ImageChannelDataType.SignedInt32, width, height);

        //create destination of the dct operation
        ClearCLImage dctImageDst
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite, ImageChannelDataType.SignedInt32, width, height);

        //load buffered image
        dctImageSrc.readFrom(bufferedImage.imageData1D, true);
        dctImageSrc.notifyListenersOfChange(lContext.getDefaultQueue());

        //run kernel
        ClearCLKernel lKernel = lProgram.createKernel("dct2");
        lKernel.setArgument("imgSrc", dctImageSrc);
        lKernel.setArgument("imgDst", dctImageDst);
        lKernel.setArgument("width", width);
        lKernel.setArgument("height", height);
        lKernel.setGlobalSizes(dctImageSrc);
        lKernel.run(true);
        dctImageDst.notifyListenersOfChange(lContext.getDefaultQueue());

        //copy out output dctImageDst image data into normal java array
        OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(height * width);
        dctImageDst.writeTo(lBuffer, true);
        int[] received = new int[(height * width)];
        lBuffer.copyTo(received);

        //recreate ClearCL 2D Image and show
        bufferedImage.setImageData1D(received);
        bufferedImage.showImage(); //After DCT;
        bufferedImage.save(pathToSaveImageTo+"saved_dct_lena.png");
        System.out.println("Image saved to: "+pathToSaveImageTo+"saved_dct_lena.png");

        /* 
        //display with ClearCL Viewer and write to tiff 
        show(lContext, dctImageDst, false);   
        writeToTiff( dctImageDst, "dct_lena",false, true);
        
        //Normalize after DCT and show with JFrame [0, 255]
        bufferedImage.normalize(0, 255);
        bufferedImage.showImage(); //After Normalization;
        bufferedImage.saveImage(pathToSaveImageTo+"saved_normalized_lena.png");
        //System.out.println("After Normalization: \n" + Arrays.toString(lImage.imageData1D));
         */
        
        /*do idct of dctImageDst and reshow (image should look almost the same as initial).
        create destination of the idct operation*/
        ClearCLImage idctImageDst
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite, ImageChannelDataType.SignedInt32, width, height);

        //run kernel
        ClearCLKernel lKernel2 = lProgram.createKernel("idct2");
        lKernel2.setArgument("imgSrc", dctImageDst);
        lKernel2.setArgument("imgDst", idctImageDst);
        lKernel2.setArgument("width", width);
        lKernel2.setArgument("height", height);
        lKernel2.setGlobalSizes(dctImageSrc);
        lKernel2.run(true);
        idctImageDst.notifyListenersOfChange(lContext.getDefaultQueue());

        //copy out output dctImageDst image data into normal java array
        OffHeapMemory lBuffer2 = OffHeapMemory.allocateFloats(height * width);
        idctImageDst.writeTo(lBuffer2, true);
        int[] received2 = new int[(height * width)];
        lBuffer2.copyTo(received2);

        //recreate ClearCL 2D Image and show
        bufferedImage.setImageData1D(received2);
        bufferedImage.showImage(); //After iDCT;
        bufferedImage.save(pathToSaveImageTo+"saved_idct_lena.png");
         System.out.println("Image saved to: "+pathToSaveImageTo+"saved_idct_lena");
        
        System.out.println("\n---------END OF DCT/iDCT DEMO------\n");
    }

    /**
     * Filters and displays the image using a (3,3,3) box filter
     *
     * @param lImageSrc image to filter
     * @param lContext context to use
     */
    //void boxFilter(ClearCLImage  lImageSrc, int filterSize, int stride) throws Exception {
    void boxFilter(ClearCLImage lImageSrc, int filterSize, int stride){
        System.out.println("---------BOX FILTERATION DEMO------\n");
        
        //confirm tha lImageSrc is of type INT (because the box filter kernel works with int)
        if(lImageSrc==null || lImageSrc.isInteger()==false){
              throw new ClearCLException("lImageSrc is null or not of type integer.");
        }

        int width = (int) lImageSrc.getWidth();
        int height = (int) lImageSrc.getHeight();
        int depth = (int) lImageSrc.getDepth();
        
        System.out.println("Input Image Dimension:  Width ("+width +") Height ("+height +")  Depth ("+depth +")");
        System.out.println("Box Filter Dimension:  Width ("+filterSize +") Height ("+filterSize +")  Depth ("+depth +")");
         
          ClearCLImage lboxFilter
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite, ImageChannelDataType.SignedInt32, filterSize, filterSize, depth);
        int[] boxFilterArray = new int[filterSize * filterSize * depth];
        Arrays.fill(boxFilterArray, 1);
        lboxFilter.readFrom(boxFilterArray, true);
        lboxFilter.notifyListenersOfChange(lContext.getDefaultQueue());

        if (filterSize >= width || filterSize >= height) {
            throw new ClearCLException("Filter size should be less than image");
        }

        //confirm that chosen stride is feasible
        if (Math.floor((width - filterSize) / stride) != Math.floor((width - filterSize) / stride)
                || Math.floor((height - filterSize) / stride) != Math.floor((width - filterSize) / stride)) {
            throw new ClearCLException("Chosen stride not feasible for image height or width");
        }

        //calculate dimension of the output of the filter operation
        int newX = (int) Math.floor((width - filterSize) / stride) + 1;
        int newY = (int) Math.floor((width - filterSize) / stride) + 1;
        int xMovements = (int) Math.floor((width-filterSize)/stride);
        int yMovements = (int) Math.floor((height-filterSize)/stride);
        System.out.println("Image Dimension (After Filtration):  Width ("+newX+") Height ("+newY+")");
        System.out.println("Number of filter movement on Image's X axis: "+xMovements);
        System.out.println("Number of filter movement on Image's Y axis: "+yMovements);

        //create destination for the filter operation
        ClearCLImage filterImageDst
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite, ImageChannelDataType.SignedInt32, newX, newY);

        //extract box filter kernel
        final ClearCLKernel lKernel = lProgram.createKernel("box_filter");

        //load kernel arguments and run
        lKernel.setArgument("inputImage", lImageSrc);
        lKernel.setArgument("outputImage", filterImageDst);
        lKernel.setArgument("boxFilter", lboxFilter);
        lKernel.setArgument("stride", stride);
        lKernel.setGlobalSizes(width, height, depth); //set global size to the dimension of the image
        lKernel.run(true);
        
        //copy out output of the filteration
        OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(newX * newY);
        filterImageDst.writeTo(lBuffer, true);
        int[] received = new int[(newX * newY)];
        lBuffer.copyTo(received);
        //System.out.println("Filtered: "+Arrays.toString(received));
        
        //use viewer to show filtered image
        show(filterImageDst, false);
        
        System.out.println("\n---------END OF BOX FILTERATION DEMO------\n");
    }
    
       /**
     * Used to debug CL Kernel issue. Paints a circle on the ClearCL Viewer
     * Panel....
     */
    public void criclePaint() {
        System.out.println("---------CIRCLE PAINT DEMO------\n");
        //dct src clear cl image        
        ClearCLImage lImageSrc
                = lContext.createSingleChannelImage(HostAccessType.ReadWrite,
                        KernelAccessType.ReadWrite,
                        ImageChannelDataType.SignedInt32, width, height, depth);
        System.out.println("Circle Image Dimensions: " + Arrays.toString(lImageSrc.getDimensions()));

        ClearCLKernel lKernel = lProgram.createKernel("circle_paint_3d");
        lKernel.setArgument("image", lImageSrc);
        lKernel.setArgument("h", (int) width / 2);
        lKernel.setArgument("k", (int) width / 2);
        lKernel.setArgument("r", (int) width / 4);
        lKernel.setGlobalSizes(lImageSrc);
        lKernel.run(true);

        try {
            //display
            show(lImageSrc, false);
        } catch (Exception ex) {
            Logger.getLogger(InterviewDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        boxFilter(lImageSrc, 3, 1); //filter size 3x3xdepth, stride 1
        System.out.println("\n---------END OF CIRCLE PAINT DEMO------\n");
    }


    /**
     * USed to display the generated image from the kernel
     *
     * @param lContext device context
     * @param lImageSrc image to display
     * @param waitWhileShowing (if true), do not progress with code until image
     * frame is closed
     * @throws Exception exception
     */
    private void show(ClearCLImage lImageSrc, boolean waitWhileShowing) {
        lImageSrc.notifyListenersOfChange(lContext.getDefaultQueue());
        ClearCLImageViewer lViewImage = ClearCLImageViewer.view(lImageSrc);
        try{
            Thread.sleep(10);
        }catch(Exception ex){
             Logger.getLogger(InterviewDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (waitWhileShowing) {
            lViewImage.waitWhileShowing();
        }
    }

    /**
     * Used to write image to Tiff using the TIFF writer. Write 8, 16 and 32 bit
     * version of the image
     *
     * @param lImageSrc image to write
     * @param name name to give the file
     * @param deleteOnExit specifies if to delete file on exit
     * @param overwrite specifies if to overwrite file if already exists
     * @throws Throwable
     */
    public void writeToTiff(ClearCLImage lImageSrc, String name,
            boolean deleteOnExit, boolean overwrite) throws Throwable {

        //write to TIFF
        TiffWriter lTiffWriter = new TiffWriter(NativeTypeEnum.Byte, 1f, 0f);;
        File lFile32 = File.createTempFile(this.getClass().getSimpleName(), name + "-32bits");

        if (deleteOnExit) {
            lFile32.deleteOnExit();
        }
        if (overwrite) {
            lTiffWriter.setOverwrite(true);
        }
        lTiffWriter.setBytesPerPixel(32);
        lTiffWriter.write(lImageSrc, lFile32);
    }

    /**
     * Transformation is done based on assumption that the image is to be
     * painted with the hyperboloid formular.
     * <b>x^2+y^2-z^2=0</b>
     */
    private float[] getNormalizationQuotients(int desiredLowerRange, int desiredHigherRange) {
        //lowest minimum possible
        float actualLowerRange = (float) (-1 * Math.pow((depth - 1), 2)); //0^2 + 0^2 - depth^2

        //maximum possible
        float actualHigherRange = (float) (Math.pow((width - 1), 2) + Math.pow((height - 1), 2));

        float quotient = (float) ((desiredHigherRange - desiredLowerRange) / (actualHigherRange - actualLowerRange));

        float[] toReturn = {actualLowerRange, desiredLowerRange, quotient};
        return toReturn;
    }
}

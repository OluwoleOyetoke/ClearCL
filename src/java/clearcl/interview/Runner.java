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
import clearcl.test.ClearCLBasicTests;
import coremem.enums.NativeTypeEnum;
import coremem.offheap.OffHeapMemory;
import static org.junit.Assert.assertEquals;

/**
 * <b> Question 1</b> Create a 3D image of resolution 128x128x128, and ‘paint’ 
 * pixels with the hyperboloid such as:
 * <b>x^2+y^2-z^2=0</b> 
 * where (0,0,0) is at the center of the image. This must be done with a kernel. 
 * Save the image as TIFF file.
 * <h1>Approach</h1>
 * <ol>
 * <li>Host platform initializes OpenCL by querying available platforms</li>
 * 
 * <li>Desired platform is selected</li>
 * 
 * <li>Select desired device (CPU, GPU, ACCELERATOR etc.)</li>
 * 
 * <li>Create context to manage the selected device and kernel execution</li>
 * 
 * <li>Create command queue attached to the device context 6. Create memory 
 * objects attached to the context</li>
 * 
 * <li>In preparation for the kernel about to be launched, write all needed data
 * from host memory onto the memory objects (created in the step above) which 
 * actually will reside in the selected device’s memory</li>
 * 
 * <li>Load kernel file (.cl) containing one or more kernels to be launched</li>
 * 
 * <li>Create kernel program object (a collection of ready to run kernels) 
 * using the loaded kernel file</li>
 * 
 * <li>Build created program object into binaries</li>
 * 
 * <li>Extract only the required kernel(s) from the built program </li>
 * 
 * <li>Pass all the arguments needed by the kernel to run to it one after the other</li>
 * 
 * <li>Enqueue the task by passing the command que and the kernel to the enqueue
 * function</li>
 * 
 * <li>Read device memory object content back to the host memory</li>
 * 
 * <li>Use synchronization mechanism (e.g. clFinish(commandqueue)) to ensure that
 * kernel finished execution before next host function is run (if need be)</li>
 * </ol>
 * 
 * @author Oluwole Oyetoke
 */
public class Runner {

    public static void main(String[] args) throws Exception, Throwable{ 
        System.out.println("Main Now runing");
        Painter painter = new Painter(128, 128, 128);
        //Painter painter = new Painter(3, 3, 3);
        painter.paint();
      

        System.out.println("Main Now run");
    }
}

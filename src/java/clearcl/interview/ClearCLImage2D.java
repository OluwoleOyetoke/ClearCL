package clearcl.interview;

import clearcl.exceptions.ClearCLException;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.jocl.CLException;

/**
 * Used to handle 2d image operations within ClearCL
 *
 * @author Oluwole Oyetoke
 */
public class ClearCLImage2D {

    //Global variables
    public int[][] imageData2D;
    public int[] imageData1D;
    public BufferedImage bufferedImage;
    private int width;
    private int height;
    private int type;

    /**
     * Default constructor. Helps specify the resolution of image to be created.
     *
     * @param pathToImage path to 2d image to load (supported formats include
     * .jpg, .bmp, .png...)
     * @param resize specify if the image should be resized
     * @param xyDimensions x and y dimensions of the resized image (if
     * resize==true)
     *
     * TODO: Add more constructors to help build 2D image independent of path to
     * image
     */
    public ClearCLImage2D(String pathToImage, boolean resize, int... xyDimensions) {
        //validate received file path
        File file = new File(pathToImage);
        if (file == null) {
            throw new CLException("Image file specified is empty");
        }

        //Convert to buffered image
        BufferedImage imageToUse;
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException ex) {
            Logger.getLogger(ClearCLImage2D.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        //Confirm that image is 2D
        if (image.getData().getNumDataElements() != 1) {
            throw new CLException("Image not 2D...[function name] only works with 2D");
        }

        //Check if resize is needed
        if (resize && (xyDimensions == null || xyDimensions.length != 2)) {
            throw new CLException("Please sepcify the new width and height you will like your image to be resized to (e.g ...add example here (x,y))");
        }
        this.type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
        if (resize) {
            this.width = xyDimensions[0];
            this.height = xyDimensions[1];
            imageToUse = resizeImage(image, type, this.width, this.height);
        } else {
            imageToUse = image;
            this.width = imageToUse.getWidth();
            this.height = imageToUse.getHeight();
        }

        //Write image data into 2d and/or 1d array (to be used by OpenCL kernel)
        int[][] imageData2D = new int[this.width][this.height];
        int[] imageData1D = new int[this.width * this.height];
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                imageData2D[i][j] = imageToUse.getData().getSample(i, j, 0);
                imageData1D[j + (i * width)] = imageData2D[i][j];
            }
        }
        this.imageData2D = imageData2D;
        this.imageData1D = imageData1D;
        buildBuffer();
    }

    /**
     * Display 2D image using a JFrame
     */
    public void showImage() throws IOException {
        showImage(this.bufferedImage);
    }

    /**
     * Display 2D image using JFrame
     *
     * @param image image to display
     */
    public static void showImage(BufferedImage image) {
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Display 2D image using JFrame
     *
     * @param pathToImage path to image
     */
    public static void showImage(String pathToImage) {
        //Validate received file path
        File file = new File(pathToImage);
        if (file == null) {
            throw new CLException("Image file specified is empty");
        }
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException ex) {
            Logger.getLogger(ClearCLImage2D.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        showImage(image);
    }

    /**
     * Rebuild buffered image object for this image. To be used when changes
     * have been made to the image data
     */
    public void buildBuffer() {
        //validate imageData
        if (imageData2D == null || imageData1D == null) {
            throw new CLException("Empty image data received");
        }
        int width = imageData2D[0].length;
        int height = imageData2D.length;
        BufferedImage image = new BufferedImage(width, height, this.type);
        WritableRaster raster = image.getRaster();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                raster.setSample(x, y, 0, imageData2D[x][y]);
            }
        }
        image.setData(raster);
        this.bufferedImage = image;
    }

    /**
     * Used to save the 2D image as png in system temp folder
     *@param name name of image e.g name = 'lena'
     * @throws IOException exception
     */
    public void saveAs(String name) throws IOException {
        String tempFolder = System.getProperty("java.io.tmpdir");
        String pathTo = tempFolder+""+name+".png";
        File ImageFile = new File(pathTo);
        ImageIO.write(this.bufferedImage, "png", ImageFile);
    }
    
        /**
     * Used to save the 2D image as png
     *
     * @param pathTo path to save image
     * @throws IOException exception
     */
    public void save(String pathTo) throws IOException {
        if (pathTo == null) {
            throw new IOException("Empty file path received");
        }
        File ImageFile = new File(pathTo);
        ImageIO.write(this.bufferedImage, "png", ImageFile);
    }

    /**
     * USed to resize the 2D image..Nor made public
     *
     * @param originalImage original buffered image
     * @param type image type
     * @param imgWidth image width
     * @param imgHeight image height
     * @return
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int type, int imgWidth, int imgHeight) {
        BufferedImage resizedImage = new BufferedImage(imgWidth, imgHeight, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, imgWidth, imgHeight, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * Used to get 2D image data
     *
     * @return imageData2D 2D image data (int[][])
     */
    public int[][] getImageData2D() {
        return imageData2D;
    }

    /**
     * USed to get the buffered image object
     *
     * @return bufferedImage buffered image
     */
    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    /**
     * Used to set the buffered image content
     *
     * @param bufferedImage
     */
    private void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        buildBuffer();
    }

    /**
     * Set 2D array of in into image
     *
     * @param imageData2D image data 2D
     */
    public void setImageData2D(int[][] imageData2D) {

        if (imageData2D != null) {
            //watch out for image size...Max 4.2B
            double check1 = Math.sqrt(imageData2D.length);
            double check2 = Math.sqrt(imageData2D[0].length);
            if ((check1 + check2) >= (2 * Math.sqrt(Integer.MAX_VALUE))) {
                System.out.println("2D array bigger than what a 1D array can receive");
                return;
            }

            this.height = imageData2D.length;
            this.width = imageData2D[0].length;
            int[] imageData1D = new int[width * height];
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    imageData1D[j + (i * width)] = imageData2D[j][i];
                }
            }
            this.imageData2D = imageData2D;
            this.imageData1D = imageData1D;
        } else {
            this.height = 0;
            this.width = 0;
            this.imageData1D = null;
            this.imageData2D = null;
        }
        buildBuffer();
    }

    /**
     * Get 1D arraay of image data
     *
     * @return imageData1D image data 1D
     */
    public int[] getImageData1D() {
        return imageData1D;
    }

    /**
     * Place 1D array of ints into image
     *
     * @param imageData1D data to place in
     */
    public void setImageData1D(int[] imageData1D) {
        if (this.imageData1D == null) {
            throw new ClearCLException("Invalid x or y co-ordinate received");
        }

        int[][] newImageData2D = null;
        if (imageData1D == null) {
            this.height = 0;
            this.width = 0;
            this.imageData1D = null;
            this.imageData2D = null;
        } else {

            if (imageData1D.length != (width * height)) {
                throw new ClearCLException("1D data you are trying to place in image does not match its initialized width x height");
            }
            newImageData2D = new int[width][height];
            int a = 0;
            int b = 0;
            for (int i = 0; i < imageData1D.length; i++) {
                a = i % width;
                b = (int) Math.floor(i / width);
                newImageData2D[b][a] = imageData1D[i];
            }
            this.imageData1D = imageData1D;
            this.imageData2D = newImageData2D;
        }
        buildBuffer();
    }

    /**
     * Redistributes the pixels of the picture between [0-255]
     */
    public void normalize(int minPixel, int maxPixel) {
        if (imageData1D == null) {
            return;
        }

        //find maximum pixel in picture
        int[] minMax = findMinMax();
        int desiredHigherRange = maxPixel;
        int desiredLowerRange = minPixel;
        int actualLowerRange = minMax[0];
        int actualHigherRange = minMax[1];

        double quotient = (double) ((double) (desiredHigherRange - desiredLowerRange) / (double) (actualHigherRange - actualLowerRange));
        System.out.println("Quotient: " + quotient);
        //normalize
        int[] normalizedImage = new int[imageData1D.length];
        for (int i = 0; i < imageData1D.length; i++) {
            normalizedImage[i] = (int) Math.round(((imageData1D[i] - actualLowerRange) * quotient) + desiredLowerRange);
        }
        this.setImageData1D(normalizedImage);
    }

    /**
     * Find the maximum and minimum pixel in the image
     *
     * @return minMax minMax[0] = minimum, minMax[1] = maximum
     */
    private int[] findMinMax() {
        if (imageData1D == null) {
            throw new ClearCLException("Image is empty");
        }
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < imageData1D.length; i++) {
            min = (imageData1D[i] < min) ? imageData1D[i] : min;
            max = (imageData1D[i] > max) ? imageData1D[i] : max;
        }
        int[] minMax = {min, max};
        System.out.println("Min: " + min + ", Max: " + max);
        return minMax;
    }

    /**
     * Get image width
     *
     * @return width width of image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Set image width. Not made public
     *
     * @param width image width to set in
     */
    private void setWidth(int width) {
        this.width = width;
    }

    /**
     * Get image height
     *
     * @return height image height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set image height...Not made public
     *
     * @param height height to set in
     */
    private void setHeight(int height) {
        this.height = height;
    }

    /**
     * Get image type
     *
     * @return type image type
     */
    public int getType() {
        return this.type;
    }

    /**
     * set image type..Not made public
     *
     * @param type image type to set in
     */
    private void setType(int type) {
        this.type = type;
    }
}

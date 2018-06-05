/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clearcl.interview;

import clearcl.exceptions.ClearCLException;
import java.awt.image.BufferedImage;

/**
 *
 * @author Oluwole Oyetoke
 */
public class ClearCLBioImage {

    public int[][][] ImageArray;
    private int maxX;
    private int maxY;
    private int maxZ;
    private int maxPixel;
    private int minPixel;
    private boolean normalized = false;

    /**
     * Default constructor. Helps specify the resolution of image to be created.
     * Also to determine if image is normalized or not
     *
     * @param maxX maximum x axis size
     * @param maxY maximum y axis size
     * @param maxZ maximum z axis size
     */
    public ClearCLBioImage(int maxX, int maxY, int maxZ) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        minPixel = 0;
        maxPixel = (normalized == true) ? 1 : 255;
        ImageArray = new int[maxX][maxY][maxZ];
    }

    public void paint(int x, int y, int z) throws ClearCLException {
        if (x < 0 || y < 0 || z < 0 || x > this.maxX || y > this.maxY || z > this.maxZ) {
            throw new ClearCLException(x + ", " + y + ", " + z + " is an invalid co-ordinate");
        }
        
         //RANGE[-16,384, 32,768]
        double val = Math.pow(x, 2) + Math.pow(y, 2) - Math.pow(z, 2);
        double a = ((val+16384)/(32768+16384));
        int normalizedVal = (int) Math.floor(255*a); //normalizes value between 0 and 255 
        ImageArray[x][y][z] = normalizedVal;
        
    }

}

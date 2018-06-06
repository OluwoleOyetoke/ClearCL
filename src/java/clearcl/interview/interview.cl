const sampler_t sampler =   CLK_NORMALIZED_COORDS_FALSE |
                            CLK_ADDRESS_CLAMP_TO_EDGE |
                            CLK_FILTER_NEAREST;


//A kernel to paint a 3D (128x128x128) image using a hyperpoloid formular
//image - Painted image is written into this
//oneD & twoD - Used by kernel to calculate x,y and z positions
//actualLowerRange, desiredLowerRange & quotient - Used to normalize the derived pixel value
__kernel void hyperboloid_paint ( __write_only image3d_t image,  const int oneD, const int twoD,
                                const float actualLowerRange, const float desiredLowerRange, const float quotient
) {
              
                __private int globalPos = get_global_id(0);
                __private float a =globalPos%twoD;
                __private float b = globalPos/twoD;

                __private int x = (int) a%oneD;
                __private int y = (int) floor(a/oneD);
		__private int z = (int) floor(b);
                
                //Get value and normalize from RANGE[-16,129, 32,258] - > [0, 255]
                __private float unNormalizedVal = pow(x, 2.0f) + pow(y, 2.0f) - pow(z,2.0f);
                __private int normalizedVal = (int) round(((unNormalizedVal-actualLowerRange)*quotient) + desiredLowerRange);
          
                //write to image 
                write_imagei (image, (int4)(x,y,z,0), (int4) normalizedVal);
}

//Kernel for performing DCT II on a 2D integer image
//imgSrc - image to perform DCT on
//imgDst - image to save DCT into
//width, height - width and height of the image
__kernel void dct2(__read_only image2d_t imgSrc, __write_only image2d_t imgDst, const int width, const int height){
        
        //Variables and constants
        __private int2 coord = (int2) (get_global_id(0), get_global_id(1));
        __private int2 localCoord = coord;
        __private int4 pixelToWrite = 0;
        __private int4 pij = 0;

        __private float cx = 0.0f;
        __private float cy = 0.0f;
        __private float fxy = 0.0f;
        __private float temp_sum = 0.0f;
        __private float a = 0.0f;
        __private float b = 0.0f;
        __private float c = 0.0f;
        __private float d = 0.0f;

        const float e = (float) ( 2/ (float) width);
        const float f = (float) ( 2/ (float) height);

        if(coord.x ==0){cx = (float) (1/sqrt((float)width));}
        else{cx = sqrt(e);}
        if(coord.y ==0){cy = (float) (1/sqrt((float)height));}
        else{cy =  sqrt(f);}
        
         for(int i=0; i<width; i++){
            for(int j=0; j<height; j++){
                localCoord.x = i; 
                localCoord.y = j; 
                pij = (int4) read_imagei (imgSrc, sampler, localCoord);
                a = ((coord.x * M_PI) * ((2*i)+1))/(2*width);
                b = cos(a);
                c = ((coord.y * M_PI) * ((2*j)+1))/(2*height);
                d = cos(c);
                temp_sum = (float) (temp_sum  + (pij.x*b*d));
            }
        }
        fxy = (float) (cx * cy * temp_sum);
        pixelToWrite = (int4) round(fxy);
        write_imagei (imgDst, coord, (int4)pixelToWrite);
}

//Kernel for performing Inverse DCT II on a 2D integer image
//imgSrc - image to perform iDCT on
//imgDst - image to save iDCT into
//width, height - width and height of the image
__kernel void idct2(__read_only image2d_t imgSrc, __write_only image2d_t imgDst, const int width, const int height){
      
        //Variables and constants
        __private int2 coord = (int2) (get_global_id(0), get_global_id(1));
        __private int2 localCoord = coord;
        __private int4 pixelToWrite = 0;
        __private int4 pij = 0;

        __private float cx = 0.0f;
        __private float cy = 0.0f;
        __private float fxy = 0.0f;
        __private float temp_sum = 0.0f;
        __private float a = 0.0f;
        __private float b = 0.0f;
        __private float c = 0.0f;
        __private float d = 0.0f;

        const float e = (float) ( 2/ (float) width);
        const float f = (float) ( 2/ (float) height);
        
         for(int i=0; i<width; i++){
            for(int j=0; j<height; j++){
                localCoord.x = i; 
                localCoord.y = j;

                if(i ==0){cx = (float) (1/sqrt((float)width));}
                else{cx = sqrt(e);}
                if(j ==0){cy = (float) (1/sqrt((float)height));}
                else{cy =  sqrt(f);}

                
                pij = (int4) read_imagei (imgSrc, sampler, localCoord);
                a = ((i * M_PI) * ((2*coord.x)+1))/(2*width);
                b = cos(a);
                c = ((j * M_PI) * ((2*coord.y)+1))/(2*height);
                d = cos(c);
                temp_sum = (float) (temp_sum  + (pij.x*b*d*cx*cy));
            }
        }
        pixelToWrite = (int4) round(temp_sum);
        write_imagei (imgDst, coord, (int4)pixelToWrite);
}

//Kernel for performing filteration using a box filter on an image
//inputImage - image to perform filteration on
//outputImage - image to save filteration into
//boxFilter - filter
//stride - hops to be made by the filter
__kernel void box_filter(__read_only image3d_t inputImage, __write_only image2d_t outputImage,
                         __read_only image3d_t boxFilter, const int stride ){
            
            //Constant variables
            const int filterWidth = get_image_width(boxFilter);
            const int filterHeight = get_image_height(boxFilter);
            const int imageWidth = get_image_width(inputImage);
            const int imageHeight = get_image_height(inputImage);
            const int imageDepth = get_image_depth(inputImage);
            
            //private variables
            __private int divider = filterWidth*filterHeight*imageDepth;
            __private int4 coord = (get_global_id(0), get_global_id(1), get_global_id(2)); //global co-ordinate 
            __private int2 outputCoord = (get_global_id(0), get_global_id(1));
            __private int4 imageLocalCoord = coord; 
            __private int4 filterLocalCoord = coord; //local corordinates of the e.g 3x3 filter during iteration
            __private int4 result = 0;

            __private int x = get_global_id(0);
            __private int y = get_global_id(1);
            __private int z = get_global_id(2);

            __private int sum = 0;
            __private int4 readImage =0;
            __private int4 readFilter =0; //always 1

        if( (z>0) || (x>(imageWidth-filterWidth)) || (y>(imageHeight-filterHeight)) ||
            ((x%stride)!=0) ||  ((y%stride)!=0) )  {
                return;
        }

            for(int i=x; i<(x+filterWidth); i++){
                imageLocalCoord.x = i;
                filterLocalCoord.x = x-i;
                for(int j=y; j<(y+filterHeight); j++){
                    imageLocalCoord.y = j;
                    filterLocalCoord.y = y-j;
                    for(int k=0; k<imageDepth; k++){
                        imageLocalCoord.z = k;
                        filterLocalCoord.z = z-i;
                        readImage = (int4) read_imagei(inputImage, sampler, imageLocalCoord);
                        readFilter = (int4) read_imagei(boxFilter, sampler, filterLocalCoord); //always one because the filter is filled with ones
                        sum = (int)  (sum + (readImage.x * readFilter.x));
                    }
                }
            }
            
           result = (int4) round((float) (sum/divider));
           outputCoord.x = round ((float) ( ( (imageLocalCoord.x+1) - filterWidth  ) /stride));
           outputCoord.y = round((float) ( ( (imageLocalCoord.y+1) - filterHeight ) /stride));      
           write_imagei(outputImage,outputCoord,(int4) result);          
}

//Kernel used to draw a 2D circle..on a 2D plane
//image  - Painted image is written into this
//h, k, r - Used to calculate circle region
__kernel void circle_paint_2d ( __write_only image2d_t image, const int h, const int k, const int r){
            __private int2 coord = (int2) (get_global_id(0), get_global_id(1));            
            int x = get_global_id(0); 
            int y = get_global_id(1);
            const int rSquared=r*r;

            //Draw Circle
            __private float left = pow((x-h),2.0f);
            __private float right = pow((y-k), 2.0f);
            if((left+right)<=rSquared){             
                write_imagef (image, coord , 0);
            }else{              
                write_imagef (image, coord  , 255);
            }
}


//Kernel used to draw a circle on a 3d image object
//image  - Painted image is written into this
//h, k, r - Used to calculate circle region
__kernel void circle_paint_3d ( __write_only image3d_t image, const int h, const int k, const int r){            
            int x = get_global_id(0); 
            int y = get_global_id(1);
            int z = get_global_id(2);
            const int rSquared=r*r;

            //Draw Circle
            __private float left = pow((x-h),2.0f);
            __private float right = pow((y-k), 2.0f);
            if((left+right)<=rSquared){
                write_imagei (image, (int4)(x, y, z, 0), 0);
            }else{
                write_imagei (image, (int4)(x, y, z, 0), 255);
            }
}

__kernel void test_fill(__read_only image2d_t imgSrc, __write_only image2d_t imgDst ){
        __private int2 coord = (int2) (get_global_id(0), get_global_id(1));
        write_imagef (imgDst, coord, 255.0f);
}
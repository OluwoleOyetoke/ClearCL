

//A kernel to paint a 3D image using a hyperpoloid formular
__kernel void painter ( __write_only image3d_t image, const int oneD, const int twoD ){
				//Get Current Global ID
                int pos = get_global_id(0);

                //Define Variables
				__private float val = 0.0f;
				__private float temp =0.0f;
				__private int normalizedVal=0;
		
                //Get postion in 3d space
				int x = (int) pos%oneD; 
                int y = (int) floor((float)pos/oneD);
				int z = (int) floor((float)pos/oneD);

                //RANGE[-16,129, 32,258]
                val = (x^2) + (y^2) - (z^2);
                temp = ((val+16129)/(32258+16129));

                //normalizes value between 0 and 255 
                normalizedVal = (int) floor(255*temp); 
                write_imagef (image, (int4)(x, y, z, 0), normalizedVal);
}

//Kernel to filter image using a box filter
__kernel void box_filter( __read_only image3d_t srcImg, __write_only image3d_t dstImg, __read_only imagefilter int stride, int width, int heigh){
					//Get Current Global and Local ID
					int globalPos = get_global_id(0);
					int globalPos = get_global_id(1);
					int globalPos = get_global_id(2);
					int localPos = get_local_id(0);					
						  
						  
						  }
__kernel void dct(){}
__kernel void idct(){}
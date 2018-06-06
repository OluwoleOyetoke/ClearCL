package clearcl.interview;

import clearcl.viewer.demo.ViewImageDemos;


/**
 * Contains main class to run the interview demo
 * @author Oluwole Oyetoke
 */
public class Runner {
    
    public static void main(String[] args) throws Exception, Throwable {
        //Run new ClearCl demos
        InterviewDemo clearCLInterviewDemo = new InterviewDemo(128, 128, 128);
        
        /*Q1 b&c. Tasks: (1) Hyperboloid paint, (2) show, (3) write to tiff (4) filter, (5) show again */
        clearCLInterviewDemo.hyperboloidPaint();
        
        /* Q2 a,b&c Tasks: (1) get and show 2D image (2) perform dct (3) show dct output (4) perform idct (5) show that image is the same */
        clearCLInterviewDemo.doDct2(); 
        
        /*Q1 a Task: (1) get code  (2) build project on netbeans (3) run demos*/
        ViewImageDemos clearCLDemo = new ViewImageDemos();
        clearCLDemo.demoViewImage2DF();
        
        //clearCLInterviewDemo.criclePaint(); //Circle Paint...to debug
        System.out.println("Main Done");
    }
}

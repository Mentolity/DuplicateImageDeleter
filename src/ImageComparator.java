import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;

//BST data structure?
//Calculate a checksum from the images signature and insert into tree?
//How to account for variance
public class ImageComparator {
	private int filesCompleted = 0;
	private int totalFiles;

	private int tolerance = 5;
	private int numOfSkipPixels = 20;
	
	private File directory;
	File[] files;
	private Stack<File> stackOfFiles = new Stack<File>();
	private Stack<File> initStackOfFiles = new Stack<File>();
	private Map<Integer, LinkedList<File>> processedImages = Collections.synchronizedMap(new HashMap<Integer, LinkedList<File>>(1000));
	
	GUI window = new GUI();
	
	private void addAllFilesToStack(File directory, boolean includeSubFolders){
		File[] files = directory.listFiles();
		
		for(File f:files){
			if(f.isFile()){
				stackOfFiles.add(f); //first stack for checking against Map processedImages
				initStackOfFiles.add(f); //second stack for building Map processedImages
			}else if(f.isDirectory() && includeSubFolders){
				addAllFilesToStack(f, includeSubFolders);
			}
		}
	}
	
	public ImageComparator(){
		while(!GUI.running()){
			try{
				Thread.sleep(100);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		
		tolerance = GUI.getTolerance();
		directory = new File(GUI.getDirectory());
		
		addAllFilesToStack(directory, GUI.getIncludeSubFoler());
		
		/*files = directory.listFiles();
		for(File f:files){
			if(f.isFile()){
				stackOfFiles.add(f); //first stack for checking against Map processedImages
				initStackOfFiles.add(f); //second stack for building Map processedImages
			}else if(f.isDirectory()){
				
			}
		}*/
		totalFiles = stackOfFiles.size();
		System.out.println("Files Found: " + totalFiles);
		
		SimilarImageFinder sif1 = new SimilarImageFinder();
		SimilarImageFinder sif2 = new SimilarImageFinder();
		SimilarImageFinder sif3 = new SimilarImageFinder();
		SimilarImageFinder sif4 = new SimilarImageFinder();
		//SimilarImageFinder sif5 = new SimilarImageFinder();
		//SimilarImageFinder sif6 = new SimilarImageFinder();
		//SimilarImageFinder sif7 = new SimilarImageFinder();
		//SimilarImageFinder sif8 = new SimilarImageFinder();
	}
	
	private class SimilarImageFinder implements Runnable{
		private BufferedImage currentImg;
		Thread t;
		
		public SimilarImageFinder(){
			this.start();
		}
		
		public void start(){
			if(t == null){
				t = new Thread (this);
				t.start();
			}
		}
		
		public void run(){
			//findSimilar();
			init();
			findAllRepeats();
		}
		
		public void init(){
			File currentFile = null;
			int currentKey = 0;
			
			long startTime = System.currentTimeMillis();
			while(!initStackOfFiles.empty()){
				currentFile = initStackOfFiles.pop();
				//System.out.println(currentFile);
				while(true){
					try{
						currentImg = ImageIO.read(currentFile);
						currentKey = calculateImageKey(currentImg);
						break;
					}catch(Exception e) {
						System.out.println("Help! : " + e + " : "+ currentFile);
						currentFile = initStackOfFiles.pop();
					}
				}
				
				if(processedImages.containsValue(currentKey)){
					processedImages.get(currentKey).add(currentFile);
				}else{
					processedImages.put(currentKey, new LinkedList<File>());
					processedImages.get(currentKey).add(currentFile);
				}
				filesCompleted++;
				System.out.println(currentFile + " : File #" + filesCompleted + " : Initialization : " + ((double)filesCompleted/totalFiles)*100);
				
			}
			
			long elapsedTime = System.currentTimeMillis() - startTime;
			System.out.println("FINISHED proceesedImages: " + (int)(elapsedTime/1000)/60 + " Minutes and " + (elapsedTime/1000)%60 + " Seconds");
			filesCompleted = 0;
			try{
				Thread.sleep(1000); //wait a second to make sure all threads have finished
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		
		private void findAllRepeats(){
			File currentFile = null;
			int currentKey = 0;
			
			long startTime = System.currentTimeMillis();
			//while theres still files on the stack pop one off
			while(!stackOfFiles.empty()){
				currentFile = stackOfFiles.pop();
				//open the current FIle to an image and calculate its key. If and error is thrown move to the next file on the stack.
				while(true){
					try{
						currentImg = ImageIO.read(currentFile);
						currentKey = calculateImageKey(currentImg);
						//System.out.println(currentFile + " : " + calculateImageKey(currentImg));
						break;
					}catch(Exception e) {
						System.out.println("Help! : " + e + " : "+ currentFile);		
						currentFile = stackOfFiles.pop();
					}
				}
				//calculate the range of possible key matches for a 5% tolerance on the key variance
				//get the linkedlist at for each key within the range of possible key matches and check if there if any image matches currentImg
				//if any matches outputResults
				int possibleKeyMatchRange = (int)(currentKey*tolerance/100);
				
				for(int i = currentKey-possibleKeyMatchRange; i<(currentKey+possibleKeyMatchRange); i++){
					if(processedImages.keySet().contains(i)){
						BufferedImage currentCompareImage;
						double totalPercentage;
						
						for(File f : processedImages.get(i)){
							try{
								if(currentFile == f)
									break;
								currentCompareImage = ImageIO.read(f);
								totalPercentage = percentSimilar(currentImg, currentCompareImage, numOfSkipPixels, tolerance);
								outputResults(totalPercentage, f, currentFile);
							}catch(Exception e) {
								System.out.println("Help! " + f);		
							}
						} 
					}
				}
				filesCompleted++;
			}
			
			long elapsedTime = System.currentTimeMillis() - startTime;
			System.out.println((int)(elapsedTime/1000)/60 + " Minutes and " + (elapsedTime/1000)%60 + " Seconds"); 
		}
		
		
		private void outputResults(double totalPercentage, File currentFile){
			outputResults(totalPercentage, currentFile, null);
		}
		
		private void outputResults(double totalPercentage, File currentFile, File otherFile){
			if(totalPercentage < tolerance){
				System.out.println(currentFile + " : File #" + filesCompleted + " : YES : MATCH : " + totalPercentage + " : PERCENT : " + ((double)filesCompleted/totalFiles)*100);
				BufferedImage img = null;
				try{
					img = ImageIO.read(currentFile);
				}catch(IOException e){
					e.printStackTrace();
				}
				GUI.addMatchFrame(currentFile, otherFile);
				
			}else{
				System.out.println(currentFile + " : File #" + filesCompleted +  " : NO : MATCH : " + totalPercentage + " : PERCENT : " + ((double)filesCompleted/totalFiles)*100);
			}
		}
		
		private int calculateImageKey(BufferedImage img){
			int c;
			double r, g, b, redBucket = 0, blueBucket = 0, greenBucket = 0, key;
			
			for(int y=0; y<img.getHeight(); y+=numOfSkipPixels){
				for(int x=0; x<img.getWidth(); x+=numOfSkipPixels){
					c = img.getRGB(x,y);
					
					b = c & 0xff;
					g = (c & 0xff00) >> 8;
					r = (c & 0xff0000) >> 16;

					blueBucket += b;
					greenBucket += g;
					redBucket += r;
				}
			}
			
			key = (blueBucket + redBucket + greenBucket)/(img.getWidth()*img.getHeight()/(numOfSkipPixels*numOfSkipPixels));
			
			return (int)key;
			
		}
		
		private double percentSimilar(BufferedImage img1, BufferedImage img2, int numOfSkipPixels, int tolerance){
			double totalPercentage = 0;
			int img1Width = img1.getWidth();
			int img1Height = img1.getHeight();
			int img2Width = img2.getWidth();
			int img2Height = img2.getHeight();
			
			outerloop:
			for(int y=0; y<img1Height; y+=numOfSkipPixels){
				for(int x=0; x<img1Width; x+=numOfSkipPixels){
					totalPercentage+=similarTo(img1.getRGB(x,y), img2.getRGB((int)(x*((double)img2Width/img1Width)),(int)(y*((double)img2Height/img1Height))));
					
					if(totalPercentage/(img1Width*img1Height/(numOfSkipPixels*numOfSkipPixels)) > tolerance)
						break outerloop;
				}
			}
			return totalPercentage/(img1Width*img1Height/(numOfSkipPixels*numOfSkipPixels));	
		}
		
		//Unused method
		//Can be used if you want to search 1 image against an entire folder
		private void findSimilar(BufferedImage img){
			System.out.println(totalFiles);
			File currentFile;
			
			long startTime = System.currentTimeMillis();
			while(!stackOfFiles.empty()){
				currentFile = stackOfFiles.pop();
				while(true){
					try{
						currentImg = ImageIO.read(currentFile);
						break;
					}catch(NullPointerException | IOException | ArrayIndexOutOfBoundsException e) {
						System.out.println("Help! " + currentFile);		
						currentFile = stackOfFiles.pop();
						filesCompleted++;
					}
				}
				
				double totalPercentage = percentSimilar(img, currentImg, numOfSkipPixels, tolerance);
				
				filesCompleted++;
				outputResults(totalPercentage, currentFile);
				
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			System.out.println((int)(elapsedTime/1000)/60 + " Minutes and " + (elapsedTime/1000)%60 + " Seconds");
		}
		
		public double similarTo(int c1, int c2){
			double p1, p2, p3;
			
			double b1 = c1 & 0xff;
			double g1 = (c1 & 0xff00) >> 8;
			double r1 = (c1 & 0xff0000) >> 16;

			p1 = ((b1/255)*100);
			p2 = ((g1/255)*100);
			p3 = ((r1/255)*100);
			
			double perc1 = ((p1+p2+p3)/3);
			
		
			double b2 = c2 & 0xff;
			double g2 = (c2 & 0xff00) >> 8;
			double r2 = (c2 & 0xff0000) >> 16;
			
			p1 = ((b2/255)*100);
			p2 = ((g2/255)*100);
			p3 = ((r2/255)*100);
			
			double perc2 = ((p1+p2+p3)/3);
			
			//System.out.println(perc1 - perc2);
			return Math.abs(perc1 - perc2);
		}
	}
}

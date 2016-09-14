import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import java.awt.Font;

import javax.swing.JSpinner;
import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;


public class GUI{
	JFrame frame = new JFrame("Duplicate Image Finder");
	static ArrayList<MatchPanel> matches = new ArrayList<MatchPanel>();
	static JScrollPane upperMatchScrollPane = new JScrollPane();
	static JScrollPane lowerOutputScrollPane = new JScrollPane();
	
	static JButton btnStart;
	static JTextPane directoryInputPane;
	static JLabel labelDirectory;
	static JLabel labelTolerance;
	static JSpinner toleranceSpinner;
	
	static JLabel lblIncludeSubfolders;
	static JCheckBox checkBoxIncludeSubFolder;
	static JCheckBox checkBoxAutoScroll;
	
	private static Stack<Stack<MatchPanel>> undoStack = new Stack<Stack<MatchPanel>>();
	static boolean autoScroll = true;	
	static boolean running = false;
	private static File deletedFileDirectory;

	public GUI() {
		//create temporary directory for deleted files
		try{
			deletedFileDirectory = File.createTempFile("deletedFiles","");
		}catch(IOException e) {
			e.printStackTrace();
		}
		deletedFileDirectory.delete();
		deletedFileDirectory.mkdir();
		//init
		initialize();
	}
	
	public static boolean running(){
		return running;
	}
	
	public static int getNumberOfMatches(){
		return matches.size();
	}
	
	private void initialize() {
		frame.setBounds(100, 100, 929, 850);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);
		
		upperMatchScrollPane.setBounds(10, 61, 900, 384-50);
		frame.getContentPane().add(upperMatchScrollPane);
		
		lowerOutputScrollPane.setBounds(10, 406, 900, 384);
		frame.getContentPane().add(lowerOutputScrollPane);
		
			
		JTextArea textArea = new JTextArea(50, 10);
		PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));
		System.setOut(printStream);
		System.setErr(printStream);
		lowerOutputScrollPane.setViewportView(textArea);	
		
		
		btnStart = new JButton("START");
		btnStart.setBounds(758, 11, 152, 39);
		frame.getContentPane().add(btnStart);
		btnStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                System.out.println("START");
                running = true;
            }
        });
		
		
		directoryInputPane = new JTextPane();
		directoryInputPane.setToolTipText("Directory in the form:\r\n\"C:/Users/Admin/Photos\"");
		directoryInputPane.setBounds(10, 30, 444, 20);
		frame.getContentPane().add(directoryInputPane);
		
		labelDirectory = new JLabel("Directory:");
		labelDirectory.setFont(new Font("Tahoma", Font.BOLD, 16));
		labelDirectory.setBounds(10, 5, 94, 14);
		frame.getContentPane().add(labelDirectory );
		
		labelTolerance = new JLabel("Tolerance: ");
		labelTolerance.setFont(new Font("Tahoma", Font.BOLD, 16));
		labelTolerance.setBounds(465, 7, 94, 14);
		frame.getContentPane().add(labelTolerance);
		
		toleranceSpinner = new JSpinner();
		toleranceSpinner.setModel(new SpinnerNumberModel(5, 0, 100, 1));
		toleranceSpinner.setToolTipText("Tolerance of for matching.\r\nE.g. 5% tolerance means if the image differs by my than 5% than it doesn't match.");
		toleranceSpinner.setBounds(509, 30, 50, 20);
		frame.getContentPane().add(toleranceSpinner);
		
		lblIncludeSubfolders = new JLabel("Include Subfolders: ");
		lblIncludeSubfolders.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblIncludeSubfolders.setBounds(569, 5, 162, 14);
		frame.getContentPane().add(lblIncludeSubfolders);
		
		checkBoxIncludeSubFolder = new JCheckBox("");
		checkBoxIncludeSubFolder.setSelected(true);
		checkBoxIncludeSubFolder.setBounds(710, 30, 21, 23);
		frame.getContentPane().add(checkBoxIncludeSubFolder);
		
		checkBoxAutoScroll = new JCheckBox("AutoScroll");
		checkBoxAutoScroll.setSelected(true);
		checkBoxAutoScroll.setBounds(10, 792, 97, 23);
		checkBoxAutoScroll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                autoScroll = !autoScroll;
            }
        });
		frame.getContentPane().add(checkBoxAutoScroll);
		frame.setVisible(true);
	}
	
	public static int getTolerance(){
		return (int)toleranceSpinner.getValue();
	}
	
	public static String getDirectory(){
		return directoryInputPane.getText();
	}
	
	public static boolean getIncludeSubFoler(){
		return checkBoxIncludeSubFolder.isSelected();
	}
	
	public static void addMatchFrame(File file1, File file2){
		
		matches.add(new MatchPanel(file1, file2));
		upperMatchScrollPane.setViewportView(matches.get(0));
	}
	
	
	private static class MatchPanel extends JPanel {
		private int width = 320;
		private int height = 300;
		
		File file1;
		File file2;
		
		JLabel imageLabel1;
		JLabel imageLabel2;
		
		JButton btnMatch;
		JButton btnNotMatch;
		JButton btnUndo;
		
		public MatchPanel(File file1, File file2) {
			BufferedImage img1 = null, img2 = null;
			this.file1 = file1;
			this.file2 = file2;
			
			
			try{
				img1 = ImageIO.read(file1);			
				img2 = ImageIO.read(file2);
			}catch(IOException e){
				e.printStackTrace();
			}
			
			
			int img1Width = img1.getWidth();
			int img1Height = img1.getHeight();
			
			int img2Width = img2.getWidth();
			int img2Height = img2.getHeight();
			
			if(img1Width > width || img1Height > height){
				if(img1Width > img1Height){
					img1 = toBufferedImage(img1.getScaledInstance(width, (int)(height*((double)img1Height/img1Width)), Image.SCALE_SMOOTH));
				}else{
					img1 = toBufferedImage(img1.getScaledInstance((int)(width*((double)img1Width/img1Height)), height, Image.SCALE_SMOOTH));
				}
			}
			
			if(img2Width > width || img2Height > height){
				if(img2Width > img2Height){
					img2 = toBufferedImage(img2.getScaledInstance(width, (int)(height*((double)img2Height/img2Width)), Image.SCALE_SMOOTH));
				}else{
					img2 = toBufferedImage(img2.getScaledInstance((int)(width*((double)img2Width/img2Height)), height, Image.SCALE_SMOOTH));
				}
			}
			
			
			setLayout(null);
			
			imageLabel1 = new JLabel(new ImageIcon(img1));
			imageLabel1.setBounds(27, 38, width, height);
			add(imageLabel1);
			
			imageLabel2 = new JLabel(new ImageIcon(img2));
			imageLabel2.setBounds(376, 38, width, height);
			add(imageLabel2);
			
			btnMatch = new JButton("Match");
			btnMatch.setToolTipText("Delete the image with the lower resolution.");
			btnMatch.setBounds(743, 38, 100, 50);
			add(btnMatch);
			
			btnNotMatch = new JButton("Not Match");
			btnNotMatch.setToolTipText("Remove potential match.");
			btnNotMatch.setBounds(743, 141, 100, 50);
			add(btnNotMatch);
			
			btnUndo = new JButton("Undo");
			btnUndo.setToolTipText("Undo last match choice.");
			btnUndo.setBounds(743, 244, 100, 50);
			add(btnUndo);
			
			btnMatch.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e){
	                System.out.println("You clicked Match");
	                Stack<MatchPanel> deletedPanels = new Stack<MatchPanel>();
	                
	                if(matches.size() > 1){
	                	deletedPanels.add(matches.remove(0));
	                	
	                	if(img1Width*img1Height > img2Width*img2Height){
	                		deleteFile(file2);
	                	}else{
	                		deleteFile(file1);
	                	}
	                	
	                	while(!matches.get(0).getFile1().exists() || !matches.get(0).getFile2().exists()){
	                		deletedPanels.add(matches.remove(0));
	                	}
	                	undoStack.add(deletedPanels);
	                	upperMatchScrollPane.setViewportView(matches.get(0));
	                	
	                }else if(matches.size() == 1){
	                	deletedPanels.add(matches.remove(0));
	                	upperMatchScrollPane.setViewportView(null);
	                	if(img1Width*img1Height > img2Width*img2Height){
	                		deleteFile(file2);
	                	}else{
	                		deleteFile(file1);
	                	}
	                }
	            }
	        });
			
			
			btnNotMatch.addActionListener(new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	                Stack<MatchPanel> deletedPanels = new Stack<MatchPanel>();
	                System.out.println("You clicked NotMatch");
	                if(matches.size() > 1){
	                	deletedPanels.add(matches.remove(0));
	                	upperMatchScrollPane.setViewportView(matches.get(0));
	                }else if(matches.size() == 1){
	                	deletedPanels.add(matches.remove(0));
	                	upperMatchScrollPane.setViewportView(null);
	                }
	            }
	        }); 
			
			btnUndo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e){
					if(undoStack.empty())
						return;
					for(MatchPanel mp : undoStack.pop()){
						File[] files = deletedFileDirectory.listFiles();
						for(File f:files){
							if(f.getName().equals(mp.getFile1().getName())){
								f.renameTo(new File(getDirectory() + "/" + f.getName()));
							}else if(f.getName().equals(mp.getFile2().getName())){
								f.renameTo(new File(getDirectory() + "/" + f.getName()));
							}
						}
						matches.add(0, mp);
						upperMatchScrollPane.setViewportView(matches.get(0));
					}	
				}
			});
		}
		
		public File getFile1(){
			return file1;
		}
		
		public File getFile2(){
			return file2;
		}
	}
	
	private static void deleteFile(File f){
		f.renameTo(new File(deletedFileDirectory + "/" +  f.getName())); //moves file to temp folder
        System.out.println("DELETED: " + f.getName());
	}
	
	
	//converts an Image to a BufferedImage
	private static BufferedImage toBufferedImage(Image img){
		if (img instanceof BufferedImage){
	        return (BufferedImage) img;
	    }
	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    // Return the buffered image
	    return bimage;
	}
	
	private class CustomOutputStream extends OutputStream {
	    private JTextArea textArea;
	     
	    public CustomOutputStream(JTextArea textArea) {
	        this.textArea = textArea;
	    }
	     
	    @Override
	    public void write(int b) throws IOException {
	        // redirects data to the text area
	        textArea.append(String.valueOf((char)b));
	        // scrolls the text area to the end of data
	        if(autoScroll)
	        	textArea.setCaretPosition(textArea.getDocument().getLength());
	    }
	}
}

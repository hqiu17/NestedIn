package progress;

import java.util.stream.*;
import java.lang.Thread;

/**
 * 
 * 
 */
public class Bar {
	//private double size = new Double(0);
	private int size = 0;
	private double resolution = 0.025;
	private int record = -1;
	private String mark = "-";
	private String fullBar = "";
	private int fullBarLen = 0;
	
	/**
	 * Constructors 
	 * mandatory input: data size;
	 * optional inputs: percentage resolution (how frequent should progress bar be updated.
	 *                      the default is to update progress bar every 2.5% percent. 
	 *                      That is 50 update in total);
	 *                  mark (building block of progress bar, the default is '-'); 
	 */
	public Bar(int size, double resolution, String mark) {
		this.size = size;
		this.resolution = resolution;
		this.mark = mark;
		setTotalHeight(resolution);
	}	
	public Bar(int size, double resolution) {
		this.size = size;		
		this.resolution = resolution;
		setTotalHeight(resolution);
	}
	public Bar(int size) {
		this.size = size;
		setTotalHeight(resolution);
	}
	
	/**
	 * Given an integer 'count', if it triggers progress bar growth, refresh current 
	 * line in terminal console and redraw progress bar
	 * @param count, raw count of how many jobs are done
	 */
	public void grow(double count) {
		/* calculate percentage. */
		double percentage = count/size;
		/* Turn percentage to zero if percentage is too small. */
		if (percentage < resolution ) percentage = 0;
		/* translate percentage into bar length. */
		int height = (int)(percentage/resolution);
		
		/**
		 * Overlay progress bar onto blank background and print progress bar only
		 * when count triggers bar growth 
		 */
		if (height > record ) {
			String sRepeated = IntStream.range(0, height).mapToObj(i -> mark).collect(Collectors.joining(""));
			sRepeated = sRepeated + '>';
			sRepeated = (sRepeated + fullBar).substring(0, fullBarLen);
			String percentage3decimal = alignDoubleToLeft(percentage, 5);
			System.out.print(sRepeated + percentage3decimal + "\r");		
			record = height;
		}
	}
	
	/**
	 * Calculate overall integer bar height fullBarLen based on percentage resolution
	 * Make a transparent blank fullBar    
	 * @param resolution
	 */
	private void setTotalHeight(double resolution) {
		fullBarLen = (int)(1.0/resolution + 1);
		fullBar    = IntStream.range(0,  fullBarLen).mapToObj(i -> " ").collect(Collectors.joining(""));
	}
	
	/**
	 * Given a double, return a string of it in specified length
	 * @param percentage double
	 * @param length, total length (in character) of string to be returned
	 * @return string of percentage in specified characters (e.g., '0.5000')
	 */
	public String alignDoubleToLeft(double d, int length) {
		String ds = String.valueOf(d);
		String background = IntStream.range(0, length).mapToObj(i -> "0").collect(Collectors.joining(""));
		ds = (ds + background).substring(0, length);
		return ds;
	}

	
	public static void main(String[] args) {
		/** define a Bar object before loop */
		Bar my = new Bar(100, 0.03, "-");
		/** a simple loop */
		for (int i=0; i<=100; i++) {
			// feed job number to Bar object. Progress bar grows if job number is sufficient
			my.grow(i);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

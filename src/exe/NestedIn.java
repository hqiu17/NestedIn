/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package exe;

/**
 * The class NestedIn scans a directory of tree files for user
 * specified pattern.
 *
 * @author  Huan Qiu
 * @version 3
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import progress.Bar;


public class NestedIn {

	private String indir     ="";
	private String donor     ="";
	private double cut       = 0;
	private String optionals ="";
	private String ignored   ="";
	private String outHGT    ="";
	private String outDir    ="";
	private boolean getInGroup = false;
	private int    minStrongNode  = 1;
	private int    minAllNode     = 2;
	private int    minOutGroupSize = 0;
	private int    thread         = 1;
	private String basicCmd  = "java -jar NestedIn.jar -dir mydirectory -don mydonor ...";
	private String version   = "NestedIn (v2.1)";
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		
		NestedIn myParser = new NestedIn();
		
		/** parse input arguments */ 
		myParser.parseArgumentInputs(args);
		
		/** prepare output file name and setup output directory */
		myParser.setOutputFileAandDirectory();
		
		/** print out mandatory parameters onto console */
		System.out.println("direcotry: " + myParser.indir);
		System.out.println("donor(s): " + myParser.donor);
		System.out.println("branch support cutoff: " + myParser.cut);
		
		/** print out optional parameters onto console */
		if (myParser.optionals.length()>0) System.out.println("optional taxa: " + myParser.optionals);
		if (myParser.ignored.length()>0)   System.out.println("ignored taxa:  " + myParser.ignored);
		if (myParser.minStrongNode>1)      System.out.println("strong node number cutoff: " + Integer.toString(myParser.minStrongNode));
		if (myParser.minStrongNode>2) {
			if (myParser.minStrongNode>1) {
				System.out.println("all node number cutoff   : " + Integer.toString(myParser.minAllNode));
			} else {
				System.out.println("all node number cutoff   : " + Integer.toString(myParser.minAllNode));
			}
		}
		if (myParser.minOutGroupSize != 0){
			System.out.println("outgroup size cutoff: " + Integer.toString(myParser.minOutGroupSize));
		}
		
		/** 
		 * Process an input directory and collect coded supporting node information  
		 * and write to output file
		 */
		ArrayList<String> nbNodesCoded = new ArrayList<String>();
		nbNodesCoded = myParser.Adir();
		
		try {
			FileWriter hgtWriter = new FileWriter(myParser.outHGT);
			hgtWriter.write("Gene\tNum.Node(support>="+Double.toString(myParser.cut)+")"+"\tNum.Node(support<"+Double.toString(myParser.cut)+")\tTotal\n");
			for (String l : nbNodesCoded) {
				hgtWriter.write(l+"\n");
			}
			hgtWriter.close();			
		} catch (IOException e) {
			System.out.println("#-> errorous writting to file: " + myParser.outHGT);
		}
		
		/** print out job run time */
		long endTime = System.currentTimeMillis();
		System.out.println("take " + (endTime - startTime)/1000 + " seconds.");
		System.out.println( String.valueOf( nbNodesCoded.size() ) + " trees meet user criteria.");
	}

	/** 
	 * Take the mandatory argument indir as an input and test monophyly for each tree inside of it	
	 * @return a list of qualifying tree and the supporting node information
	 */
	public ArrayList<String> Adir () {
		ArrayList<String> results = new ArrayList<String>();
		
		/** setup directory path, create an empty list to hold output and initiate progress bar*/
		Path dp = Paths.get(indir);
		List<Path> files = new ArrayList<Path>();
		Bar progress = new Bar (200);
		int size = 100;
		
		/** read input directory, record tree list and set sample size for progress bar*/
		try{
			files = Files.walk(dp,1)
					.skip(1)
					.filter(x->x.toString().matches("(.+)tree|(.+)tre"))
					.filter(x->Files.isRegularFile(x))
					.collect(Collectors.toList());
			size = files.size();
			progress = new Bar (size);
		}
		catch (IOException e) {
			System.out.println("#1-> erronreous reading directory: " + indir);
		}
	
		/** create thread-safe class to count finished tree*/
		CountSyn count = new CountSyn();
		
		/** create and launch tasks in parallel*/
		ArrayList<Task> tasks = new ArrayList<Task>(); 
		for (int i=1; i<=thread; i++) {
			Task task = new Task(files, count, donor, cut, optionals, ignored, 
					        minStrongNode, minAllNode, results, outDir, getInGroup, minOutGroupSize, progress);
			tasks.add(task);
			task.start();
		}
		
		/** wait for the join of all task threads*/
		for (Task task: tasks) {
			try {
				task.join();
			}catch(Exception e){
				System.out.println("task join error " + e);	
			}
		}
		
		return results;
	}

	/**
	 * Parse command line input arguments
	 * @param args command line arguments
	 */
	private void parseArgumentInputs(String[] args){
		CommandLineParser cparser = new DefaultParser();
		
		/** setup parameters */
		Options coptions = new Options();
		
		coptions.addOption("h"  , "help"       , false, "print usage instruction");
		coptions.addOption("v"  , "version"    , false, "print version number");
		
		coptions.addOption("dir", "directory"  , true,  "input Directory containing newick trees");
		coptions.addOption("don", "donor"      , true,  "Donor(s); separate multiple donors with comma");

		coptions.addOption("cut", "cutoff"     , true,  "node support Cutoff (default=0)");		
		coptions.addOption("out", "output"     , true,  "specify suffix for Output directory and files");		

		coptions.addOption("opt", "optional"   , true,  "Optional taxa allowed in monophyletic ingroup");
		coptions.addOption("ign", "ignore"     , true,  "taxa to be Ignored while screening trees");
		coptions.addOption("igp", "ingroup"    , false, "export details of monophyletic Ingroups");
		
		coptions.addOption("ssn", "ssnode"     , true,  "minimal Strongly Supported Nodes uniting query and donors (default=1)");
		coptions.addOption("asn", "asnode"     , true,  "minimal number of All Supporting Nodes uniting query and donors (default=2)");
		coptions.addOption("ogs", "outgroupsize", true,  "minimal OutGroup Size for a tree to be considered valid (default=5)");
		
		coptions.addOption("thd", "thread"     , true,  "number of threads to use (default=1)");
		
		HelpFormatter formatter = new HelpFormatter();
		/** add null comparator so options are sorted in original order */
		formatter.setOptionComparator(null);
		
		/** parse command line arguments */
		try{
			CommandLine line = cparser.parse(coptions, args);
			
			if (args.length == 0 | line.hasOption("help")) {
				formatter.printHelp( basicCmd, coptions );
				System.exit(0);
			}
			if (line.hasOption("version")) {
				System.out.println(version);
				System.exit(0);
			}
			
			if (line.hasOption("directory")) indir         = line.getOptionValue("directory");
			if (line.hasOption("output"))    outHGT        = line.getOptionValue("output");
			if (line.hasOption("donor"))     donor         = line.getOptionValue("donor");
			if (line.hasOption("optional"))  optionals     = line.getOptionValue("optional");
			if (line.hasOption("ignore"))    ignored       = line.getOptionValue("ignore");
			if (line.hasOption("cutoff"))    cut           = Double.parseDouble(line.getOptionValue("cutoff")) ;
			if (line.hasOption("ssnode"))    minStrongNode = Integer.parseInt(line.getOptionValue("ssnode"));
			if (line.hasOption("asnode"))    minAllNode    = Integer.parseInt(line.getOptionValue("asnode"));
			if (line.hasOption("ingroup"))   getInGroup    = true;
			if (line.hasOption("outgroupsize"))  minOutGroupSize = Integer.parseInt(line.getOptionValue("outgroupsize"));
			if (line.hasOption("thread"))    thread        = Integer.parseInt(line.getOptionValue("thread"));
		}
		catch( ParseException exp) {
			System.out.println( "Unexpected exception: " + exp.getMessage());
			System.out.println("");
			formatter.printHelp( basicCmd, coptions );
			System.exit(1);
		}
		
		/** quit if no input directory is provided */
		if (indir.isEmpty()) {
			System.out.println("Warning: no input directory is specified");
			System.exit(1);
		}
		/** quit if no donor taxa are provided */
		if (this.donor.isEmpty()) {
			System.out.println("Warning: no donor(s) is specified");
			System.exit(1);
		}

	}
	
	/** Make output directory based on input arguments */
	private void setOutputFileAandDirectory() {		
		/** if outHGT is not specified, make output directory based on input arguments */
		if (outHGT.isEmpty()) {
			outHGT = indir;
			if (outHGT.endsWith("/")) outHGT = outHGT.substring(0, outHGT.length()-1);
			outHGT = outHGT + ".From"+donor + "_Cut"+cut;
			if ( ! optionals.isEmpty()) outHGT = outHGT + "_With" + optionals;
			if ( ! ignored.isEmpty())   outHGT = outHGT + "_Ign" + ignored;
			if (minStrongNode>1) {
				outHGT = outHGT + "_Ssn" + Integer.toString(minStrongNode);
				if (minAllNode>2) outHGT = outHGT + "Asn" + Integer.toString(minAllNode);
			}else {
				if (minAllNode>2) outHGT = outHGT + "_Asn" + Integer.toString(minAllNode);
			}
			if (minOutGroupSize>0) outHGT = outHGT + "_OutGrpSz" + Integer.toString(minOutGroupSize);
			
			outHGT = outHGT.replaceAll(",", "");
		}
		/** create output directory and figure out out-file */
		outDir = outHGT + ".trees";
		new File(outDir).mkdirs();
		outHGT = outHGT + ".candidates.txt";
	}
}

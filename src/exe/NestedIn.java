package exe;

import java.io.BufferedReader;
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

import newicktree.NewickTree;
import newicktree.TestMonophyly;
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
	private String basicCmd  = "java -jar NestedIn.jar -dir mydirectory -don mydonor ...";
	private String version   = "NestedIn (v01)";
	
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		
		NestedIn myParser = new NestedIn();
		
		/**  
		 * command line input for test purpose
		 */
		//args = new String[]{"-dir", "/Users/Qcell/eclipse-workspace/a06.symmi.txt.tres/", 
		//		"-don", "Bacteria,Archaea", "-cut", "85", "-opt","Alveolata", "-ign", "Cladosiphon,Hydra", //}; //,
		//		"-out", "../ax06"};

		/** parse input arguments */ 
		myParser.parseArgumentInputs(args);
		
		/** prepare output file name and setup output directory */
		myParser.setOutputFileAandDirectory();
		
		/** print out mandatory parameters onto console */
		System.out.println("direcotry:     " + myParser.indir);
		System.out.println("donor(s):      " + myParser.donor);
		System.out.println("cut-off:       " + myParser.cut);
		
		/** print out optional parameters onto console */
		if (myParser.optionals.length()>0) System.out.println("optional taxa: " + myParser.optionals);
		if (myParser.ignored.length()>0)   System.out.println("ignored taxa:  " + myParser.ignored);
		if (myParser.minStrongNode>1)      System.out.println("strong node number cutoff: " + Integer.toString(myParser.minStrongNode));
		if (myParser.minStrongNode>2) {
			if (myParser.minStrongNode>1) {
				System.out.println("all node number cutoff   : " + Integer.toString(myParser.minAllNode));
			} else {
				System.out.println("all node number cutoff: " + Integer.toString(myParser.minAllNode));
			}
		}
		
		/** 
		 * Go through all trees in the input directory and collect coded supporting node information  
		 * and write to output file
		 */
		ArrayList<String> nbNodesCoded = new ArrayList<String>();
		nbNodesCoded = myParser.Adir(myParser.indir, myParser.donor, myParser.cut, myParser.optionals, myParser.ignored);
		
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
	
		/* !!! hgtWriter::write woudn't work in pipe
		try {
			FileWriter hgtWriter = new FileWriter(outHGT);
			nbNodesCoded.stream().forEach(hgtWriter::write);
			hgtWriter.close();
		} catch (IOException e) {
			//System.out.println("#-> erronreous reading directory: ");
		}
		*/
		
		long endTime = System.currentTimeMillis();
		System.out.println("take " + (endTime - startTime)/1000 + " seconds.");
		System.out.println( String.valueOf( nbNodesCoded.size() ) + " trees meet user criteria.");
	}

	/** 
	 * Take a directory as input and test monophyly for each tree	
	 * @param dir
	 * @param donor
	 * @param cut
	 * @param optionals
	 * @param ignored
	 * @return a list of qualifying tree and the supporting node information
	 */
	public ArrayList<String> Adir (String dir, String donor, double cut, String optionals, String ignored) {
		ArrayList<String> results = new ArrayList<String>();
		
		/** setup directory path, create an empty list to hold output and initiate progress bar*/
		Path dp = Paths.get(dir);
		List<Path> files = new ArrayList<Path>();
		Bar progress = new Bar (200);
		
		/** read input directory, record tree list and set sample size for progress bar*/
		try{
			files = Files.walk(dp,1)
					.skip(1)
					.filter(x->x.toString().matches("(.+)tree|(.+)tre"))
					.filter(x->Files.isRegularFile(x))
					.collect(Collectors.toList());
			int size = files.size();
			progress = new Bar (size);
		}
		catch (IOException e) {
			System.out.println("#1-> erronreous reading directory: " + dir);
		}
		
		/** loop through all trees and collect result*/
		int count=1;
		for ( Path tree : files ) {
			String code = Atree(tree.toString(), donor, cut, optionals, ignored);
			if (!code.isEmpty()) results.add(code);
			progress.grow(count);
			count++;
		}
		
		return results;
	}
	
	/** 
	 * examine a single tree. If input tree meets criteria, 
	 * 1) write the tree the output directory
	 * 2) return a string encoding node information: "query \t strong nodes \t weak nodes \t all nodes"
	 * if input tree fails, return an empty string
	 * @param intree
	 * @param donor
	 * @param cut
	 * @param optionals
	 * @param ignored
	 * @return string
	 */
	public String Atree (String intree, String donor, double cut, String optionals, String ignored) {

		String query = getQuery(intree);
		if (query.isEmpty()) return "";
		String filename = intree.substring(intree.lastIndexOf("/")+1);
	
		// read input tree file (get the first line actually)
		Path fp = Paths.get(intree);
		String line = new String("");
		try {
			BufferedReader reader = Files.newBufferedReader(fp);
			while (true) {
				line = reader.readLine();
				break;
			}
			reader.close();
		}
		catch(IOException e){
			System.out.println("#-> erronreous reading file: " + intree);
		}
		
		
		if (line == null) return "";
		
		// create NewickTree object and launch decomposition
		NewickTree tree = new NewickTree(line);
		List<String> bp = tree.getBipartitions(query);
		TestMonophyly test = new TestMonophyly(bp, query, donor, cut, optionals, ignored);
		test.testExclusive();
		//test.testGeneralized();		
		
		/* to be updated in future version: 
		 * write bipartitions to output file
		 * 
		String outfile = intree + ".table.txt";
		try{
			FileWriter writer = new FileWriter(outfile);
			for (String i : bp) {
				writer.write(i+"\n");
			}
			writer.close();
		}
		catch(IOException e){
			System.out.println("#-> erroreus writing file: " + outfile);
		}
		*/
		

		/** get destine of the input tree */
		int myFate = fate(test.getStrongNodes(), test.getWeakNodes(), test.getAdjustedStrongNodes());
		
		/** if input tree meet criteria, do the following */
		if (myFate > 1 ) {
			/** 1) write input tree to output directory */
			String outputrees = outDir + "/" + filename;
			try{
				FileWriter writer = new FileWriter(outputrees);
				writer.write(line + "\n");
				writer.close();
			}
			catch(IOException e){
				System.out.println("#-> errorous writing tree file: " + outputrees);
			}
			
			/** 2) if requested, write ingroup details to output directory */
			if (getInGroup) {
				String outputInGroupSeqs = outDir + "/" + filename + ".igs.txt";
				try {
					FileWriter writer = new FileWriter(outputInGroupSeqs);
					for (String seqs : test.getSupportDonorsAndOptionals()) {
						writer.write(seqs + "\n");
					}
					writer.close();
				}
				catch(IOException e) {
					System.out.println("#-> errorous file writing to: " + outputInGroupSeqs);
				}
			}
			
			/** 3) make coded node information and return */
			String outcome = query +"\t"+ 
							 test.getStrongNodes() +"\t"+ 
							 test.getWeakNodes() + "\t" + 
					         (test.getStrongNodes() + test.getWeakNodes())
					         ;
			return outcome;
		}
		
		return "";
	}
	public String Atree (String intree, String donor, double cut, String optionals) {
		String ignored = new String();
		return Atree(intree, donor, cut, optionals, ignored);
	}
	public String Atree (String intree, String donor, double cut) {
		String optionals = new String();
		String ignored = new String();
		return Atree(intree, donor, cut, optionals, ignored);
	}
	public String Atree (Path intree, String donor, double cut, String optionals, String ignored) {
		String str = intree.toString();
		return Atree(str, donor, cut, optionals, ignored);
	}
	public String Atree (Path intree, String donor, double cut, String optionals) {
		String str = intree.toString();
		return Atree(str, donor, cut, optionals);
	}
	public String Atree (Path intree, String donor, double cut) {
		String str = intree.toString();
		return Atree(str, donor, cut);
	}
		
	private int fate (int strong, int weak, int fixed) {
		int mystrong = strong + fixed;
		int myall    = strong + weak + fixed;
		if (mystrong > minStrongNode) {
			return 9;        // strong support
		}else if (mystrong == minStrongNode && myall >= minAllNode) {
			return 2;        // ok support
		}else if (mystrong ==1) {
			return 1;        // weak support
		}else {
			return 0;        // non-monophyletic support
		}
	}
	
	
	// figure out query name from the file name using "_2refseq" as marker
	// ??? to be thrown here
	public String getQuerySpecial(String input, String mark) {
		String query = null;
		int index_ending = 0;
		int index_leading = input.lastIndexOf("/");
		
		if (index_leading != -1) {
			index_ending = input.indexOf(mark, index_leading);
		} else {
			index_ending = input.indexOf(mark);
		}
		
		if (index_ending != -1) {
			query = input.substring(index_leading+1,index_ending);
		} 
		return query;
	}
	
	public String getQuery(String input) {
		String query = null;
		query = getQuerySpecial(input, "_2refseq");
		if (query == null) query = getQuerySpecial(input, ".contre");
		if (query == null) query = getQuerySpecial(input, ".tre");
		if (query == null) query = getQuerySpecial(input, ".");
		return query;
	}
	
	/**
	 * parse command line input arguments
	 * @param args
	 */
	private void parseArgumentInputs(String[] args){
		CommandLineParser cparser = new DefaultParser();
		
		/** setup parameters */
		Options coptions = new Options();
		
		coptions.addOption("h"  , "help"       , false, "print usage instruction");
		coptions.addOption("v"  , "version"    , false, "print version number");
		
		coptions.addOption("dir", "directory", true,  "input Directory containing newick trees");
		coptions.addOption("don", "donor"      , true,  "Donor(s); separate multiple donors with comma");

		coptions.addOption("cut", "cutoff"     , true,  "node support Cutoff (default=0)");		
		coptions.addOption("out", "output"     , true,  "specify suffix for Output directory and files");		

		coptions.addOption("opt", "optional"   , true,  "Optional taxa allowed in monophyletic ingroup");
		coptions.addOption("ign", "ignore"     , true,  "taxa to be Ignored while screening trees");
		coptions.addOption("igp", "ingroup"    , false, "export details of monophyletic Ingroups");
		
		coptions.addOption("ssn", "ssnode"     , true,  "minimal Strongly Supported Nodes uniting query and donors (default=1)");
		coptions.addOption("asn", "asnode"     , true,  "minimal number of All Supporting Nodes uniting query and donors (default=2)");
		
		
		/** 
		 * issue not yet solved
		 
		Option opt = Option.builder("opt1").required(false).longOpt("optional_taxa")
				.desc("optional taxa interwining the monophyletic ingroup")
				.build();
		coptions.addOption(opt);
		
		//args = new String[]{"-help"};
		*/
		
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
			
			if (line.hasOption("directory"))  indir      = line.getOptionValue("directory");
			if (line.hasOption("output"))       outHGT     = line.getOptionValue("output");
			
			if (line.hasOption("donor"))     donor         = line.getOptionValue("donor");
			if (line.hasOption("optional"))  optionals     = line.getOptionValue("optional");
			if (line.hasOption("ignore"))    ignored       = line.getOptionValue("ignore");

			if (line.hasOption("cutoff"))    cut           = Double.parseDouble(line.getOptionValue("cutoff")) ;
			
			if (line.hasOption("ssnode"))    minStrongNode = Integer.parseInt(line.getOptionValue("ssnode"));
			if (line.hasOption("asnode"))    minAllNode    = Integer.parseInt(line.getOptionValue("asnode"));
			
			if (line.hasOption("ingroup"))   getInGroup    = true;
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
	
	/** make output directory based on input arguments */
	private void setOutputFileAandDirectory() {
		//System.out.println("Output: " + outHGT);
		
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
				
			outHGT = outHGT.replaceAll(",", "");
		}
		/** create output directory and figure out out-file */
		outDir = outHGT + ".trees";
		new File(outDir).mkdirs();
		outHGT = outHGT + ".txt";
		//System.out.println("Output: " + outDir);
	}
	
}

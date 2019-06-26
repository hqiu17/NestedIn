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
	private int minStrongNode  = 1;
	private int minAllNode     = 2;
	
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
		
		/** print out some key parameters onto terminal */
		System.out.println("direcotry:     " + myParser.indir);
		System.out.println("donor(s):      " + myParser.donor);
		System.out.println("cut-off:       " + myParser.cut);
		System.out.println("optional taxa: " + myParser.optionals);
		System.out.println("ignored taxa:  " + myParser.ignored);

		
		/** tree parse heavy lifting and put results in an ArrayList */
		ArrayList<String> nbNodesCoded = new ArrayList<String>();
		nbNodesCoded = myParser.Adir(myParser.indir, myParser.donor, myParser.cut, myParser.optionals, myParser.ignored);
		
		// write results to out-file
		try {
			FileWriter hgtWriter = new FileWriter(myParser.outHGT);
			//hgtWriter.write("Gene\tStronglySupprtedNode(up to 2)\tWeaklySupprtedNode\tAdjustedNode\tStatus\n");
			hgtWriter.write("Gene\tNum.Node(>="+Double.toString(myParser.cut)+")"+"\tNum.Node(<"+Double.toString(myParser.cut)+")\n");
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
	 * process a directory
	 * return a list comprising coded nodes combination for each tree:
	 */
	public ArrayList<String> Adir (String dir, String donor, double cut, String optionals, String ignored) {
		ArrayList<String> results = new ArrayList<String>();
		
		// read directory and create a list containing all tree files
		// and set up progress object
		Path dp = Paths.get(dir);
		List<Path> files = new ArrayList<Path>();
		Bar progress = new Bar (200);
		try{
			files = Files.walk(dp,1)
					.skip(1)
					.filter(x->x.toString().matches("(.+)tree|(.+)tre"))
					.filter(x->Files.isRegularFile(x))
					.collect(Collectors.toList());
					//.toArray();
			int size = files.size();
			progress = new Bar (size);
		}
		catch (IOException e) {
			System.out.println("#1-> erronreous reading directory: " + dir);
		}
		
		// loop through all trees
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
	 * Examine a single tree file
	 * If input tree meets criteria, 1) write the tree the output directory
	 * 		2) return a string encoding nodes support:
	 * 		"query \t strong supported nodes \t weakly supported nodes \t adjusted nodes"
	 * On failed tree, only return a empty string
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
		
		/* to be updated in futher version: 
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
			System.out.println("#-> errorous writing file: " + outfile);
		}
		*/
		
		// check if tree meet requirement (at least one node support monophyly), then
		// export input tree to output directory		
		int myFate = fate(test.getStrongNodes(), test.getWeakNodes(), test.getAdjustedStrongNodes());
		//## test code
		//String outcome1 = query +"\t"+ test.getStrongNodes() +"\t"+ test.getWeakNodes() 
		//+"\t"+ test.getAdjustedStrongNodes() + "\t" + myFate;
		//System.out.println("#top " + outcome1);
		
		if (myFate > 1 ) {
			String outputrees = outDir + "/" + filename;
			try{
				FileWriter writer = new FileWriter(outputrees);
				writer.write(line + "\n");
				writer.close();
			}
			catch(IOException e){
				System.out.println("#-> errorous writing tree file: " + outputrees);
			}
			
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
			
			
			
			String outcome = query +"\t"+ test.getStrongNodes() +"\t"+ test.getWeakNodes() + "\t" + 
					         (test.getStrongNodes() + test.getWeakNodes()) + "\t" + myFate;
			//+"\t"+ test.getAdjustedStrongNodes() +"\t"+ myFate;
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
		coptions.addOption("dir", "directory", true,  "input Directory containing newick trees");
		coptions.addOption("out", "output"     , true,  "specify suffix for Output directory and files");		
		coptions.addOption("don", "donor"      , true,  "Donor(s); separate multiple donors with comma");
		coptions.addOption("cut", "cutoff"     , true,  "node support Cutoff (default=0)");
		coptions.addOption("opt", "optional"   , true,  "Optional taxa in monophyletic ingroup");
		coptions.addOption("ign", "ignore"     , true,  "taxa to be Ignored while screening trees");
		
		coptions.addOption("ssn", "ssnode"     , true,  "minimal Strongly Supported Nodes uniting query and donors (default=1)");
		coptions.addOption("asn", "asnode"     , true,  "minimal number of All Supporting Nodes uniting query and donors (default=2)");
		
		coptions.addOption("igp", "ingroup"    , false, "export details of monophyletic Ingroups");
		coptions.addOption("h"  , "help"       , false, "list usage instruction");
		
		
		/** 
		 * issue not yet solved
		 
		Option opt = Option.builder("opt1").required(false).longOpt("optional_taxa")
				.desc("optional taxa interwining the monophyletic ingroup")
				.build();
		coptions.addOption(opt);
		
		//args = new String[]{"-help"};
		*/
		
		/** parse command line arguments */
		try{
			CommandLine line = cparser.parse(coptions, args);
			
			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				System.out.println("");
				formatter.printHelp( "NestedIn (version 01)", coptions );
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
			System.out.println( "Unexpected exception:" + exp.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			System.out.println("");
			formatter.printHelp( "NestedIn", coptions );
		}
		
		/** quit if no input directory is provided */
		if (indir.isEmpty()) {
			System.out.println("\nWarning: no input directory is specified");
			System.exit(1);
		}
		/** quit if no donor taxa are provided */
		if (this.donor.isEmpty()) {
			System.out.println("\nWarning: no donor(s) is specified");
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
			if ( ! ignored.isEmpty())  outHGT = outHGT + "_Ignr" + ignored;
			outHGT = outHGT.replaceAll(",", "");
		}
		/** create output directory and figure out out-file */
		outDir = outHGT + ".trees";
		new File(outDir).mkdirs();
		outHGT = outHGT + ".txt";
		//System.out.println("Output: " + outDir);
	}
	
}

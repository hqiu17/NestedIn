package exe;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import newicktree.NewickTree;
import newicktree.TestMonophyly;
import progress.Bar;

/**
 * 
 * @author shadow
 *
 */
public class Task extends Thread{
	List<Path> files = new ArrayList<Path>();
	CountSyn count = new CountSyn();
	String donor;
	double cut;
	String optionals;
	String ignored;
	int minStrongNode;
	int minAllNode;
	int minOutGroupSize;
	ArrayList<String> results = new ArrayList<String>();
	String outDir;
	boolean getInGroup = false;
	Bar progress = new Bar(100);
	
	/**
	 * Constructor
	 * @param files a list containing a series of file paths
	 * @param count a CountSyn object for task counting across threads 
	 * @param donor a string for donor species. Multiple species are separated by comma ','.
	 * @param cut a double as cutoff for minimal branch support
	 * @param optionals a string for optional species. Multiple species are separated by comma ','.
	 * @param ignored a string for species to be ignored. Multiple species are separated by comma ','.
	 * @param minStrongNode an integer defining minimal number of nodes supporting desired monophyly strongly.
	 * @param minAllNode an integer defining minimal number of all nodes supporting desired monophyly regardless of support levels.
	 * @param results a list holding strings for results.
	 * @param outDir a string defining output directory
	 * @param getInGroup boolean whether or not to retrieve sequences in monophyly ingroup
	 * @param minOutGroupSize an integer defining the minimal number of sequences required in outgroup
	 * @param progress a Bar object
	 */
	public Task(List<Path> files, CountSyn count, String donor, double cut, String optionals, 
			    String ignored, int minStrongNode, int minAllNode, ArrayList<String> results, 
			    String outDir, boolean getInGroup, int minOutGroupSize, Bar progress) {
		this.files = files;
		this.count = count;
		this.donor = donor;
		this.cut = cut;
		this.optionals = optionals;
		this.ignored  = ignored;
		this.minStrongNode = minStrongNode;
		this.minAllNode = minAllNode;
		this.results = results;
		this.outDir = outDir;
		this.getInGroup = getInGroup;
		this.minOutGroupSize = minOutGroupSize; 
		this.progress = progress;
	}
	
	/**
	 * Launch scanning for the whole set of input trees
	 */
	public void run() {
		while(true){
			int index = this.count.getNext() -1 ;
			if (index>=files.size()) break;
			String code = Atree( files.get(index).toString(), donor, cut, optionals, ignored, minOutGroupSize);
			if (!code.isEmpty()) results.add(code);
			progress.grow(index+1);
		}
	}
	
	/** 
	 * Examine a single tree. If input tree meets criteria, 
	 * 1) write the tree the output directory
	 * 2) return a string encoding node information: "query \t strong nodes \t weak nodes \t all nodes"
	 * if input tree fails, return an empty string
	 * @param intree a string for path leading to the newick tree file.
	 * @param donor a string for the query species.
	 * @param cut a double defining brach support cutoff
	 * @param optionals a string for species to be allowed in monophyletic group
	 * @param ignored a string for species to be ignored 
	 * @return string
	 */
	public String Atree (String intree, String donor, double cut, String optionals, String ignored, int minOutGroupSize) {

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
		TestMonophyly test = new TestMonophyly(bp, query, donor, cut, optionals, ignored, minOutGroupSize);
		test.testExclusive();
		
		/* test monophyly with a limited irrelevant sequences. TBD*/
		//test.testGeneralized();

		/** get destine of the input tree */
		int myFate = fate(test.getStrongNodes(), test.getWeakNodes(), test.getAdjustedStrongNodes());
		
		/** if input tree meet criteria, do the following */
		if (myFate > 1 ) {
			/* 1) write input tree to output directory */
			String outputrees = outDir + "/" + filename;
			try{
				FileWriter writer = new FileWriter(outputrees);
				writer.write(line + "\n");
				writer.close();
			}
			catch(IOException e){
				System.out.println("#-> errorous writing tree file: " + outputrees);
			}
			
			/* 2) if requested, write ingroup details to output directory */
			if (getInGroup) {
				String outputInGroupSeqs = outDir + "/" + filename + ".ingroup.txt";
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
			
			/* 3) make coded node information and return */
			String outcome = query +"\t"+ 
							 test.getStrongNodes() +"\t"+ 
							 test.getWeakNodes() + "\t" + 
					         (test.getStrongNodes() + test.getWeakNodes())
					         ;
			return outcome;
		}
		
		return "";
	}
	
	public String Atree (String intree, String donor, double cut, String optionals, String ignored) {
		return Atree(intree, donor, cut, optionals, ignored, minOutGroupSize);
	}
	public String Atree (String intree, String donor, double cut, String optionals) {
		String ignored = new String();
		return Atree(intree, donor, cut, optionals, ignored);
	}
	public String Atree (String intree, String donor, double cut) {
		String optionals = new String();
		return Atree(intree, donor, cut, optionals);
	}
	public String Atree (Path intree, String donor, double cut, String optionals, String ignored) {
		String str = intree.toString();
		return Atree(str, donor, cut, optionals, ignored);
	}
	public String Atree (Path intree, String donor, double cut, String optionals) {
		String str = intree.toString();
		String ignored = new String();
		return Atree(str, donor, cut, optionals, ignored);
	}
	public String Atree (Path intree, String donor, double cut) {
		String str = intree.toString();
		String optionals = new String();
		return Atree(str, donor, cut, optionals);
	}
	
	/**
	 * Define the fate bi-partition
	 * @param strong an integer for the number of nodes strongly supporting desired monophyly
	 * @param weak an integer for the number of nodes weakly supporting desired monophyly
	 * @param fixed an integer for the number of nodes nodes supporting desired monophyly after fixing
	 *        a limited number of potential contaminations
	 * @return a integer coding for different fates of a tree ('9': strong; '2': fine; '1': week; '0': failed)
	 */
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
	
	/**
	 * Extract query from tree file name
	 * @param input a string for input tree fine name
	 * @return a string for query sequence name
	 */
	public String getQuery(String input) {
		String query = null;
		query = getQuerySpecial(input, "_2refseq");
		if (query == null) query = getQuerySpecial(input, ".contre");
		//if (query == null) query = getQuerySpecial(input, ".tree");
		if (query == null) query = getQuerySpecial(input, ".tre");
		if (query == null) query = getQuerySpecial(input, ".");
		return query;
	}

}

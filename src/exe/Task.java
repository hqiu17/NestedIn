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


public class Task extends Thread{
	List<Path> files = new ArrayList<Path>();
	CountSyn count = new CountSyn();
	String donor;
	double cut;
	String optionals;
	String ignored;
	int minStrongNode;
	int minAllNode;
	ArrayList<String> results = new ArrayList<String>();
	String outDir;
	boolean getInGroup = false;
	Bar progress = new Bar(100);
	
	public Task(List<Path> files, CountSyn count, String donor, double cut, String optionals, 
			    String ignored, int minStrongNode, int minAllNode, ArrayList<String> results, 
			    String outDir, boolean getInGroup, Bar progress) {
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
		this.progress = progress;
	}
	
	public void run() {
		while(true){
			int index = this.count.getNext() -1 ;
			if (index>=files.size()) break;
			String code = Atree( files.get(index).toString(), donor, cut, optionals, ignored);
			if (!code.isEmpty()) results.add(code);
			progress.grow(index+1);
		}
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
		//if (query == null) query = getQuerySpecial(input, ".tree");
		if (query == null) query = getQuerySpecial(input, ".tre");
		if (query == null) query = getQuerySpecial(input, ".");
		return query;
	}
	
	
	
	
}

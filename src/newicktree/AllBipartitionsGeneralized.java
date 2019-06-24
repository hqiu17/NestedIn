package newicktree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AllBipartitionsGeneralized {
	private String donor;
	private List<String> bipartitions = new ArrayList<String>();
	private int nbStrongMonophyleticNodes=0;
	private int nbWeakMonophyleticNodes  =0;
	private int nbAdjustedMonophyleticNodes =0;
	List<List<String>> minorContaminationsStrongNodes = new ArrayList<List<String>>();
	List<List<String>> minorContaminationsWeakNodes   = new ArrayList<List<String>>();

	public AllBipartitionsGeneralized(List<String> bipartitions, String query, String donor,
			double support_cut, String optionals, String ignored) {
		//System.out.println("#AllBipartitionGeneralized 1");
		this.donor = donor;
		this.bipartitions = bipartitions;
		
		AllBipartitions tree = new AllBipartitions(bipartitions, query, donor, support_cut, optionals, ignored);
		nbStrongMonophyleticNodes = tree.getStrongNodes();
		nbWeakMonophyleticNodes   = tree.getWeakNodes();
		//System.out.println("#  "+ nbStrongMonophyleticNodes +" "+ nbWeakMonophyleticNodes);
		
		if (nbStrongMonophyleticNodes<=1) {
			minorContaminationsStrongNodes = tree.getMinorContaminationsStrongNodes();
			nbAdjustedMonophyleticNodes = checkMinorContamForAllNodes(minorContaminationsStrongNodes);
			//System.out.println("#1 "+ fixedStrongNodes);
		}
	}
	
	public AllBipartitionsGeneralized(List<String> bipartitions, String query, String donor,
			double support_cut) {
		//System.out.println("#AllBipartitionGeneralized 2");
		this.donor = donor;
		this.bipartitions = bipartitions;
		
		AllBipartitions tree = new AllBipartitions(bipartitions, query, donor, support_cut);
		nbStrongMonophyleticNodes = tree.getStrongNodes();
		nbWeakMonophyleticNodes   = tree.getWeakNodes();
		
		if (nbStrongMonophyleticNodes<=1) {
			minorContaminationsStrongNodes = tree.getMinorContaminationsStrongNodes();
			nbAdjustedMonophyleticNodes = checkMinorContamForAllNodes(minorContaminationsStrongNodes);
		}
	}
	
	
	/*
	 * methods
	 */

	private int checkMinorContamForAllNodes(List<List<String>> contam ){
		int nbAdjusted=0;
		for (List<String> c : contam) {
			if (checkMinorContamForANode(c)) nbAdjusted++;
			if (nbAdjusted>=2) break;	//@@ if two adjusted strong nodes, no further test (non-exhaustive)
		}
		return nbAdjusted;
	}
	
	private boolean checkMinorContamForANode(List<String> contam ){
		// Examine all contaminat (c), if all form 'c'-'donor' monophyly, then this biparitition is adjusted  
		for (String c : contam) {
			AllBipartitions bp = new AllBipartitions(bipartitions, c, donor, 90);
			if ( bp.getStrongNodes() < 1 ) {
				return false;
			}
		}
		return true;
	}
	
	/*
	 * getters
	 */
	public int getStrongNodes(){
		return nbStrongMonophyleticNodes;
	}
	public int getWeakNodes(){
		return nbWeakMonophyleticNodes;
	}
	public int getAdjustedStrongNodes(){
		return nbAdjustedMonophyleticNodes;
	}

	/*
	 * 
	 */	
	
	public static void main(String[] args) throws IOException {
		ArrayList<String> bipartitions = new ArrayList<String>();
		String optionals = new String("Chromal");
		String ignored = "";
		//Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.contree.table.txt");
		//Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.hq.contree.table.txt");
		Path fp = Paths.get("/Users/shadow/Documents/java/data/evm.model.contig697.1xx_2refseq.info.txt.phy.txt.contree.table.txt");
		BufferedReader reader = Files.newBufferedReader(fp);
		while(true) {
			String line = reader.readLine();
			if (line == null) break;
			bipartitions.add(line);
		}
		
		AllBipartitionsGeneralized tree = new AllBipartitionsGeneralized(bipartitions, "evm.model.contig697.1xx", "Bacteria", 90, optionals, ignored);
		
		System.out.println("#b " + tree.getStrongNodes());
		System.out.println("#b " + tree.getWeakNodes());
		System.out.println("#b " + tree.getAdjustedStrongNodes());
	}

}

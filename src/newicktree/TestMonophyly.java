package newicktree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestMonophyly {
	private List<String> bipartitions = new ArrayList<String>();
	private String query;
	private String donor;
	private double support_cut;
	private String optionals = new String();
	private String ignored = new String();
	
	private int nbStrongMonophyleticNodes = 0;
	private int nbWeakMonophyleticNodes = 0;
	private int nbAdjustedMonophyleticNodes = -1;
	private int minOutGroupSize = 5;
	
	private ArrayList<String> supportDonorsAndOptionals = new ArrayList<String>();
	
	// constructor1
	
	/**
	 * Constructor
	 * @param bipartitions a string containing ingroup and outgroup
	 * @param query a string for query species
	 * @param donor a string for donor species. Multiple species are separated by comma ','.
	 * @param support_cut a double as cutoff for branch support
	 * @param optionals a string for optional species. Multiple species are separated by comma ','.
	 * @param ignored a string for species to be ignored. Multiple species are separated by comma ','.
	 * @param minOutGroupSize an integer to define the minimal number of sequences in outgroup (default = 5).
	 */
	public TestMonophyly(List<String> bipartitions, String query, String donor,
			double support_cut, String optionals, String ignored, int minOutGroupSize) {
		this.bipartitions = bipartitions;
		this.query = query;
		this.donor = donor;
		this.support_cut = support_cut;
		this.optionals = optionals;
		this.ignored = ignored;
		this.minOutGroupSize = minOutGroupSize;
	}

	/**
	 * Constructor
	 * @param bipartitions a string containing ingroup and outgroup.
	 * @param query a string for query species.
	 * @param donor a string for donor species. Multiple species are separated by comma ','.
	 * @param support_cut a double as cutoff for minimal branch support.
	 * @param optionals a string for optional species. Multiple species are separated by comma ','.
	 * @param ignored a string for species to be ignored. Multiple species are separated by comma ','. 
	 */
	public TestMonophyly(List<String> bipartitions, String query, String donor,
			double support_cut, String optionals, String ignored) {
		this.bipartitions = bipartitions;
		this.query = query;
		this.donor = donor;
		this.support_cut = support_cut;
		this.optionals = optionals;
		this.ignored = ignored;
	}

	/**
	 * Constructor
	 * @param bipartitions a string containing ingroup and outgroup
	 * @param query a string for query species
	 * @param donor a string for donor species. Multiple species are separated by comma ','.
	 * @param support_cut a double as cutoff for branch support
	 */
	public TestMonophyly(List<String> bipartitions, String query, String donor,
			double support_cut) {
		this.bipartitions = bipartitions;
		this.query = query;
		this.donor = donor;
		this.support_cut = support_cut;
	}
	
	/**
	 * Test all bi-partitions of a tree for monophyletic relationship.
	 */
	public void testExclusive() {
		AllBipartitions tree = new AllBipartitions(bipartitions, query, donor, support_cut, optionals, ignored, minOutGroupSize);
		nbAdjustedMonophyleticNodes = 0;
		nbStrongMonophyleticNodes = tree.getStrongNodes();
		nbWeakMonophyleticNodes = tree.getWeakNodes();
		supportDonorsAndOptionals = tree.getSupportDonorsAndOptionals();
	}
	
	/**
	 * Test all bi-partitions of a tree for monophyletic relationship allowing 
	 * a limited number of irrelevant sequences (due to contamination).
	 */
	public void testGeneralized() {
		AllBipartitionsGeneralized tree = new AllBipartitionsGeneralized(bipartitions, query, donor, support_cut, optionals, ignored);
		nbAdjustedMonophyleticNodes = tree.getAdjustedStrongNodes(); 
		nbStrongMonophyleticNodes = tree.getStrongNodes() + nbAdjustedMonophyleticNodes;
		nbWeakMonophyleticNodes = tree.getWeakNodes();
	}

	/*
	 *  getters
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
	public ArrayList<String> getSupportDonorsAndOptionals(){
		return supportDonorsAndOptionals;
	}
	
	/*
	 * 
	 */
	
	public static void main(String[] args) throws IOException {
		ArrayList<String> bipartitions = new ArrayList<String>();
		ArrayList<String> optionals = new ArrayList<String>();
		optionals.add("SSSS");
		optionals.add("Viridi");

		//Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.contree.table.txt");
		//Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.hq.contree.table.txt");
		Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig2149.8xx_2refseq.info.txt.phy.txt.contree.table.txt");
		BufferedReader reader = Files.newBufferedReader(fp);
		while(true) {
			String line = reader.readLine();
			if (line == null) break;
			bipartitions.add(line);
		}
		
		TestMonophyly tree = new TestMonophyly(bipartitions, "add.evm.model.contig2149.8xx", "Bacteria", 90);
		tree.testExclusive();
		System.out.println("#a " + tree.getStrongNodes());
		System.out.println("#a " + tree.getWeakNodes());
		System.out.println("#a " + tree.getAdjustedStrongNodes());
		
		tree.testGeneralized();
		System.out.println("#b " + tree.getStrongNodes());
		System.out.println("#b " + tree.getWeakNodes());
		System.out.println("#b " + tree.getAdjustedStrongNodes());
		
	}
}

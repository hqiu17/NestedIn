package newicktree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.*; 

public class AllBipartitions {
	private int nbStrongMonophyleticNodes=0;
	private int nbWeakMonophyleticNodes  =0;
	private ArrayList<String> supportDonorsAndOptionals = new ArrayList<String>();
	private ArrayList<String> Donors       = new ArrayList<String>();
	private ArrayList<String> DonorsStrong = new ArrayList<String>();
	ArrayList<List<String>> minorContaminationsStrongNodes = new ArrayList<List<String>>();
	ArrayList<List<String>> minorContaminationsWeakNodes   = new ArrayList<List<String>>();

	public AllBipartitions(List<String> bipartitions, String query, String donor, double support_cut, String optionals, String ignored) {
		visitAllBipartitions(bipartitions, query, donor, support_cut, optionals, ignored);
	}
	public AllBipartitions(List<String> bipartitions, String query, String donor, double support_cut, String optionals) {
		String ignored = new String();
		visitAllBipartitions(bipartitions, query, donor, support_cut, optionals, ignored);
	}
	public AllBipartitions(List<String> bipartitions, String query, String donor, double support_cut) {
		String optionals = new String();
		String ignored = new String();
		visitAllBipartitions(bipartitions, query, donor, support_cut, optionals, ignored);
	}

	
	/**
	 * Go through a list if all bi-partitions, do tests and population class variables
	 * @param bipartitions    a list containing all bi-partitions
	 * @param query           query sequence
	 * @param donor           donor taxa
	 * @param support_cut     node cutoff
	 * @param optionals       optional taxa allowed to be inside of monophyly
	 * @param ignored         taxa to be ignored
	 */
	public void visitAllBipartitions(List<String> bipartitions,
			                         String query,
			                         String donor,
		     						 double support_cut,
		     						 String optionals,
		     						 String ignored){

		/**
		 * loop through bi-partitions and test them one by one. 
		 * each bi-partition is in the format: support-value   "TAB"   one-half   "TAB"   the-other-half 
		 * */
		for (String l : bipartitions) {
			
			/**
			 * if 2 or more strongly supported nodes, break out of loop
			 * this non-exhaustive search speeds up entire job, because not all bi-partitions need to examined
			 */ 
			if ( nbStrongMonophyleticNodes >=2 ) break;
			
			
			/** break down the bi-partition string */
			String[] data = l.split("\t");
			double mySupport = Double.parseDouble(data[1]) ;
			String half01 = data[2].substring(1, data[2].length()-1);
			String half02 = data[3].substring(1, data[3].length()-1);
			
			/** examine the bi-partition in ABipatition class */
			ABipartition bp = new ABipartition(half01+"\t"+ half02);
			bp.checkIngroup(query, donor, optionals, ignored);
			int aStatus = bp.getStatus();
			
			
			if (aStatus >0) {				
			/** if bi-partition supports query-donor monophyly */
				
				if (mySupport >= support_cut) {
				/** if node supports query-donor monophyly */
					
					/** add in-group donors to variable Donors */
					donorPopulationInRecord(bp.getDonorSeqs());
					
					/** test if this donor-group already presents in DonorsStrong. if so, skip */
					if (donorPopulationInRecordStrong(bp.getDonorSeqs())) continue;
					
					/** increment strong monophyletic node */
					nbStrongMonophyleticNodes += aStatus;
					
					/** make record of in-group details */
					String record = new String(); 
					record = Double.toString(mySupport) +"\t"+ String.join(",", bp.getDonorSeqs()) +"\t"+ String.join(",", bp.getOptionalSeqs());
					supportDonorsAndOptionals.add(record);
				
				} else {
				/** if node does not support query-donor monophyly */
					
					/** test if this donor-group already met anywhere (in Donors). if so, skip */
					if (donorPopulationInRecord(bp.getDonorSeqs())) continue;
					
					/** increment weak monophyletic node */
					nbWeakMonophyleticNodes   += aStatus;
					
					/** make record of in-group details */
					String record = new String();
					record = Double.toString(mySupport) +"\t"+ String.join(",", bp.getDonorSeqs()) +"\t"+ String.join(",", bp.getOptionalSeqs());
					supportDonorsAndOptionals.add(record);	
				}
				
			} else if (aStatus == -1) {
			/** in-group is adjustable containing minimal irrelevant sequences (<3)*/
				
				/** if the collection of irrelevant sequences has something */
				if (! bp.getMinorContamination().isEmpty()) {
					if (mySupport >= support_cut){
						minorContaminationsStrongNodes.add(bp.getMinorContamination());
					} else {
						minorContaminationsWeakNodes.add(bp.getMinorContamination());
					}
				}
			}
			
		}
	}
	
	/** 
	 * this method is to generalize the following two similar function
	 * Not done yet.
	public boolean addAndReturn(List<String>multiDonorPopulations, List<String>allDonors) {
		List<String> myDonorPopulation = new ArrayList<String>(allDonors);
		Collections.sort(myDonorPopulation)
		boolean status = true;
		return status;
	}
	*/
	
	/**
	 * Sort elements in allDonors and concatenate into comma (','), compare the 
	 * resulting super-donor to class variable Donors.
	 * Donors are defined by supporting nodes regardless of bootstrap value 
	 * If the super-donor is in Donors, return true. 
	 * Otherwise, return false and add the super-donor into Donors. 
	 * @param allDonors
	 * @return status
	 */
	private boolean donorPopulationInRecord(List<String>allDonors) {
		boolean status = true;
		List<String> mydonors = new ArrayList<String>(allDonors);
		Collections.sort(mydonors);
		String mydonorstring = new String();
		mydonorstring = String.join(",",mydonors);
		if (Donors.contains(mydonorstring)) {
		} else {
			Donors.add(mydonorstring);
			status = false;
		}
		return status;
	}
	
	/**
	 * Sort elements in allDonors and concatenate into comma (','), compare the 
	 * resulting super-donor to class variable DonorsStrong.
	 * DonorsStrong are defined by supporting nodes with bootstrap value greater than cut-off 
	 * If the super-donor is in Donors, return true. 
	 * Otherwise, return false and add the super-donor into Donors. 
	 * @param allDonors
	 * @return status
	 */
	private boolean donorPopulationInRecordStrong(List<String>allDonors) {
		boolean status = true;
		List<String> mydonors = new ArrayList<String>(allDonors);
		Collections.sort(mydonors);
		String mydonorstring = new String();
		mydonorstring = String.join(",",mydonors);
		if (DonorsStrong.contains(mydonorstring)) {
		} else {
			DonorsStrong.add(mydonorstring);
			status = false;
		}
		return status;
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
	public ArrayList<String> getSupportDonorsAndOptionals(){
		return supportDonorsAndOptionals;
	}
	public ArrayList<List<String>> getMinorContaminationsStrongNodes(){
		return minorContaminationsStrongNodes;
	}
	public ArrayList<List<String>> getMinorContaminationsWeakNodes(){
		return minorContaminationsWeakNodes;
	}

	
	
	public static void main(String[] args) throws IOException {
		ArrayList<String> bipartitions = new ArrayList<String>();
		String optionals = new String();
		optionals = "XXXXXXX";
		optionals = optionals+",Chromal";
		optionals = optionals+",Plantae";
		optionals = optionals+",Opisthokonta";

		
		Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.contree.table.txt");
		//Path fp = Paths.get("/Users/shadow/Documents/java/data/add.evm.model.contig458.25xx_2refseq.info.txt.phy.txt.hq.contree.table.txt");
		BufferedReader reader = Files.newBufferedReader(fp);
		while(true) {
			String line = reader.readLine();
			if (line == null) break;
			bipartitions.add(line);
			//System.out.println(line);
		}
		
		AllBipartitions tree = new AllBipartitions(bipartitions, "add.evm.model.contig458.25xx", "Bacteria", 90, optionals);
		//AllBipartitions tree = new AllBipartitions(bipartitions, "Opisthokonta.Fungi-Coccidioides_immitis.XP_001242411.2", "Bacteria", 85, optionals);
		
		System.out.println("#a " + tree.getStrongNodes());
		System.out.println("#a " + tree.getWeakNodes());
		//tree.getMinorContaminations().forEach(System.out::println);
		//System.out.println(minorContaminations);
		
		//bipartitions.forEach(System.out::println);
		
	}

}

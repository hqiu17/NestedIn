package newicktree;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AllBipartitions {
	private int nbStrongMonophyleticNodes=0;
	private int nbWeakMonophyleticNodes  =0;
	private ArrayList<String> supportDonorsAndOptionals = new ArrayList<String>();
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

	/*
	 * Core function
	 * examine all bi-partitions and calculate nodes that support monophyly (strongly or weakly)
	 */
	public void visitAllBipartitions(List<String> bipartitions, String query, String donor, double support_cut, String optionals, String ignored){
		// A bipartition format: support-value + "\t" + one-half + "\t" + the-other-half 
		//System.out.println(bipartitions);
		//System.out.println("#AllBipartition l36: '" +" "+ query +" "+ donor +" " + optionals + "'\t'" + ignored +"'");
		for (String l : bipartitions) {
			//@@@ non-exhaustive search (need to be more flexible here) 
			if ( nbStrongMonophyleticNodes >=2 ) break;
			
			String[] data = l.split("\t");
			double mySupport = Double.parseDouble(data[1]) ;
			String half01 = data[2].substring(1, data[2].length()-1);
			String half02 = data[3].substring(1, data[3].length()-1);
		
			ABipartition bp = new ABipartition(half01+"\t"+ half02);
			
			bp.checkIngroup(query, donor, optionals, ignored);
			int aStatus = bp.getStatus();
			//System.out.println ("#-> " + aStatus);
			
			if (aStatus >0) {
				if (mySupport >= support_cut) {
					nbStrongMonophyleticNodes += aStatus;
					String record = new String();
					record = Double.toString(mySupport) +"\t"+ String.join(",", bp.getDonorSeqs()) +"\t"+ String.join(",", bp.getOptionalSeqs());
					supportDonorsAndOptionals.add(record);
					
					//System.out.println("#1alls " + aStatus);
					//System.out.println("#2alls " + record);					
				} else {
					nbWeakMonophyleticNodes   += aStatus;
					String record = new String();
					record = Double.toString(mySupport) +"\t"+ String.join(",", bp.getDonorSeqs()) +"\t"+ String.join(",", bp.getOptionalSeqs());
					supportDonorsAndOptionals.add(record);
					//System.out.println("#1allw " + aStatus);
					//System.out.println("#2allw " + record);	
				}
			} else if (aStatus == -1) {
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

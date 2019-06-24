package newicktree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NewickTree {
	
	private ArrayList<String> edges = new ArrayList<String>();
    private String newick_tree = new String();
    private HashMap<String,String> nsupports = new HashMap<String,String>(); 
    
	// constructor
	public NewickTree() {
	}
	public NewickTree(String line) {
		line = line.trim();
		if (! line.isEmpty()) {
			this.newick_tree = line;
			Decomposition();			
		}
	}
	//
	public String toString() {
		return this.newick_tree;
	}
	
	
	/*
	 *  getters
	 */
	
	// return all directional edges 
	public ArrayList<String> getBraches() {
		return edges;
	}
	// return all bi-partitions of leaves
	public List<String> getBipartitions(String seed) {
		Graph graph = new Graph(edges);
		return graph.fision(seed, nsupports);
	}
	public List<String> getBipartitions() {
		String seed = null;
		//return graph.fision("EThermoplasma_acidophilum.WP_010901349.1");
		return getBipartitions(seed);
	}

	public String getQueryTopTree(String leaf) {
		Graph nt = new Graph(edges);
		return nt.writeNewickTree(nsupports, leaf);
	}
	public String getTree() {
		String query = null;
		return getQueryTopTree(query);
	}

	public String getRerootedTree(ArrayList<String> input_edges, String node) {
		// Network nt = new Network(input_edges);
		// String tree = Network.growTree(node);
 
		return null;
	}


	/*
	 * methods for core processing
	 */
	
	/**
	 * this method goes through the tree visiting each ")" and makes judgment if
	 * bifurcations are left in the newick tree to be parsed. The loop stops when
	 * the left most ")" is <3 index to the right end of string newick tree
	 * 
	 * it also take care of the leftover of newick tree and turn it into edges
	 */
	private void Decomposition() {
        int node_tag = 0;
        int count=0;
        while (true) {
            int cbracket_right = newick_tree.indexOf(")");
            if (cbracket_right > (newick_tree.length()-3) ) {
                break;
            } else if (count >=1000) {
                break;
            } else {
                edges.addAll(collapsBifurcation(node_tag));
            	//System.out.println(newick_tree);
                node_tag++;
            }
            count++;
        }
        ArrayList<String> remaining_edges = new ArrayList<String>();        
        Parser_xfurcation pt = new Parser_xfurcation(newick_tree.toString());
        remaining_edges = pt.getEdges();
        edges.addAll(remaining_edges);
        //return bifurcations;
	}
	
	/**
	 * helper method that breaks bifurcations down to nodes/leaves
	 * collaps bifurcation one by one and updates newick tree at the end of each cycle
	 */
    private ArrayList<String> collapsBifurcation (int node_tag) {
        String interior_node = Integer.toString(node_tag);
        interior_node = "node_" + interior_node + ":0.1";
        
        ArrayList<String> edges = new ArrayList<String>();
        int bifurcation_left;
        int bifurcation_right;
        int cbracket_right = newick_tree.indexOf(")");
        int cbracket_left  = newick_tree.lastIndexOf("(", cbracket_right);
        char leading_char  = newick_tree.charAt(cbracket_left-1);
        /* 
         * There are 2 ways that a bifurcation is represented in newick tree
         * 1)  ((A:0.1,B:0.1)95:0.01 , c)    # bifurcation to the left of comma 
         * 2a) (c ,(A:0.1,B:0.1)95:0.01)     # bifurcation to the right of comma
         * 2b) (c ,(A:0.1,B:0.1)95:0.01, d)  # bifurcation to the right of comma; the tree is trifurcation
         */
        if (leading_char == ',') {
        	// 2a) scenario
            int ending_paranthesis = newick_tree.indexOf(")",cbracket_right+1);
            // 2b) scenario
            int ending_comma = newick_tree.indexOf(",",cbracket_right);
            bifurcation_left  = cbracket_left;            
            if (ending_comma > ending_paranthesis || ending_comma == -1) {
                bifurcation_right = ending_paranthesis;
            } else {
                bifurcation_right = ending_comma;
            }

        } else {
            bifurcation_left  = cbracket_left;
            int ending_comma   = newick_tree.indexOf(",", cbracket_right);
            bifurcation_right = ending_comma;
        }
        // build reduced tree for the next round of bifurcation recognition
        String wing_left   = newick_tree.substring(0, bifurcation_left);
        String bifurcation = newick_tree.substring(bifurcation_left,bifurcation_right);
        //String bifurcation_t = newick_tree.substring(bifurcation_left-10,bifurcation_right+10);
        String wing_right  = newick_tree.substring(bifurcation_right);
        newick_tree = new String(wing_left + interior_node + wing_right);
                
        // make edges for the collapsed bifurcation
        Parser_xfurcation pb = new Parser_xfurcation(bifurcation,interior_node);
        edges = pb.getEdges();
        
        // populate hashmap nsupports 
        interior_node = Integer.toString(node_tag);
        interior_node = "node_" + interior_node;
        String nsupport = pb.getNodeSupport();
        nsupports.put(interior_node, nsupport);
        

        // temporary output for debug
        //System.out.println("# " + interior_node + " ---> " + bifurcation);
        
        return edges;
    }

    public static void main(String[] args) {
    	NewickTree tree = new NewickTree(
    			"(Cuniculiplasma_divulgatum.WP_021788675.1:0.0773168973,xxx:0.2,(((((((Cafeteria_roenbergensis.YP_003969862.1:1.0155268887,Acanthamoeba_polyphaga.YP_003986746.1:0.6713382986)96:0.303775,Chrysochromulina_ericina.YP_009173464.1:0.9724348142)88:0.232339,add.evm.model.contig1250.9xx:0.8524546185)100:0.785817,"
    			+ "((((((MLingula_anatina.XP_013410080.1:0.1637130783,(MOctopus_bimaculoides.XP_014782290.1:0.1946955030,((MBiomphalaria_glabrata.XP_013069038.1:0.0557790563,MAplysia_californica.XP_005092830.1:0.0278133929,XXX01:0.2)97:0.051695,MHelobdella_robusta.XP_009026544.1:0.2195018150):0.019732):0.029768):0.042879,"
    			+ "((MRhagoletis_zephyria.XP_017488336.1:0.1000871902,MHyalella_azteca.XP_018027884.1:0.3116807717):0.024858,MGalendromus_occidentalis.XP_003743470.1:0.0992659710)87:0.042898):0.027611,MBranchiostoma_belcheri.XP_019619252.1:0.1278861925):0.034312,MCiona_intestinalis.XP_002129417.1:0.1539190261):0.064760,"
    			+ "MNecator_americanus.XP_013307342.1:0.2984450148):0.050973,MAnopheles_gambiae.XP_310127.3:0.4751263353)95:0.331271)100:0.765153,EEuryarchaeota_archaeon.WP_086637140.1:0.3198444231):0.078293,EMethanopyrus_sp..WP_088335994.1:0.7087447465)100:0.280803,EAcidiplasma_sp..WP_048101327.1:0.0424591981):0.045691,EThermoplasma_acidophilum.WP_010901349.1:0.1029968739);"
    			);
    	
    	String nw = tree.getQueryTopTree("add.evm.model.contig125");
    	System.out.println(nw);

	}

}

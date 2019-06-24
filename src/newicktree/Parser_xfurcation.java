package newicktree;

import java.util.ArrayList;

public class Parser_xfurcation {
	
    ArrayList<String> edges = new ArrayList<String>();
    String nsupport = new String();
    
    // constructor
	public Parser_xfurcation(String line) {
		toNodes(line);
	}
    public Parser_xfurcation (String line, String interior_node) {
        if (interior_node.contains(":")) {
            int colon_index1 = interior_node.indexOf(":");
            interior_node = interior_node.substring(0,colon_index1);
        }
        toNodes(line, interior_node);
    }

    /*
     * methods
     */
    
    // getter
    public ArrayList<String> getEdges() {
        return edges;
    }
    public String getNodeSupport() {
    	if (nsupport.isEmpty()) nsupport = "-1"; 
        return nsupport;
    }
    

    /*
     * core methods
     */
    
    // find leaves in input and put all leaf-to-leaf edges into "edges"
    public ArrayList<String> toNodes(String line) {
    	
	    ArrayList<String> nodes = new ArrayList<String>();
        int colon_index=0;
        int comma_index=0;
        
        // When dealing with the final collapsed tree, such as (A, B, C),
        // parentheses are carried in the line
        // simple solution is to remove the leading "("
        int paranthesis = line.indexOf("(");
        if (paranthesis != -1) {
        	line = line.substring(paranthesis+1);
        }
	    
	    while (true ) {
	    	// break the loop if no further colon is found
	    	colon_index = line.indexOf(":", colon_index+1);
	    	if (colon_index == -1) break;
	    	
	    	int paranthesis_index = line.lastIndexOf(")", colon_index);	    	
	    	if (paranthesis_index != -1) {
	    		nsupport = line.substring(paranthesis_index+1, colon_index);
	    		//System.out.println(paranthesis_index + "\t" + colon_index + "\t" + nsupport);
	    	} 
	    	
	    	// temporary storage of comma 
	        int comma_index0 = line.lastIndexOf(",", colon_index);
	        
	        // given "A:0.5,B:0.5)95:0.5", the first node (A) start from string index 0
	        if (comma_index0 < 0) { 
	        	comma_index = 0;
	        // given "A:0.5,B:0.5)95:0.5", the second node (B) start from comma index + 1
	        } else {
		        // the last colon (as in "95:0.5") follows a monophyly instead of a single node
	        	// it shares the same common index (as in "0.5,B") with the second colon (as in B:0.5)
	        	// this signature allow the removal of last colon from further process
		        if (comma_index0 == (comma_index-1) ) { 
		        	continue;
		        } else {
		        	comma_index = comma_index0+1;
		        }
	        }
	    	String tip = line.substring(comma_index, colon_index);
	    	nodes.add(tip);
	    }
	    
	    // make directional edges for all pairs 
	    for (int i=0; i<nodes.size(); i++) {
	    	for (int j=0; j<nodes.size(); j++) {
	    		if (i == j) continue;
	    		edges.add(nodes.get(i) + "\t" + nodes.get(j) );
	    		//System.out.println( "@ " + nodes.get(i) + "\t" + nodes.get(j) );
	    	}
	    }
	    return nodes;
    }
    
    // when an interior node is given ("tag"), make edges 
    public void toNodes(String line, String tag) {
    	ArrayList<String> nodes = toNodes(line);
    	for (String n : nodes) {
    		edges.add(tag + "\t" + n);
    		edges.add(n + "\t" + tag);
    		//System.out.println(" #" + tag + "\t" + n );
    	}
    }
    
}

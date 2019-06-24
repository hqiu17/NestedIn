package newicktree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph {
	
	private HashMap<String, HashSet<String>> graph = new HashMap<String, HashSet<String>>();  
	private List<String> edges = new ArrayList<String>();
	
	// constructor
	public Graph(ArrayList<String> edges) {
		this.edges = edges;
		drawGraph(this.edges);
	}
	public List<String> ToString() {
		return edges;
	}
	
	/*
 	 *	core methods
	 */
	
	// return bi-partitions of the graph, putting the half containing the "seed" leaf at front 
	// followed by the second half;
	// when "seed" is null, a random seed is picked from the terminal leaves
	public List<String> fision(String seed, HashMap<String,String> nsupports) {
		List<String> bipartitions = new ArrayList<String>();
		
		// prepare interior nodes (excluding terminal leaves)
		Set<String> nodes = graph.keySet();
		List<String> interior_nodes = new ArrayList<String>();
		List<String> outerior_nodes = new ArrayList<String>();
		for (String node : nodes) {
			int index = node.indexOf("node_");
			if (index != -1) {
				interior_nodes.add(node); 
			} else {
				outerior_nodes.add(node);
			}
		}
		
		// test input seed
		if (outerior_nodes.isEmpty()) return bipartitions;
		if (seed == null) seed = outerior_nodes.get(0);
		
		// loop through each interior node, remove it from graph, and find the node linkage
		for (String inode : interior_nodes) {
			HashMap<String, HashSet<String>> local_graph = deletNode(graph, inode );
			HashSet<String> linked = spread (local_graph,seed);
			
			// pull out outerior nodes from node linkage
			HashSet<String> linked_outerior_nodes = new HashSet<String>();
			for (String l : linked) {
				if (outerior_nodes.contains(l)) linked_outerior_nodes.add(l);
			}
			// get the remaining outerior nodes
			HashSet<String> remaining_outerior_nodes = new HashSet<String>(outerior_nodes);
			for (String l : linked) {
				remaining_outerior_nodes.remove(l);
			}

			bipartitions.add(inode +"\t"+ nsupports.get(inode) +"\t"+ linked_outerior_nodes +"\t"+ remaining_outerior_nodes);
		}

		return bipartitions;
	}

	// return bi-partitions of the graph, putting the half containing a random "seed" leaf 
	// at front followed by the second half
	public List<String> fision(HashMap<String,String> nsupports) {
		String seed = null;
		return fision(seed, nsupports);
	}
	
	/*
	// this method deletes a specified interior node from the graph
	private HashMap<String, HashSet<String>> deletNode (HashMap<String, HashSet<String>> graph, String node) {
		// !!! make a copy of hashmap
		HashMap<String, HashSet<String>> graph01 = new HashMap<String, HashSet<String>>(graph);
		HashSet<String> neighbours = new HashSet<String>(graph01.get(node));
		graph01.remove(node);
		for (String n : neighbours) {
			// !!! make a copy of hashset 
			HashSet<String> hs = new HashSet<String>(graph01.get(n));
			hs.remove(node);
			graph01.put(n, hs);
		}
		return graph01;
	}
	*/
	
	private HashMap<String, HashSet<String>> deletNode (HashMap<String, HashSet<String>> g, String node) {
		// !!! make a copy of hashmap
		HashMap<String, HashSet<String>> graph = new HashMap<String, HashSet<String>>(g);
		HashSet<String> neighbours = new HashSet<String>(graph.get(node));
		graph.remove(node);
		for (String n : neighbours) {
			// !!! make a copy of hashset 
			HashSet<String> hs = new HashSet<String>(graph.get(n));
			hs.remove(node);
			graph.put(n, hs);
		}
		return graph;
	}
	
	
	// helper method that finds all nodes that have paths to a seed
	private HashSet<String> spread(HashMap<String, HashSet<String>> graph, String seed) {
		HashSet<String> done = new HashSet<String>();
		HashSet<String> novel = new HashSet<String>();
		novel.add(seed);
		done.add(seed);
		while(true) {
			int increment = 0;
			HashSet<String> next_round = new HashSet<String>();
			for (String d : novel ) {
				HashSet<String> d_neighbours = graph.get(d);
				if (d_neighbours == null || d_neighbours.isEmpty()) continue;
				for (String n : d_neighbours) {
					if (! done.contains(n)) {
						next_round.add(n);
						done.add(n);
						increment +=1;
					}
				}
			}
			
			if (increment > 0) {
				novel = next_round;
			} else {
				break;
			}
		}
		return done;
	}
	
	// convert graph to newick tree, draw bi/multi-furcations one by one
	public String writeNewickTree(HashMap<String,String> nodeSupports, String query) {
		HashMap<String, HashSet<String>> g = new HashMap<String, HashSet<String>>(graph);
		//System.out.println("# hash size " + g.keySet().size());
		int count=0;
		while (true) { count++; //System.out.println("#-> " + count);
			int trim=0;
			ArrayList<String> allNodes = new ArrayList<String>();
			allNodes.addAll(g.keySet());
			for (String node : allNodes) {
				if ( !node.contains("node_") ) continue;
				String support = nodeSupports.get(node);
				if (support == null) support = "0";

				
				HashSet<String> neighbours = new HashSet<String>(g.get(node));
				HashSet<String> leaf_group = new HashSet<String>();

				boolean hookup = false;
				// break out after one group of neighbor is found
				for (String n1 : neighbours) {
					if (hookup ) break;
					if (n1.contains("node_")) continue;
					leaf_group.add(n1);
					// go through whole list of neighbors and collect those connected
					for (String n2 : neighbours) {
						if (n2.contains("node_") || n1.equals(n2) ) continue;
						if ( areConnected(g, n1, n2)) {
							leaf_group.add(n2);
							hookup = true;
						}
					}
					if (hookup) {
						turnNodesToBifurcation(g, node, leaf_group, support, query);
						trim++;
					} else {
						leaf_group.clear();
					}
				}
				
			}
			if (trim == 0) break;
			if ( count > 1000 ) break;
		}
		
		// after all interior nodes are replaced, reorganize leaves putting query at beginning of the tree
		// unite all leaves into a polyphyletic newick tree
		String newTreeHead = new String("(");
		String newTreeTail = new String();
		List<String> tips = g.keySet().stream().map(x->x+":0.1,").collect(Collectors.toList());
		if (query != null && !query.isEmpty()) {
			for (String tip : tips ) {
				if (tip.contains(query)) {
					newTreeHead = newTreeHead + tip;
				}else{
					newTreeTail = newTreeTail + tip;
				}
			}
		} else {
			for (String tip : tips ) {
				newTreeHead = newTreeHead + tip;
			}
		}
		newTreeHead = newTreeHead + newTreeTail; 
		newTreeHead = newTreeHead.substring(0, newTreeHead.length()-1);
		newTreeHead = newTreeHead + ");";
		return newTreeHead;
	}
	public String writeNewickTree(HashMap<String,String> nodeSupports) {
		String query = "";
		return writeNewickTree(nodeSupports, query);
		
	}
	
	// time connected nodes and write them as the name of their ancestral node in the graph
	private void turnNodesToBifurcation(HashMap<String, HashSet<String>>g, String in, 
			HashSet<String> leaf_group, String support, String query){
		//Set<String> lg = new HashSet<String>(leaf_group); 
		
		
		// order leaves putting query_containing leaves at front
		String withQuery = new String();
		ArrayList<String> otherLeaves    = new ArrayList<String>();
		ArrayList<String> orderedLeaves  = new ArrayList<String>();
		if (query == null || query.isEmpty()) {
			orderedLeaves.addAll( leaf_group.stream().collect(Collectors.toList()));
		} else {
			for (String l : leaf_group) {
				if (l.contains(query)) {
					withQuery = l;
				} else {
					otherLeaves.add(l);
				}
			}
			orderedLeaves.add( withQuery);
			orderedLeaves.addAll( otherLeaves.stream().collect(Collectors.toList()));
		}
		// put ordered leaves into newick tree format, remove them from graph
		// and replace the parental node with newick tree
		HashSet<String> neighbours = new HashSet<String>(g.get(in));
		String newNodeName = new String("(");
		for ( String l : orderedLeaves ) {
			if (l.isEmpty()) continue;
			newNodeName = newNodeName + l + ":0.1,";
			g.remove(l);
			neighbours.remove(l);
		}
		newNodeName = newNodeName.substring(0, newNodeName.length()-1) + ")";
		newNodeName = newNodeName + support;
		g.remove(in);
		g.put(newNodeName, neighbours);
		// update all nodes connected with the parental node (the one being replaced)
		for (String node : neighbours) {
			HashSet<String> secondaryNeighbours = new HashSet<String>(g.get(node));
			secondaryNeighbours.remove(in);
			secondaryNeighbours.add(newNodeName);
			g.put(node, secondaryNeighbours);
		}
	}

	// test if two nodes are connected
	// ??? need to be generalized to multifurcation
	private boolean areConnected(HashMap<String, HashSet<String>> g, String n1, String n2) {
		System.out.println("#  " + n1 + " -- " + n2);
		System.out.println("#a " + g.get(n1) );
		System.out.println("#b " + g.get(n2) );
		if (g.get(n1).contains(n2) && g.get(n2).contains(n1)) {
			System.out.println("#c true **********************");
			return true;
		}
		System.out.println("#d false");
		return false;
	}
	
	
	// populate graph (hashmap) with edges
	private void drawGraph (List<String> edges) {
		for (String edge : edges) {
			String[] nodes = edge.split("\t");
			addEdgeToGraph(nodes[0],nodes[1]);
			addEdgeToGraph(nodes[1],nodes[0]);
		}
	}
	private void addEdgeToGraph(String n1, String n2){
		if (graph.containsKey(n1)) {
			HashSet<String> neighbours = graph.get(n1);
			neighbours.add(n2);
			graph.put(n1, neighbours);
		} else {
			HashSet<String> neighbours = new HashSet<String>();
			neighbours.add(n2);
			graph.put(n1, neighbours);
		}		
	}
	
	public String getRerootedTree(String leave){
		edges.forEach(System.out::println);
		return "xx";
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

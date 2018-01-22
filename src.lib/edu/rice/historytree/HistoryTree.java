/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.historytree;


import edu.rice.historytree.generated.Serialization;


/** Top level class for implementing a history tree. 
 *
 * @author crosby
 *
 * @param <A> The type of aggregate value
 * @param <V> The type of annotation
 */

public class HistoryTree<A,V> extends TreeBase<A,V> {
		
	
	/** Make an empty merkle tree with a given aggobj and datastore.  */
	public HistoryTree(AggregationInterface<A,V> aggobj,
	    		   HistoryDataStoreInterface<A,V> datastore) {
	    super(aggobj,datastore);
	}
	@Override
	public A agg() {
		return aggV(time);
    }

	/** Get the aggregate at a particular historical version number */
    public A aggV(int version) {
    	assert (version <= time);
    	NodeCursor<A,V>  child, leaf, node;

    	child = leaf = this.leaf(version);
    	node = leaf.getParent(root);
    	A agg = leaf.getAgg();
		//System.out.println("leaf"+node);

    	while (node!=null && version >= (1<<node.layer()-1)) {
    		//System.out.println("aggv"+node);
    		NodeCursor<A,V>  left = node.left();
    		if (child.equals(left))
    			agg = aggobj.aggChildren(agg,null);
    		else {
    			A leftagg = left.getAgg();
    			assert leftagg != null;
    			agg = aggobj.aggChildren(leftagg,agg);
    		}
    		child = node;
    		node = node.getParent(root);
    	}
    	return agg;
    }
	
    @Override
	public A getAggAtVersion(NodeCursor<A,V> node, int version){
		// can only look up aggs for past versions
		assert (version <= time);
		// node doesn't exist
		if(node == null) {
			return null;
		}
		// if this agg is frozen at that version 
		// then it will not have changed in the future so we can use it 
		// straight away
		if(node.isFrozen(version)) {
			return node.getAgg();
		}
		// TODO: @Crosby seems to have been confused and used different
		// semantics in Merkle and History trees. In Merkle trees
		// we return empty aggregators, but in history trees
		// we return nulls. This is confusing and should be resolved
		// but for now we keep it so that the test cases are still
		// okay
		if(node.isLeaf()) {
			return null;
		}

		// otherwise calculate the left and right aggs recursively
		A leftAgg, rightAgg;
		leftAgg = this.getAggAtVersion(node.left(), version);
		rightAgg = this.getAggAtVersion(node.right(), version);
		
		if(leftAgg == null && rightAgg == null) {
			return null;
		}
		
		A agg = this.aggobj.aggChildren(leftAgg, rightAgg);
		return agg;
	}
    
	
    /**
     * Same spec as {@link #aggV(int)}, but also returns the 
     * children along with the aggregation value.
     * @param version
     * @return
     */
    public AggWithChildren<A> aggVWithChildren(int version) {
    	assert (version <= time);
    	NodeCursor<A,V>  child, leaf, node;
    	A leftagg = this.aggobj.emptyAgg();
    	A rightagg = this.aggobj.emptyAgg();

    	child = leaf = this.leaf(version);
    	node = leaf.getParent(root);
    	A agg = leaf.getAgg();
		//System.out.println("leaf"+node);
    	
    	while (node!=null && version >= (1<<node.layer()-1)) {
    		//System.out.println("aggv"+node);
    		NodeCursor<A,V>  left = node.left();
    		if (child.equals(left)) {
    			// TODO: this is a hack -- need to add a copy method to the 
    			// aggregation interface spec
    			leftagg = this.aggobj.parseAgg(this.aggobj.serializeAgg(agg));
    			rightagg = this.aggobj.emptyAgg();
    			agg = aggobj.aggChildren(agg,null);
    		}
    		else {
    			leftagg = left.getAgg();
    			rightagg = this.aggobj.parseAgg(this.aggobj.serializeAgg(agg));;
    			assert leftagg != null;
    			agg = aggobj.aggChildren(leftagg,agg);
    		}
    		child = node;
    		node = node.getParent(root);
    	}
    	AggWithChildren<A> aggPlusChildren = new AggWithChildren<A>(agg, leftagg, rightagg);
    	return aggPlusChildren;
    }
    
    //
    //  Operations for making pruned trees.
    //
    @Override
	public HistoryTree<A, V> makePruned(HistoryDataStoreInterface<A, V> newdatastore) {
    	HistoryTree<A,V> out = new HistoryTree<A,V>(this.aggobj,newdatastore);
    	out.updateTime(this.time);
        out.root = out.datastore.makeRoot(root.layer());
    	out.copySiblingAggs(this,this.leaf(time),out.forceLeaf(time),true);
    	return out;
        }
    
    
    // Potentially implement later
    public HistoryTree<A,V> makePruned(HistoryDataStoreInterface<A,V> newdatastore,
    		int version){
    	HistoryTree<A,V> out = new HistoryTree<A,V>(this.aggobj, newdatastore);
    	// create a new history tree with the right time 
    	// and version
    	out.updateTime(version);
    	int layer = log2(version);
    	out.root = out.datastore.makeRoot(layer);
    	out.copySiblingAggs(this, this.leaf(version), out.forceLeaf(version), true);
    	return out;
    }

    @Override
    void parseSubtree(NodeCursor<A,V> node, Serialization.HistNode in) {
    	if (parseNode(node,in))
    		return; // If its a stub.

    	// Not a stub. Must always have a left and may have right child.
    	if (!in.hasLeft())
    		throw new Error("Invalid Proof. Missing left child.");

    	parseSubtree(node.forceLeft(), in.getLeft());

    	if (in.hasRight()) {
    		parseSubtree(node.forceRight(), in.getRight());
    		if (node.isFrozen(time)) {
    			node.markValid();
    			node.setAgg(aggobj.aggChildren(node.left().getAgg(),node.right().getAgg()));
    		}
    	}
    }
}

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
import edu.rice.historytree.generated.Serialization.HistNode;
import edu.rice.historytree.storage.AppendOnlyArrayStore;

/**
 * Top level class for implementing a history tree.
 * 
 * @author crosby
 * 
 * @param <A>
 *            The type of aggregate value
 * @param <V>
 *            The type of annotation
 */
public class MerkleTree<A, V> extends TreeBase<A, V> {
	/** Has this Merkle tree been frozen and marked immutable? */
	boolean isFrozen = false;

	/** Make an empty merkle tree with a given aggobj and datastore. */
	public MerkleTree(AggregationInterface<A, V> aggobj,
			HistoryDataStoreInterface<A, V> datastore) {
		super(aggobj, datastore);
		// Complain if this mistake is made. 
		if (datastore instanceof AppendOnlyArrayStore<?, ?>)
			throw new Error(
					"Merkle Tree incompatible with AppendOnlyArrayStore");
	}

	@Override
	public MerkleTree<A, V> makePruned(
			HistoryDataStoreInterface<A, V> newdatastore) {
		if (isFrozen == false)
			throw new Error("Attempt to make pruned trees out of an unfrozen merkle tree");
		MerkleTree<A, V> out = new MerkleTree<A, V>(this.aggobj, newdatastore);
		out.updateTime(this.time);
		out.root = out.datastore.makeRoot(root.layer());
		out.isFrozen = true;
		out.root.copyAgg(this.root);
		return out;
	}

	/**
	 * Helper when freezing the Merkle tree. mark all of the unused right
	 * children to be valid with the default NULL agg.
	 */
	public void freezeHelper(NodeCursor<A, V> node) {
		node.markValid();
		if (node.right() == null)
			node.forceRight().setAgg(aggobj.emptyAgg()); // Force every right
															// child to be
															// valid, will keep
															// the NULL default
															// agg.
		node.setAgg(aggobj.aggChildren(node.left().getAgg(), node.right()
				.getAgg()));
	}

	/**
	 * Freeze the tree. After this, nothing else should be added to the Merkle
	 * tree. Until being frozen, proofs and subtrees must not be built.
	 * 
	 * Take every node in path to leaf and mark it valid and set as a stub with
	 * a precomputed agg.
	 */
	public void freeze() {
		if (isFrozen == true)
			throw new Error("Attempt to re-freeze frozen Merkle tree");
		isFrozen = true;
		if (time <= 0)
			return;
		NodeCursor<A, V> node = leaf(time);
		while ((node = node.getParent(root)) != null) {
			freezeHelper(node);
		}
	}

	@Override
	public A agg() {
		if (!isFrozen)
			throw new Error("Cannot compute agg from unfrozen MerkleTree");
		if (root == null)
			return aggobj.emptyAgg();
		return root.getAgg();
	}
	

	@Override
	public A getAggAtVersion(NodeCursor<A,V> node, int version){
		if(!isFrozen) {
			throw new Error("Cannot compute agg from unfrozen MerkleTree");
		}
		// can only look up aggs for past versions
		assert (version <= time);
		
		// node doesn't exist we pass it back up
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
			return aggobj.emptyAgg();
		}

		// otherwise calculate the left and right aggs recursively
		A leftAgg, rightAgg;
		leftAgg = this.getAggAtVersion(node.left(), version);
		rightAgg = this.getAggAtVersion(node.right(), version);
		
		if(leftAgg == null && rightAgg == null) {
			return aggobj.emptyAgg();
		}
		
		A agg = this.aggobj.aggChildren(leftAgg, rightAgg);
		return agg;
	}
    

	@Override
	public void parseTree(Serialization.PrunedTree in) {
		super.parseTree(in);
		isFrozen = true;
	}

	@Override
	void parseSubtree(NodeCursor<A, V> node, HistNode in) {
		if (parseNode(node, in))
			return; // If its a stub.

		parseSubtree(node.forceLeft(), in.getLeft());
		parseSubtree(node.forceRight(), in.getRight());
		node.markValid();
		node.setAgg(aggobj.aggChildren(node.left().getAgg(), node.right()
				.getAgg()));
	}
}

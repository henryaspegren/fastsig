package edu.rice.batchsig;

import java.util.ArrayList;
import edu.rice.historytree.HistoryTree;


/** Verify each of the messages in the batch one at a time. */
public class VerifyHisttreeSingle extends VerifyHisttreeEagerlyBase {
	public VerifyHisttreeSingle(SignaturePrimitives signer) {
		super(signer);
	}

	@Override
	protected void process(ArrayList<IMessage> l) {
		for (IMessage m : l) {
			HistoryTree<byte[], byte[]> tree = HistTreeTools.parseHistoryTree(m);	
			// TODO: The leaf is actually checked twice here because 
			// 			verifyHistoryRoot actually checks the leaf as well
			//			need to clean up this code and edit verifyHistroyRoot's 
			//			documentation
			m.signatureValidity(Verifier.checkLeaf(m, tree) && HistTreeTools.verifyHistoryRoot(signer,m,tree));
		}
	}
}
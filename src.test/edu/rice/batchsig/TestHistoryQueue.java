package edu.rice.batchsig;

import org.junit.Test;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.VerifyQueue;

import junit.framework.TestCase;


public class TestHistoryQueue extends TestCase {

	@Test
	/** Make sure splicepoints are properly made */
	public void testSplicepoints() {
		DigestPrimitive prims = new DigestPrimitive();
		HistoryQueue signqueue=new HistoryQueue(prims);
		
		Object targetA = new Object(); 
		Object targetB = new Object();
		Object targetC = new Object();
		Object targetD = new Object();

		MessageWrap msg0 = new MessageWrap(1000).setRecipient(targetA); 
 		MessageWrap msg1 = new MessageWrap(1001).setRecipient(targetA); 
		MessageWrap msg2 = new MessageWrap(1002).setRecipient(targetB); 
		MessageWrap msg3 = new MessageWrap(1003).setRecipient(targetC); 
		MessageWrap msg4 = new MessageWrap(1004).setRecipient(targetB); 
		MessageWrap msg5 = new MessageWrap(1005).setRecipient(targetA); 

		MessageWrap msg6 = new MessageWrap(1006).setRecipient(targetC); 
		MessageWrap msg7 = new MessageWrap(1007).setRecipient(targetB); 
		MessageWrap msg8 = new MessageWrap(1008).setRecipient(targetD); 
		MessageWrap msg9 = new MessageWrap(1009).setRecipient(targetB); 
		
		//     MESSAGE FLOW VISUALIZATION
		//     1000,1001,1002,1003,1004,1005 || 1006,1007,1008,1009
		// A : 1000,1001,               1005 ||
		// B : 			 1002,     1004,     ||      1007,     1009
		// C : 				  1003,          || 1006,
		// D : 								 ||	          1008,
	
		
		/* Batch 1 */
		signqueue.add(msg0);
		signqueue.add(msg1);
		signqueue.add(msg2);
		signqueue.add(msg3);
		signqueue.add(msg4);
		signqueue.add(msg5);
		signqueue.process();
		assertEquals(1,prims.signcount);
		
		assertEquals(msg0.getSignatureBlob().getLeaf(),0);
		assertEquals(msg1.getSignatureBlob().getLeaf(),1);
		assertEquals(msg2.getSignatureBlob().getLeaf(),2);
		assertEquals(msg3.getSignatureBlob().getLeaf(),3);
		assertEquals(msg4.getSignatureBlob().getLeaf(),4);
		assertEquals(msg5.getSignatureBlob().getLeaf(),5);
		
		assertEquals(0,msg0.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg1.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg2.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg3.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg4.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg5.getSignatureBlob().getSpliceHintCount());

		/* Batch 2 */
		signqueue.add(msg6);
		signqueue.add(msg7);
		signqueue.add(msg8);
		signqueue.add(msg9);
		signqueue.process();
		assertEquals(2,prims.signcount);

		assertEquals(msg6.getSignatureBlob().getLeaf(),6);
		assertEquals(msg7.getSignatureBlob().getLeaf(),7);
		assertEquals(msg8.getSignatureBlob().getLeaf(),8);
		assertEquals(msg9.getSignatureBlob().getLeaf(),9);

		assertEquals(5,msg6.getSignatureBlob().getSpliceHint(0));
		assertEquals(5,msg7.getSignatureBlob().getSpliceHint(0));
		assertEquals(0,msg8.getSignatureBlob().getSpliceHintCount());
		assertEquals(5,msg9.getSignatureBlob().getSpliceHint(0));
	}
	@Test
	public void testVerify() {
		DigestPrimitive prims = new DigestPrimitive();
		HistoryQueue signqueue = new HistoryQueue(prims);
		
		Object target1 = new Object();
		Object target2 = new Object();
		Object target3 = new Object();
		Object target4 = new Object();

		MessageWrap msg0 = new MessageWrap(1000).setRecipient(target1);
		MessageWrap msg1 = new MessageWrap(1001).setRecipient(target1);
		MessageWrap msg2 = new MessageWrap(1002).setRecipient(target2);
		MessageWrap msg3 = new MessageWrap(1003).setRecipient(target3);
		MessageWrap msg4 = new MessageWrap(1004).setRecipient(target2);
		MessageWrap msg5 = new MessageWrap(1005).setRecipient(target1);

		MessageWrap msg6 = new MessageWrap(1006).setRecipient(target3);
		MessageWrap msg7 = new MessageWrap(1007).setRecipient(target2);
		MessageWrap msg8 = new MessageWrap(1008).setRecipient(target4);
		MessageWrap msg9 = new MessageWrap(1009).setRecipient(target1);

		signqueue.add(msg0);
		signqueue.add(msg1);
		signqueue.add(msg2);
		signqueue.add(msg3);
		signqueue.add(msg4);
		signqueue.add(msg5);
		signqueue.process();

		signqueue.add(msg6);
		signqueue.add(msg7);
		signqueue.add(msg8);
		signqueue.add(msg9);
		signqueue.process();

		// Now try to verify them, in one batch.
		System.out.println("***** Verify Pass 1 *****");
		VerifyQueue verifyqueue = new VerifyQueue(prims);
		
		msg0.wantValid(); verifyqueue.add(msg0);
		msg1.wantValid(); verifyqueue.add(msg1);
		msg2.wantValid(); verifyqueue.add(msg2);
		msg3.wantValid(); verifyqueue.add(msg3);
		msg4.wantValid(); verifyqueue.add(msg4);
		msg5.wantValid(); verifyqueue.add(msg5);
		msg6.wantValid(); verifyqueue.add(msg6);
		msg7.wantValid(); verifyqueue.add(msg7);
		msg8.wantValid(); verifyqueue.add(msg8);
		msg9.wantValid(); verifyqueue.add(msg9);
		verifyqueue.process();
		assertEquals(1,prims.verifycount); // Only one tree-ID.
		
		// Now try to verify them, in more than one batch.
		System.out.println("***** Verify Pass 2 *****");
		prims.reset();
		assertEquals(0,prims.verifycount); // One for msg8, to a new recipient_host.
		verifyqueue = new VerifyQueue(prims);
		
		msg0.wantValid(); verifyqueue.add(msg0);
		msg1.wantValid(); verifyqueue.add(msg1);
		msg2.wantValid(); verifyqueue.add(msg2);
		msg3.wantValid(); verifyqueue.add(msg3);
		msg4.wantValid(); verifyqueue.add(msg4);
		verifyqueue.process();
		assertEquals(1,prims.verifycount); // One for msg8, to a new recipient_host.
		
		// Corrupt message 5.
		msg5.data[1]=0;
	
		msg5.wantInValid(); verifyqueue.add(msg5);
		msg6.wantValid(); verifyqueue.add(msg6);
		msg7.wantValid(); verifyqueue.add(msg7);
		msg8.wantValid(); verifyqueue.add(msg8);
		msg9.wantValid(); verifyqueue.add(msg9);
		verifyqueue.process();
	}

	public void testVerify2() {
		DigestPrimitive prims = new DigestPrimitive();
		HistoryQueue signqueue = new HistoryQueue(prims);
		
		Object targets[] = new Object[20];
		for (int i=0 ; i < targets.length ; i++) {
			targets[i] = new Object();
		}

		MessageWrap msgA[] = {
				new MessageWrap(1000).setRecipient(targets[1]), // A Two messages in the same group.
				new MessageWrap(1001).setRecipient(targets[2]), // B Two messages in consecutive groups.
				new MessageWrap(1002).setRecipient(targets[3]), // C Two messages in non-consecutive groups.
				new MessageWrap(1003).setRecipient(targets[4]), 
				new MessageWrap(1004).setRecipient(targets[1]), // A Two messages in the same group. 
		};

		MessageWrap msgB[] = {
				// TODO: This is very confusing - 
				// is the repeat of message with data 1004 intentional 
				// is this supposed to be treated as a single message
				// accidentally sent twice or a repeated message 
				new MessageWrap(1004).setRecipient(targets[2]), // B
				new MessageWrap(1005).setRecipient(targets[4]), // D
				new MessageWrap(1006).setRecipient(targets[0]), 
				new MessageWrap(1007).setRecipient(targets[0])
		};

		MessageWrap msgC[] = {
				new MessageWrap(1008).setRecipient(targets[3]), // C
				new MessageWrap(1009).setRecipient(targets[4]), // D
				new MessageWrap(1008).setRecipient(targets[0]), 
				new MessageWrap(1009).setRecipient(targets[0]), 
		};
		
		MessageWrap msgD[] = {
				new MessageWrap(1008).setRecipient(targets[3]), // C
				new MessageWrap(1009).setRecipient(targets[4]), // D
				new MessageWrap(1008).setRecipient(targets[0]), 
				new MessageWrap(1009).setRecipient(targets[0]), 
		};
		
		playBatch(signqueue, msgA);
		playBatch(signqueue, msgB);
		playBatch(signqueue, msgC);
		
		VerifyQueue verify;
		
		prims.reset();
		verify = new VerifyQueue(prims);
		verify.add(msgA[0]); msgA[0].wantValid();
		verify.process();
		assertEquals(1,prims.verifycount); // One redundant hash; two in same group.
		
		prims.reset();
		verify = new VerifyQueue(prims);
		verify.add(msgA[0]); msgA[0].wantValid();
		verify.add(msgA[4]); msgA[4].wantValid();
		verify.process();
		assertEquals(1,prims.verifycount); // One redundant hash; two in same group.
		
		// This should splice together, only requiring one signature.
		prims.reset();
		verify = new VerifyQueue(prims);
		verify.add(msgA[1]); msgA[1].wantValid();
		verify.add(msgB[0]); msgB[0].wantValid();
		verify.process();
		assertEquals(1,prims.verifycount);

		// This should splice together, only requiring one signature.
		prims.reset();
		verify = new VerifyQueue(prims);
		verify.add(msgA[2]); msgA[2].wantValid();
		verify.add(msgC[0]); msgC[0].wantValid();
		verify.process();
		assertEquals(1,prims.verifycount); // One redundant hash; two in same group.
		
		// TODO: This should not splice together, requiring two signatures.
		prims.reset();
		verify = new VerifyQueue(prims);
		verify.add(msgA[1]); msgA[1].wantValid();
		verify.add(msgC[1]); msgC[1].wantValid();
		verify.process();
		assertEquals(2,prims.verifycount); // One redundant hash; two in same group.
		
		// TODO: split this off into a separate test case, Crosby just left this incomplete
		// Alternate version, to test bad hashes histories, but good (local) signatures.
		MessageWrap msgAalt[] = {
				new MessageWrap(2000).setRecipient(targets[1]),
				new MessageWrap(2001).setRecipient(targets[1]),
				new MessageWrap(2002).setRecipient(targets[2]),
				new MessageWrap(2003).setRecipient(targets[3]),
				new MessageWrap(2003).setRecipient(targets[4]), 
		};
		
	}
	static void playBatch(HistoryQueue queue, MessageWrap msg[]) {
		play(queue,msg);
		queue.process();
	}
	static void play(HistoryQueue queue, MessageWrap msg[]) {
		for (int i = 0 ; i < msg.length ; i++) {
			queue.add(msg[i]);
		}
	}
}

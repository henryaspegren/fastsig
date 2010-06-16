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

package edu.rice.batchsig;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.Builder;

/** Wraps a signer with one that caches signature verifications so that the same signature and data need only be verified once */
public class CachingSigner implements SignaturePrimitives {
	final private SignaturePrimitives orig;

	CachingSigner(SignaturePrimitives orig) {
		this.orig = orig;
	}

	@Override
	public void sign(byte[] data, Builder out) {
		orig.sign(data,out);
		
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		// TODO Implement the cache.
		return orig.verify(data,sig);
	}

}

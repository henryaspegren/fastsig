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

package edu.rice.batchsig.bench;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;


import com.google.protobuf.ByteString;

import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.SignatureAlgorithm;

public class PublicKeyPrims implements SignaturePrimitives {
	
	byte signer_id_bytes[];
	ByteString signer_id;
	Signature signer, verifier;
	SignatureAlgorithm sigalgo;
	KeyPair kp;
	
	private PublicKeyPrims() {
	}
	
	
	/** Make a given key with the default provider
	 *
	 * @param signer_id_string ID used to denote the signer principal.
	 * @param algo Algorithm to use (e.g. "sha1withdsa")
	 * @param size Number of bits in the key
	 * @return A primitive object.
	 */
	public static PublicKeyPrims make(String signer_id_string, String algo, int size) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		return make(signer_id_string, algo, size, null);
	}

	/** Make a given key
	 * 
	 * @param signer_id_string ID used to denote the signer principal.
	 * @param algo Algorithm to use (e.g. "sha1withdsa")
	 * @param size Number of bits in the key
	 * @param provider Which provider (May be null)
	 * @return A primitive object.
	 */
	public static PublicKeyPrims make(String signer_id_string, String algo, int size, String provider) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException  {
		PublicKeyPrims out = new PublicKeyPrims();
		System.out.format("MakingPublicKeyPrims for signer=%s and algo=%s\n",signer_id_string,algo);
		//System.out.println("ALGO2 " + algo.substring(algo.length()-3));
		
		out.signer_id_bytes = signer_id_string.getBytes();
		out.signer_id = ByteString.copyFrom(out.signer_id_bytes);

		// If we already have it....
		if (out.load(signer_id_string,algo,size,provider))
			return out;

		System.err.println("Making new keypair with algo: '"+algo+"' and provider '"+provider+"'");
		
		// For key generation, only 'RSA' or 'DSA'
		String kpg_algo = algo.substring(algo.length()-3);
		
		// Otherwise, generate it.
		KeyPairGenerator kpg;
		if (provider != null) 
			kpg = KeyPairGenerator.getInstance(kpg_algo,provider);
		else
			kpg = KeyPairGenerator.getInstance(kpg_algo);

		kpg.initialize(size);
		out.initialize(kpg.genKeyPair(),algo, size,provider);
		out.save(makeFilename(signer_id_string,algo,size));
		return out;
	}
		
	static String makeFilename(String signer_id_string, String algo, int size) {
		return String.format("keys/KEY.%s-%s-%d.key",signer_id_string,algo,size);
	}
	
	void initialize(KeyPair kp, String algo, int size, String provider) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		this.kp = kp;
		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();

		if (provider != null) {
			signer = Signature.getInstance(algo,provider);
			verifier = Signature.getInstance(algo,provider);
		} else {
			signer = Signature.getInstance(algo);
			verifier = Signature.getInstance(algo);
		}
		signer.initSign(privateKey);
		verifier.initVerify(publicKey);

		if (algo.toLowerCase().equals("sha1withrsa")) {
			sigalgo = SignatureAlgorithm.SHA1_RSA;
		} else if (algo.toLowerCase().equals("sha1withdsa")) {
			sigalgo = SignatureAlgorithm.SHA1_DSA;
		} else if (algo.toLowerCase().equals("sha256withrsa")) {
			sigalgo = SignatureAlgorithm.SHA1_RSA;
		} else if (algo.toLowerCase().equals("sha256withdsa")) {
			sigalgo = SignatureAlgorithm.SHA1_DSA;
		} else {
			throw new Error("Unknown signature algorithm");
		}
	}
	
	public void save(String filename) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
			out.writeObject(kp);
			out.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public boolean load(String signer_id_string, String algo, int size, String provider) {
		try {
			ObjectInputStream input = new ObjectInputStream(new FileInputStream(makeFilename(signer_id_string,algo,size)));
			initialize((KeyPair)input.readObject(),algo,size,provider);
			input.close();
			return true;
		} catch (IOException e) {
			// If it fails, just generate it.
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		try {
		out.setSignatureAlgorithm(sigalgo);
		out.setSignatureBytes(ByteString.copyFrom(signBytes(data)));
		out.setSignerId(signer_id);
		} catch (SignatureException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		if (sig.getSignatureAlgorithm() != sigalgo) {
			System.out.println("Info: Mismatched signature algorithms");
			return false;
		}
		if (!Arrays.equals(signer_id_bytes, sig.getSignerId().toByteArray())) {
			System.out.println("Info: Mismatched signerids "+signer_id.toStringUtf8()+"!="+sig.getSignerId().toStringUtf8());
			return false;
		}
		try {
			return verifyBytes(data,sig.getSignatureBytes().toByteArray());
		} catch (SignatureException e) {
			e.printStackTrace();
			return false;
		}
	}


	/** Queue of verified signatures */
	Queue<ByteString> expirationQueue;

	/** Map from data bytes to signature bytes */
	class Cache extends LinkedHashMap<ByteString,ByteString> {
		final int size_limit;
		Cache(int size_limit) {
			this.size_limit = size_limit;
		}
		protected boolean removeEldestEntry(Map.Entry<ByteString,ByteString> eldest) {
			return this.size() > size_limit;
		}
		boolean hasValidSignature(byte [] databytes, byte [] sigbytes) {
			ByteString sig = ByteString.copyFrom(sigbytes);
			ByteString data = ByteString.copyFrom(databytes);
			return sig.equals(this.get(data));
		}
		void addSignature(byte [] databytes, byte [] sigbytes) {
			ByteString sig = ByteString.copyFrom(sigbytes);
			ByteString data = ByteString.copyFrom(databytes);
			this.put(data, sig);
		}
	}

	Cache cache;
	
	static final private int CACHE_SIZE = 100;
	
	public void activateCache() {
		cache = new Cache(CACHE_SIZE);
	}
		
	protected boolean verifyBytes(byte [] databytes, byte [] sigbytes) throws SignatureException {
		if (cache != null && cache.hasValidSignature(databytes,sigbytes)) {
			if (++Tracker.singleton.verifycount_cached % 100 == 0)
				System.err.println("Verifycount_cached: "+Tracker.singleton.verifycount_cached);			
			return true;
		}
		if (++Tracker.singleton.verifycount % 1000 == 0) {
			System.err.println("Verifycount: "+Tracker.singleton.cryptoreport());
			System.out.println("Verifycount: "+Tracker.singleton.cryptoreport());
		}
		verifier.update(databytes);
		boolean isValid = verifier.verify(sigbytes);
		
		if (cache != null && isValid) {
			cache.addSignature(databytes,sigbytes);
		}
		return isValid;
	}

	protected byte[] signBytes(byte [] databytes) throws SignatureException {
		if (++Tracker.singleton.signcount % 1000 == 0)
			System.err.println("SIgncount: "+Tracker.singleton.signcount);
		signer.update(databytes);
		return signer.sign();
	}
}

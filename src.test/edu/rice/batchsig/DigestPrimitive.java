package edu.rice.batchsig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import com.google.protobuf.ByteString;

import edu.rice.historytree.generated.Serialization.TreeSigBlob;
import edu.rice.historytree.generated.Serialization.TreeSigBlob.SignatureAlgorithm;


/** 'Sign' a string by computing a digest over it. 
 * 
 * Use this for testing. 
 * For testing.
 * 
 * @author crosby
 *
 */
public class DigestPrimitive implements SignaturePrimitives {
	/** Trackthe number of signs and verifies, for unittesting */
	int signcount = 0, verifycount = 0;
	
	void reset() {
		signcount = 0;
		verifycount = 0;
	}
	
	static public byte[] hash(byte[] data) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(data);
			return Base64.encodeBase64(md.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void sign(byte[] data, TreeSigBlob.Builder out) {
		signcount++;
		out.setSignatureAlgorithm(SignatureAlgorithm.TEST_DIGEST);

		//System.out.println("Signing '"+new String(data)+"' with "+new String(hash(data)));
		
		out.setSignatureBytes(ByteString.copyFrom(hash(data)));
	}

	@Override
	public boolean verify(byte[] data, TreeSigBlob sig) {
		verifycount++;
		//System.out.println("Validating '"+new String(data)+"' : "+new String(hash(data)) + "  == " + new String(sig.getSignatureBytes().toByteArray()));
		if (sig.getSignatureAlgorithm() == SignatureAlgorithm.TEST_DIGEST)
			return Arrays.equals(hash(data),sig.getSignatureBytes().toByteArray());
		System.out.println("A non-'test' signature sent under test");
		return false;
	}

}

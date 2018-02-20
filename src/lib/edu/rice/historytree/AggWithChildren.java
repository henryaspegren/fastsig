package edu.rice.historytree;

/**
 * Helper class representing an aggregate along its right 
 * and left child aggregations (which may be null). 
 * This is useful because the data in the left and right 
 * might be required for proofs or computation.
 * @author henryaspegren
 *
 * @param <T>
 */
public class AggWithChildren<T>{
	private final T mainAgg;
	private final T leftAgg;
	private final T rightAgg;
	
	
	public AggWithChildren(T mainAgg, T leftAgg, T rightAgg) {
		this.mainAgg = mainAgg;
		this.leftAgg = leftAgg;
		this.rightAgg = rightAgg;
	}
	
	public T getMain() {
		return this.mainAgg;
	}
	
	public T getLeft() {
		return this.leftAgg;
	}
	
	public T getRight() {
		return this.rightAgg;
	}
	
};
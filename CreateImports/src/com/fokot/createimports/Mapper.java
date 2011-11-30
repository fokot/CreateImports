package com.fokot.createimports;

import java.util.ArrayList;
import java.util.List;

/**
 * Map function - maps objects of type A to type B
 */
abstract class Mapper<A, B> {

	/**
	 * mapping
	 */
	public abstract B map(A a);

	/**
	 * Maps collection of objects of type A to collection of type B objects by the mapping function
	 */
	public static <A, B> List<B> map(List<A> l, Mapper<A,B> m){
		List<B> r = new ArrayList<B>(l.size());
		for(A a : l) {
			r.add(m.map(a));
		}
		return r;
	}
}
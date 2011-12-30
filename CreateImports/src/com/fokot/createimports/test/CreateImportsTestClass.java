package com.fokot.createimports.test;

// will be replaced by simple import - used
import java.math.*;

// will be replaced by simple import - used
import java.io.*;

// will be replaced by simple import - used
import java.util.*;

// will be kept - used
import java.util.Collection;

// generic parameter and superclass will be simplified
public class CreateImportsTestClass<T extends java.io.InputStream> extends java.io.BufferedInputStream {


	// will be fixed
	private java.util.Collection c;

	// will be fixed
	private Collection<java.io.InputStream> cc;

	public CreateImportsTestClass(InputStream in) {
		super(in);

		BigDecimal db = null;

		// will be fixed
		java.math.BigDecimal db2 = null;

		// will be kept - no fixing needed
		OutputStream os = null;

		// will be kept - source contains java.math.BigDecimal
		com.fokot.createimports.test.bd.BigDecimal.class.getClass();

		// will be kept - no fixing needed
		ArrayList<String> o = null;

		// will be kept and no import will be created because is from java.lang package
		Long l = new Long(5);

		// will be fixed - all Map interface, generic parameter and HashMap
		java.util.Map<String, java.lang.Long> s = new java.util.HashMap<String, Long>();

		// will be fixed - no import created because it is from java.lang.package
		java.lang.String str;

		// will be kept - because there is integer in this package
		java.lang.Integer i;

		// will be fixed
		java.util.Arrays.deepHashCode(null);
	}

	// will be fixed
	protected void haluza(java.io.BufferedInputStream i){
	}
}
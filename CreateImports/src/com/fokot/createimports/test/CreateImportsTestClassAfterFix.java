package com.fokot.createimports.test;

//will be kept - used
import java.util.Collection;

import java.io.InputStream;
import java.math.BigDecimal;
import java.io.OutputStream;
import java.util.ArrayList;
import java.lang.String;
import java.lang.Long;
import java.io.BufferedInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

//generic parameter and superclass will be simplified
public class CreateImportsTestClassAfterFix<T extends InputStream> extends BufferedInputStream {


	// will be fixed
	private Collection c;

	// will be fixed
	private Collection<InputStream> cc;

	public CreateImportsTestClassAfterFix(InputStream in) {
		super(in);

		BigDecimal db = null;

		// will be fixed
		BigDecimal db2 = null;

		// will be kept - no fixing needed
		OutputStream os = null;

		// will be kept - source contains java.math.BigDecimal
		com.fokot.createimports.test.bd.BigDecimal.class.getClass();

		// will be kept - no fixing needed
		ArrayList<String> o = null;

		// will be kept and no import will be created because is from java.lang package
		Long l = new Long(5);

		// will be fixed - all Map interface, generic parameter and HashMap
		Map<String, Long> s = new HashMap<String, Long>();

		// will be fixed - no import created because it is from java.lang.package
		String str;

		// will be kept - because there is integer in this package
		java.lang.Integer i;

		// will be fixed
		Arrays.deepHashCode(null);
	}

	// will be fixed
	protected void haluza(BufferedInputStream i){
	}
}
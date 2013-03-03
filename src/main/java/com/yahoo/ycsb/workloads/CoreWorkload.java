/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.workloads;

import java.util.Properties;
import com.yahoo.ycsb.*;
import com.yahoo.ycsb.db.MagicKey;
import com.yahoo.ycsb.generator.CounterGenerator;
import com.yahoo.ycsb.generator.DiscreteGenerator;
import com.yahoo.ycsb.generator.Generator;
import com.yahoo.ycsb.generator.ConstantIntegerGenerator;
import com.yahoo.ycsb.generator.HotspotIntegerGenerator;
import com.yahoo.ycsb.generator.HistogramGenerator;
import com.yahoo.ycsb.generator.IntegerGenerator;
import com.yahoo.ycsb.generator.ScrambledZipfianGenerator;
import com.yahoo.ycsb.generator.SkewedLatestGenerator;
import com.yahoo.ycsb.generator.UniformIntegerGenerator;
import com.yahoo.ycsb.generator.ZipfianGenerator;
import com.yahoo.ycsb.measurements.Measurements;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * The core benchmark scenario. Represents a set of clients doing simple CRUD operations. The relative 
 * proportion of different kinds of operations, and other properties of the workload, are controlled
 * by parameters specified at runtime.
 * 
 * Properties to control the client:
 * <UL>
 * <LI><b>fieldcount</b>: the number of fields in a record (default: 10)
 * <LI><b>fieldlength</b>: the size of each field (default: 100)
 * <LI><b>readallfields</b>: should reads read all fields (true) or just one (false) (default: true)
 * <LI><b>writeallfields</b>: should updates and read/modify/writes update all fields (true) or just one (false) (default: false)
 * <LI><b>readproportion</b>: what proportion of operations should be reads (default: 0.95)
 * <LI><b>updateproportion</b>: what proportion of operations should be updates (default: 0.05)
 * <LI><b>insertproportion</b>: what proportion of operations should be inserts (default: 0)
 * <LI><b>scanproportion</b>: what proportion of operations should be scans (default: 0)
 * <LI><b>readmodifywriteproportion</b>: what proportion of operations should be read a record, modify it, write it back (default: 0)
 * <LI><b>requestdistribution</b>: what distribution should be used to select the records to operate on - uniform, zipfian, hotspot, or latest (default: uniform)
 * <LI><b>maxscanlength</b>: for scans, what is the maximum number of records to scan (default: 1000)
 * <LI><b>scanlengthdistribution</b>: for scans, what distribution should be used to choose the number of records to scan, for each scan, between 1 and maxscanlength (default: uniform)
 * <LI><b>insertorder</b>: should records be inserted in order by key ("ordered"), or in hashed order ("hashed") (default: hashed)
 * </ul> 
 */
public class CoreWorkload extends Workload
{

	/**
	 * The name of the database table to run queries against.
	 */
	public static final String TABLENAME_PROPERTY="table";

	/**
	 * The default name of the database table to run queries against.
	 */
	public static final String TABLENAME_PROPERTY_DEFAULT="usertable";

	public static String table;


	/**
	 * The name of the property for the number of fields in a record.
	 */
	public static final String FIELD_COUNT_PROPERTY="fieldcount";
	
	/**
	 * Default number of fields in a record.
	 */
	public static final String FIELD_COUNT_PROPERTY_DEFAULT="10";

	int fieldcount;

	/**
	 * The name of the property for the field length distribution. Options are "uniform", "zipfian" (favoring short records), "constant", and "histogram".
	 * 
	 * If "uniform", "zipfian" or "constant", the maximum field length will be that specified by the fieldlength property.  If "histogram", then the
	 * histogram will be read from the filename specified in the "fieldlengthhistogram" property.
	 */
	public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY="fieldlengthdistribution";
	/**
	 * The default field length distribution.
	 */
	public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "constant";

	/**
	 * The name of the property for the length of a field in bytes.
	 */
	public static final String FIELD_LENGTH_PROPERTY="fieldlength";
	/**
	 * The default maximum length of a field in bytes.
	 */
	public static final String FIELD_LENGTH_PROPERTY_DEFAULT="100";

	/**
	 * The name of a property that specifies the filename containing the field length histogram (only used if fieldlengthdistribution is "histogram").
	 */
	public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY = "fieldlengthhistogram";
	/**
	 * The default filename containing a field length histogram.
	 */
	public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT = "hist.txt";

	/**
	 * Generator object that produces field lengths.  The value of this depends on the properties that start with "FIELD_LENGTH_".
	 */
	IntegerGenerator fieldlengthgenerator;
	
	/**
	 * The name of the property for deciding whether to read one field (false) or all fields (true) of a record.
	 */
	public static final String READ_ALL_FIELDS_PROPERTY="readallfields";
	
	/**
	 * The default value for the readallfields property.
	 */
	public static final String READ_ALL_FIELDS_PROPERTY_DEFAULT="true";

	boolean readallfields;

	/**
	 * The name of the property for deciding whether to write one field (false) or all fields (true) of a record.
	 */
	public static final String WRITE_ALL_FIELDS_PROPERTY="writeallfields";
	
	/**
	 * The default value for the writeallfields property.
	 */
	public static final String WRITE_ALL_FIELDS_PROPERTY_DEFAULT="false";

	boolean writeallfields;


	/**
	 * The name of the property for the proportion of transactions that are reads.
	 */
	public static final String READ_PROPORTION_PROPERTY="readproportion";
	
	/**
	 * The default proportion of transactions that are reads.	
	 */
	public static final String READ_PROPORTION_PROPERTY_DEFAULT="0.95";

	/**
	 * The name of the property for the proportion of transactions that are updates.
	 */
	public static final String UPDATE_PROPORTION_PROPERTY="updateproportion";
	
	/**
	 * The default proportion of transactions that are updates.
	 */
	public static final String UPDATE_PROPORTION_PROPERTY_DEFAULT="0.05";

	/**
	 * The name of the property for the proportion of transactions that are inserts.
	 */
	public static final String INSERT_PROPORTION_PROPERTY="insertproportion";
	
	/**
	 * The default proportion of transactions that are inserts.
	 */
	public static final String INSERT_PROPORTION_PROPERTY_DEFAULT="0.0";

	/**
	 * The name of the property for the proportion of transactions that are scans.
	 */
	public static final String SCAN_PROPORTION_PROPERTY="scanproportion";
	
	/**
	 * The default proportion of transactions that are scans.
	 */
	public static final String SCAN_PROPORTION_PROPERTY_DEFAULT="0.0";
	
	/**
	 * The name of the property for the proportion of transactions that are read-modify-write.
	 */
	public static final String READMODIFYWRITE_PROPORTION_PROPERTY="readmodifywriteproportion";
	
	/**
	 * The default proportion of transactions that are scans.
	 */
	public static final String READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT="0.0";
	
	/**
	 * The name of the property for the the distribution of requests across the keyspace. Options are "uniform", "zipfian" and "latest"
	 */
	public static final String REQUEST_DISTRIBUTION_PROPERTY="requestdistribution";
	
	/**
	 * The default distribution of requests across the keyspace
	 */
	public static final String REQUEST_DISTRIBUTION_PROPERTY_DEFAULT="uniform";

	/**
	 * The name of the property for the max scan length (number of records)
	 */
	public static final String MAX_SCAN_LENGTH_PROPERTY="maxscanlength";
	
	/**
	 * The default max scan length.
	 */
	public static final String MAX_SCAN_LENGTH_PROPERTY_DEFAULT="1000";
	
	/**
	 * The name of the property for the scan length distribution. Options are "uniform" and "zipfian" (favoring short scans)
	 */
	public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY="scanlengthdistribution";
	
	/**
	 * The default max scan length.
	 */
	public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT="uniform";
	
	/**
	 * The name of the property for the order to insert records. Options are "ordered" or "hashed"
	 */
	public static final String INSERT_ORDER_PROPERTY="insertorder";
	
	/**
	 * Default insert order.
	 */
	public static final String INSERT_ORDER_PROPERTY_DEFAULT="hashed";
	
	/**
   * Percentage data items that constitute the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION = "hotspotdatafraction";
  
  /**
   * Default value of the size of the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION_DEFAULT = "0.2";
  
  /**
   * Percentage operations that access the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION = "hotspotopnfraction";
  
  /**
   * Default value of the percentage operations accessing the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION_DEFAULT = "0.8";
	
	IntegerGenerator keysequence;

	DiscreteGenerator operationchooser;

	IntegerGenerator keychooser;

	Generator fieldchooser;

	CounterGenerator transactioninsertkeysequence;
	
	IntegerGenerator scanlength;
	
	boolean orderedinserts;

	int recordcount;
	
	protected static IntegerGenerator getFieldLengthGenerator(Properties p) throws WorkloadException{
		IntegerGenerator fieldlengthgenerator;
		String fieldlengthdistribution = p.getProperty(FIELD_LENGTH_DISTRIBUTION_PROPERTY, FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);
		int fieldlength=Integer.parseInt(p.getProperty(FIELD_LENGTH_PROPERTY,FIELD_LENGTH_PROPERTY_DEFAULT));
		String fieldlengthhistogram = p.getProperty(FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY, FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT);
		if(fieldlengthdistribution.compareTo("constant") == 0) {
			fieldlengthgenerator = new ConstantIntegerGenerator(fieldlength);
		} else if(fieldlengthdistribution.compareTo("uniform") == 0) {
			fieldlengthgenerator = new UniformIntegerGenerator(1, fieldlength);
		} else if(fieldlengthdistribution.compareTo("zipfian") == 0) {
			fieldlengthgenerator = new ZipfianGenerator(1, fieldlength);
		} else if(fieldlengthdistribution.compareTo("histogram") == 0) {
			try {
				fieldlengthgenerator = new HistogramGenerator(fieldlengthhistogram);
			} catch(IOException e) {
				throw new WorkloadException("Couldn't read field length histogram file: "+fieldlengthhistogram, e);
			}
		} else {
			throw new WorkloadException("Unknown field length distribution \""+fieldlengthdistribution+"\"");
		}
		return fieldlengthgenerator;
	}
	
	/**
	 * Initialize the scenario. 
	 * Called once, in the main client thread, before any operations are started.
	 */
	public void init(Properties p) throws WorkloadException
	{
	    
		table = p.getProperty(TABLENAME_PROPERTY,TABLENAME_PROPERTY_DEFAULT);
		
		fieldcount=Integer.parseInt(p.getProperty(FIELD_COUNT_PROPERTY,FIELD_COUNT_PROPERTY_DEFAULT));
		fieldlengthgenerator = CoreWorkload.getFieldLengthGenerator(p);
		
		double readproportion=Double.parseDouble(p.getProperty(READ_PROPORTION_PROPERTY,READ_PROPORTION_PROPERTY_DEFAULT));
		double updateproportion=Double.parseDouble(p.getProperty(UPDATE_PROPORTION_PROPERTY,UPDATE_PROPORTION_PROPERTY_DEFAULT));
		double insertproportion=Double.parseDouble(p.getProperty(INSERT_PROPORTION_PROPERTY,INSERT_PROPORTION_PROPERTY_DEFAULT));
		double scanproportion=Double.parseDouble(p.getProperty(SCAN_PROPORTION_PROPERTY,SCAN_PROPORTION_PROPERTY_DEFAULT));
		double readmodifywriteproportion=Double.parseDouble(p.getProperty(READMODIFYWRITE_PROPORTION_PROPERTY,READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT));
		recordcount=Integer.parseInt(p.getProperty(Client.RECORD_COUNT_PROPERTY));
		String requestdistrib=p.getProperty(REQUEST_DISTRIBUTION_PROPERTY,REQUEST_DISTRIBUTION_PROPERTY_DEFAULT);
		int maxscanlength=Integer.parseInt(p.getProperty(MAX_SCAN_LENGTH_PROPERTY,MAX_SCAN_LENGTH_PROPERTY_DEFAULT));
		String scanlengthdistrib=p.getProperty(SCAN_LENGTH_DISTRIBUTION_PROPERTY,SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);
		
		int insertstart=Integer.parseInt(p.getProperty(INSERT_START_PROPERTY,INSERT_START_PROPERTY_DEFAULT));
		
		readallfields=Boolean.parseBoolean(p.getProperty(READ_ALL_FIELDS_PROPERTY,READ_ALL_FIELDS_PROPERTY_DEFAULT));
		writeallfields=Boolean.parseBoolean(p.getProperty(WRITE_ALL_FIELDS_PROPERTY,WRITE_ALL_FIELDS_PROPERTY_DEFAULT));
		
		if (p.getProperty(INSERT_ORDER_PROPERTY,INSERT_ORDER_PROPERTY_DEFAULT).compareTo("hashed")==0)
		{
			orderedinserts=false;
		}
		else
		{
			orderedinserts=true;
		}

		keysequence=new CounterGenerator(insertstart);
		operationchooser=new DiscreteGenerator();
		if (readproportion>0)
		{
			operationchooser.addValue(readproportion,"READ");
		}

		if (updateproportion>0)
		{
			operationchooser.addValue(updateproportion,"UPDATE");
		}

		if (insertproportion>0)
		{
			operationchooser.addValue(insertproportion,"INSERT");
		}
		
		if (scanproportion>0)
		{
			operationchooser.addValue(scanproportion,"SCAN");
		}
		
		if (readmodifywriteproportion>0)
		{
			operationchooser.addValue(readmodifywriteproportion,"READMODIFYWRITE");
		}

		transactioninsertkeysequence=new CounterGenerator(recordcount);
		if (requestdistrib.compareTo("uniform")==0)
		{
			keychooser=new UniformIntegerGenerator(0,recordcount-1);
		}
		else if (requestdistrib.compareTo("zipfian")==0)
		{
			//it does this by generating a random "next key" in part by taking the modulus over the number of keys
			//if the number of keys changes, this would shift the modulus, and we don't want that to change which keys are popular
			//so we'll actually construct the scrambled zipfian generator with a keyspace that is larger than exists at the beginning
			//of the test. that is, we'll predict the number of inserts, and tell the scrambled zipfian generator the number of existing keys
			//plus the number of predicted keys as the total keyspace. then, if the generator picks a key that hasn't been inserted yet, will
			//just ignore it and pick another key. this way, the size of the keyspace doesn't change from the perspective of the scrambled zipfian generator
			
			int opcount=Integer.parseInt(p.getProperty(Client.OPERATION_COUNT_PROPERTY));
			int expectednewkeys=(int)(((double)opcount)*insertproportion*2.0); //2 is fudge factor
			
			keychooser=new ScrambledZipfianGenerator(recordcount+expectednewkeys);
		}
		else if (requestdistrib.compareTo("latest")==0)
		{
			keychooser=new SkewedLatestGenerator(transactioninsertkeysequence);
		}
		else if (requestdistrib.equals("hotspot")) 
		{
      double hotsetfraction = Double.parseDouble(p.getProperty(
          HOTSPOT_DATA_FRACTION, HOTSPOT_DATA_FRACTION_DEFAULT));
      double hotopnfraction = Double.parseDouble(p.getProperty(
          HOTSPOT_OPN_FRACTION, HOTSPOT_OPN_FRACTION_DEFAULT));
      keychooser = new HotspotIntegerGenerator(0, recordcount - 1, 
          hotsetfraction, hotopnfraction);
    }
		else
		{
			throw new WorkloadException("Unknown request distribution \""+requestdistrib+"\"");
		}

		fieldchooser=new UniformIntegerGenerator(0,fieldcount-1);
		
		if (scanlengthdistrib.compareTo("uniform")==0)
		{
			scanlength=new UniformIntegerGenerator(1,maxscanlength);
		}
		else if (scanlengthdistrib.compareTo("zipfian")==0)
		{
			scanlength=new ZipfianGenerator(1,maxscanlength);
		}
		else
		{
			throw new WorkloadException("Distribution \""+scanlengthdistrib+"\" not allowed for scan length");
		}
	}

	/**
	 * Do one insert operation. Because it will be called concurrently from multiple client threads, this 
	 * function must be thread safe. However, avoid synchronized, or the threads will block waiting for each 
	 * other, and it will be difficult to reach the target throughput. Ideally, this function would have no side
	 * effects other than DB operations.
	 */
	public boolean doInsert(DB db, Object threadstate)
	{
		int keynum=keysequence.nextInt();
//		if (!orderedinserts)
//		{
//			keynum=Utils.hash(keynum);
//		}
		String dbkey="user"+keynum;
		HashMap<String,ByteIterator> values=new HashMap<String,ByteIterator>();

		for (int i=0; i<fieldcount; i++)
		{
			String fieldkey="field"+i;
			ByteIterator data= new RandomByteIterator(fieldlengthgenerator.nextInt());
			values.put(fieldkey,data);
		}
		if (db.insert(table,dbkey,values) == 0)
			return true;
		else
			return false;
	}

	/**
	 * Do one transaction operation. Because it will be called concurrently from multiple client threads, this 
	 * function must be thread safe. However, avoid synchronized, or the threads will block waiting for each 
	 * other, and it will be difficult to reach the target throughput. Ideally, this function would have no side
	 * effects other than DB operations.
	 */
	public int doTransaction(DB db, Object threadstate)
	{
		String op=operationchooser.nextString();
		
		int returnValue = DB.OK;
			
		int committed = DB.OK;
		
		returnValue = db.beginTransaction();
		
		if(returnValue != DB.OK){
		
			return 1;
		}

		if (op.compareTo("READ")==0)
		{
			returnValue = doTransactionRead(db);
		}
		else if (op.compareTo("UPDATE")==0)
		{
		    db.markWriteTx();
			returnValue = doTransactionUpdate(db);
		}
		else if (op.compareTo("INSERT")==0)
		{
		    db.markWriteTx();
			returnValue = doTransactionInsert(db);
		}
		else if (op.compareTo("SCAN")==0)
		{
			returnValue = doTransactionScan(db);
		}
		else
		{
		    int keyRead = -1;
		    int keyWrite = -1;
		    int restarts = 0;
		    do {
			db.markWriteTx();
			returnValue = doTransactionReadModifyWrite(db, keyRead, keyWrite);
			committed = db.endTransaction((returnValue == DB.OK));	    
			if ((returnValue == DB.OK) && (committed == DB.OK)) {
			    return restarts;
			}
			
			restarts++;
			keyRead = lastReadKey.get();
			keyWrite = lastWrittenKey.get();
			returnValue = db.beginTransaction();
			if(returnValue != DB.OK){ throw new RuntimeException("should not have happened"); }
		    } while (true);
		}
		
		committed = db.endTransaction((returnValue == DB.OK));
		
		
		return 0;
	}

	public int doTransactionRead(DB db)
	{
		//choose a random key
		int keynum;
		
		int ret = DB.OK;
		do
		{
			keynum=keychooser.nextInt();
		}
		while (keynum>transactioninsertkeysequence.lastInt());
		
		if (!orderedinserts)
		{
			keynum=Utils.hash(keynum);
		}
		String keyname="user"+keynum;

		HashSet<String> fields=null;

		if (!readallfields)
		{
			//read a random field  
			String fieldname="field"+fieldchooser.nextString();

			fields=new HashSet<String>();
			fields.add(fieldname);
		}

		ret = db.read(new MagicKey(keyname, keynum),fields,new HashMap<String,ByteIterator>());
		
		return ret;
	}
	
	public static int MUL_READ_COUNT;
	
	public int boundKeyToNode(int keynum) {
	    int parcel = MagicKey.NUMBER / MagicKey.CLIENTS;
	    while (keynum < (parcel * Client.NODE_INDEX)) {
	        keynum += parcel;
	    }
	    while (keynum >= (parcel * (Client.NODE_INDEX + 1))) {
	        keynum -= parcel;
	    }
	    return keynum;
	}
	
	private static final ThreadLocal<Integer> lastReadKey = new ThreadLocal<Integer>() {
	    protected Integer initialValue() {
		return -1;
	    };
	};
	
	private static final ThreadLocal<Integer> lastWrittenKey = new ThreadLocal<Integer>() {
	    protected Integer initialValue() {
		return -1;
	    };
	};
	
	public int doTransactionReadModifyWrite(DB db, int forcedKeyNum, int forcedKeyWrite)
	{
		
		int ret = DB.OK;
		
		//choose a random key
		int keynum;
//		if (forcedKeyNum == -1) {
		    do
		    {
			keynum=keychooser.nextInt();
		    }
		    while (keynum>transactioninsertkeysequence.lastInt());
		    lastReadKey.set(keynum);
//		} else {
//		    keynum = forcedKeyNum;
//		}
		
		//choose a random key
		int keyToWrite;
//		if (forcedKeyWrite == -1) {
		    do
		    {
			keyToWrite=keychooser.nextInt();
		    }
		    while (keyToWrite>transactioninsertkeysequence.lastInt());
		    lastWrittenKey.set(keyToWrite);
//		} else {
//		    keyToWrite = forcedKeyWrite;
//		}

//		if (!orderedinserts)
//		{
//			keynum=Utils.hash(keynum);
//			keyToWrite=Utils.hash(keyToWrite);
//		}

		HashSet<String> fields=null;

		if (!readallfields)
		{
			//read a random field  
			String fieldname="field"+fieldchooser.nextString();

			fields=new HashSet<String>();
			fields.add(fieldname);
		}
		
		HashMap<String,ByteIterator> values=new HashMap<String,ByteIterator>();

		if (writeallfields)
		{
		   //new data for all the fields
		   for (int i=0; i<fieldcount; i++)
		   {
		      String fieldname="field"+i;
		      ByteIterator data = new RandomByteIterator(fieldlengthgenerator.nextInt());
		      values.put(fieldname,data);
		   }
		}
		else
		{
		   //update a random field
		   String fieldname="field"+fieldchooser.nextString();
		   ByteIterator data = new RandomByteIterator(fieldlengthgenerator.nextInt());
		   values.put(fieldname,data);
		}

		//do the transaction
		
		long st=System.currentTimeMillis();

		for (int k = 0; k < MUL_READ_COUNT; k++) {
		    int newNum = boundKeyToNode(keynum + k);
		    MagicKey mk = new MagicKey("user"+newNum, newNum);
		    mk.locationCheck();
		    ret = db.read(mk,fields,new HashMap<String,ByteIterator>());
		    
		    
		    if(ret != DB.OK){
			return ret;
		    }
		}
		
		keyToWrite = boundKeyToNode(keyToWrite);
		MagicKey mk = new MagicKey("user"+keyToWrite, keyToWrite);
		mk.locationCheck();
		ret = db.update(mk,values);
		
		if(ret != DB.OK){
			return ret;
		}

		long en=System.currentTimeMillis();
		
		Measurements.getMeasurements().measure("READ-MODIFY-WRITE", (int)(en-st));
		
		return ret;
	}
	
	public int doTransactionScan(DB db)
	{
		
		int ret = DB.OK;

		//choose a random key
		int keynum;
		do
		{
			keynum=keychooser.nextInt();
		}
		while (keynum>transactioninsertkeysequence.lastInt());

//		if (!orderedinserts)
//		{
//			keynum=Utils.hash(keynum);
//		}
		String startkeyname="user"+keynum;
		
		//choose a random scan length
		int len=scanlength.nextInt();

		HashSet<String> fields=null;

		if (!readallfields)
		{
			//read a random field  
			String fieldname="field"+fieldchooser.nextString();

			fields=new HashSet<String>();
			fields.add(fieldname);
		}

		ret = db.scan(table,startkeyname,len,fields,new Vector<HashMap<String,ByteIterator>>());
		
		return ret;
	}

	public int doTransactionUpdate(DB db)
	{
		int ret = DB.OK;
		//choose a random key
		int keynum;
		do
		{
			keynum=keychooser.nextInt();
		}
		while (keynum>transactioninsertkeysequence.lastInt());

//		if (!orderedinserts)
//		{
//			keynum=Utils.hash(keynum);
//		}
		String keyname="user"+keynum;

		HashMap<String,ByteIterator> values=new HashMap<String,ByteIterator>();

		if (writeallfields)
		{
		   //new data for all the fields
		   for (int i=0; i<fieldcount; i++)
		   {
		      String fieldname="field"+i;
		      ByteIterator data = new RandomByteIterator(fieldlengthgenerator.nextInt());
		      values.put(fieldname,data);
		   }
		}
		else
		{
		   //update a random field
		   String fieldname="field"+fieldchooser.nextString();
		   ByteIterator data = new RandomByteIterator(fieldlengthgenerator.nextInt());
		   values.put(fieldname,data);
		}

		ret = db.update(new MagicKey(keyname, keynum), values);
		
		return ret;
	}

	public int doTransactionInsert(DB db)
	{
		int ret = DB.OK;
		//choose the next key
		int keynum=transactioninsertkeysequence.nextInt();
//		if (!orderedinserts)
//		{
//			keynum=Utils.hash(keynum);
//		}
		String dbkey="user"+keynum;
		
		HashMap<String,ByteIterator> values=new HashMap<String,ByteIterator>();
		for (int i=0; i<fieldcount; i++)
		{
			String fieldkey="field"+i;
			ByteIterator data = new RandomByteIterator(fieldlengthgenerator.nextInt());
			values.put(fieldkey,data);
		}
		ret = db.insert(new MagicKey(dbkey, keynum), values);
		
		return ret;
	}
}

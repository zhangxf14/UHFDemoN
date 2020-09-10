package com.reader.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * cache object of 6C tag's Operations (including writing and killing) 
 * @author Administrator
 *
 */
public class OperateTagBuffer {

	public int fuDanOpCode = 0;
	public int type = 0x04;
	public int readLen = 0x02;
	public int TempCount = 0;
	public byte[] readMemCmd;
	public int readCount = 0;
	public Map<Integer, Double> points = new HashMap<>();

	public byte[] rtcTestTemp;

	public int testTempProgress = 0;


	public int timeStamp;
	public int testingCount;
	public int testTempCount;
	public double topTemp;
	public double bottomTemp;
	public int overTopTempCount;
	public int overBottomTempCount;
	public int maxTemp;
	public int minTemp;
	public int testTempInterval;
	public int testTempDelayTime;

	/**
	 * described object of 6C tag's Operations (including writing and killing)
	 * @author Administrator
	 *
	 */
    public static class OperateTagMap {
    	/** PC value*/
    	public String strPC;
    	/** CRC value*/
    	public String strCRC;
    	/** EPC value*/
    	public String strEPC;
    	/** Data in total */
    	public String strData;
    	/** Length of data */
    	public String nDataLen;
    	/** Antenna ID*/
    	public byte btAntId;
    	/** Operating times */
    	public int nReadCount;
		/**
		 *Operations (including writing and killing)tag default constructor
		 */
		public OperateTagMap() {
			strPC = "";
			strCRC = "";
			strEPC = "";
			strData = "";
			nDataLen = "";
			btAntId = 0;
			nReadCount = 0;
		}
    }
    
    /** Pick specific tag from the cache object*/
	public String strAccessEpcMatch;
	/** cache object */
	public List<OperateTagMap> lsTagList;
	
	/**
	 * Operate cache of tags, including writing and killing  
	 */
	public OperateTagBuffer() {
		strAccessEpcMatch = "";
		lsTagList = new ArrayList<OperateTagMap>();
	}
	
	/**
	 * Clear cache  of tags 
	 */
    public final void clearBuffer() {
    	lsTagList.clear();
    	points.clear();
    }

}

/**
 * 
 */
package com.ouc.tcp.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.ouc.tcp.message.TCP_PACKET;

/** 
 * 类描述：
 * 作者： wanwenlong 
 * 创建日期：2020年12月21日
 * 修改人：
 * 修改日期：
 * 修改内容：
 * 版本号： 1.0.0   
 */
public class TCP_ReceiverWindow {

	Queue<int[]> dataQueue = new LinkedList<int[]>(); 
	//private volatile int rwnd = 1000;
	Map<Integer, int[]> dataBuffer = new HashMap<Integer, int[]>();
	private volatile int expectedSeqNum = 0;//期待收到的包序号
	
	/*构造函数*/
	public TCP_ReceiverWindow() {
	}
	
	// 对发送方失序的包缓存
	public int rcvPackReno(int ack, int[] data) {
		int ackNum = ack / 100;
		System.out.println("Recv packet ack: " + ackNum + ", expectedSeqNum: " + expectedSeqNum);
		if (ackNum >= expectedSeqNum) {  //大于等于预期序号，否则失序
			if (dataBuffer.get(ackNum) == null) {
				dataBuffer.put(ackNum, data);    //缓存包
				System.out.println("Buffer packet " + ackNum);
				while (dataBuffer.get(expectedSeqNum) != null) {  //滑动窗口
					dataQueue.add(dataBuffer.get(expectedSeqNum));
					expectedSeqNum++;
					System.out.println("ExpectedSeqNum change: " + expectedSeqNum);
				}
			}
		}
		return expectedSeqNum - 1;
	}
	
	// 不对发送方失序的包缓存
	public int rcvPackRenoNoBuffer(int ack, int[] data) {
		int ackNum = ack / 100;
		System.out.println("Recv packet ack: " + ackNum + ", expectedSeqNum: " + expectedSeqNum);
		if (ackNum == expectedSeqNum) {  //不等于预期序号就拒收
			dataQueue.add(data);
			expectedSeqNum++;
			System.out.println("ExpectedSeqNum change: " + expectedSeqNum);
		}
		return expectedSeqNum - 1;
	}
	
	public Queue<int[]> getDataQueue() {
		return dataQueue;
	}
	
	public int[] getDataEle() {
		return dataQueue.poll();
	}

	
}

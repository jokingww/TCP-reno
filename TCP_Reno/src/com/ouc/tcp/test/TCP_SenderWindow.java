/**
 * 
 */
package com.ouc.tcp.test;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
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
public class TCP_SenderWindow {

	private volatile int sendBase = 0;
	private volatile int nextSeqNum = 0;
	
	private Client senderClient;
	
	// TCP reno
	// private volatile int MSS = 100;
	private volatile int cwnd = 1;
	private volatile int ssthresh = 16; 
	private Vector<TCP_PACKET> tcpPackSend = new Vector<TCP_PACKET>();	//记录已经发过的包，以供重传
	private Map<Integer, Integer> ackCount = new HashMap<Integer, Integer>();  //统计收到ack数目
	private volatile int count = 0;
	private UDT_Timer timer;
	
	/*构造函数*/
	public TCP_SenderWindow(Client client) {
		this.senderClient = client;
	}
	
	public void sendPackReno(TCP_PACKET tcpPack) {
		try {
			int seq = tcpPack.getTcpH().getTh_seq() / 100;
			tcpPackSend.add(tcpPack.clone());
			ackCount.put(seq, 0);
			senderClient.send(tcpPackSend.get(seq));
			if (sendBase == nextSeqNum) {  //窗口第一次不空，启动计时器，此时窗口内只有一个包发送出去
				timer = new UDT_Timer();
				timer.schedule(new TaskPacketsRetrans(), 3000, 3000);
			}
			nextSeqNum++;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}	
	
	public void recvPackReno(TCP_PACKET recvPack) {
		if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int ack = recvPack.getTcpH().getTh_ack();
			int ackNum = ack / 100;
			boolean moveFlag = false; //窗口是否滑动
			if (ackNum >= sendBase - 1) {
				ackCount.put(ackNum, ackCount.get(ackNum) + 1); //统计ACK次数
				System.out.println("AckNum " + ackNum + ", ackCount: " + ackCount.get(ackNum));
				//将sendBase到ackNum的所有包设为ACK，窗口左移
				while(sendBase <= ackNum && sendBase < nextSeqNum) {
					changeCwnd();  //每次收到ACK就使用慢开始或拥塞避免改变窗口大小
					sendBase++;
					moveFlag = true;
				}
				if (moveFlag) {	//发送窗口滑动，重启计时器
					timer.cancel();
					timer = null;
					if (sendBase != nextSeqNum) {
						timer = new UDT_Timer();
						timer.schedule(new TaskPacketsRetrans(), 3000, 3000);
					}	
				} else { //此时ackNum == sendBase - 1，必然是重复数据包
					if (ackCount.get(ackNum) > 3) {
						System.out.println("FastRetransAndRecovery, resend packet " + (ackNum+1));
						fastRetransmitAndRecovery();
					}
				}
			}
		} else {
			System.out.println("Corrupt Packet");
		}
		System.out.println("Current SendBase: " + sendBase + ", cwnd size: " + cwnd);
	}
	
	public void changeCwnd() {
		if (cwnd >= ssthresh) {
			count++;
			System.out.println("New ack! Congestion avoidance, new cwnd: " + cwnd + "+" + count + "/" + cwnd);
			if (count == cwnd) {
				count = 0;	// 累计1/cwnd个加一
				cwnd++;
			}
		} else {
			System.out.println("New ack! Slow start, cwnd: " + cwnd + "+1");
			cwnd++;
		}
	}
	
	public void fastRetransmitAndRecovery() {
		//最后一个包之后的重复包不受理
		if (sendBase != 1000) {
			//快重传
			senderClient.send(tcpPackSend.get(sendBase));
			reSetTimer();
			//快恢复
			System.out.println("Congestion! Old ssthresh: " + ssthresh + ", cwnd: " + cwnd);
			ssthresh = (cwnd / 2) > 2 ? (cwnd / 2) : 2;
			//cwnd = 1;
			cwnd = ssthresh;
			count = 0;
			System.out.println("New ssthresh: " + ssthresh + ", cwnd: " + cwnd);
		}
	}
	
	public class TaskPacketsRetrans extends TimerTask {
		public TaskPacketsRetrans() {
			super();
		}
		
		@Override
		public void run() {
			senderClient.send(tcpPackSend.get(sendBase));
			reSetTimer();
			// 超时重传，发生拥塞
			System.out.println("Congestion! Old ssthresh: " + ssthresh + ", cwnd: " + cwnd);
			ssthresh = (cwnd / 2) > 2 ? (cwnd / 2) : 2;
			cwnd = 1;
			count = 0;
			System.out.println("New ssthresh: " + ssthresh + ", cwnd: " + cwnd);
		}
	}
	
	public void blockReno() {
		while (sendBase + cwnd <= nextSeqNum);
	}
	
	public void congestion() {
	}
	
	public void reSetTimer() { //重启计时器
		timer.cancel();
		timer = new UDT_Timer();
		timer.schedule(new TaskPacketsRetrans(), 3000, 3000);
	}
}

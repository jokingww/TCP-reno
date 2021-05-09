/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	private volatile int sequence = -1;//已经接受的最新的包
		
	public TCP_ReceiverWindow receiverWindow = new TCP_ReceiverWindow();
	
	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			//生成ACK报文段（设置确认号）
			int ack = recvPack.getTcpH().getTh_seq();
			sequence = receiverWindow.rcvPackReno(ack, recvPack.getTcpS().getData());
			//sequence = receiverWindow.rcvPackRenoNoBuffer(ack, recvPack.getTcpS().getData());
			tcpH.setTh_ack(sequence * 100 + 1);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			ackPack.setTcpH(tcpH);
			//回复ACK报文段
			reply(ackPack);
		}
		
		//交付数据（每20组数据交付一次）
		System.out.println("DataQueue Size = " + receiverWindow.getDataQueue().size());
		if(receiverWindow.getDataQueue().size() >= 20) 
			deliver_data();	
		else if (sequence == 999) 	//最后一个包到达
			deliver_data();	
		System.out.println();
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		System.out.println("Write data");
		System.out.println();
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(receiverWindow.getDataQueue().size() != 0) {
				int[] data = receiverWindow.getDataEle();
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志 eflag
		//0.信道无差错	4.出错 / 丢包
		//1.只出错	5.出错 / 延迟
		//2.只丢包	6.丢包 / 延迟
		//3.只延迟	7.出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte)7);
				
		//发送数据报
		client.send(replyPack);
	}
	
}

package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		CRC32 crc32 = new CRC32();
		// 计算seq、ack和sum部分(sum初始化为0不用计算)；
		crc32.update(tcpPack.getTcpH().getTh_seq());
		crc32.update(tcpPack.getTcpH().getTh_ack());
		//crc32.update(tcpPack.getTcpH().getTh_sum());
		// TCP数据部分
		for(int i = 0; i < tcpPack.getTcpS().getData().length; i++) {
			crc32.update(tcpPack.getTcpS().getData()[i]);
		}
		checkSum = (int) crc32.getValue();
		return (short) checkSum;
	}
	
}

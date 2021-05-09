package com.ouc.tcp.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.ouc.tcp.app.SystemStart;

public class TestRun {
	
	public static void main(String[] args) throws InterruptedException {
		//SystemStart.main(null);
		try {
		    System.setOut(new PrintStream(new FileOutputStream("C:\\Users\\wan\\Desktop\\out.txt")));
		    SystemStart.main(null);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
	}
}

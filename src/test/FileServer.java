package test;

import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;

/**
 * @author 田野之弥
 * @version 1.0
 */
public class FileServer extends Thread {
	ServerSocket server;
	private static int TCPport = 2021;
	ExecutorService executorService; // 线程池
	final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

	/**
	 * 建立serverSocket,开启线程池，启动服务器
	 * @throws IOException
	 */
	public FileServer() throws IOException {
		server = new ServerSocket(TCPport);// 创建服务器套接字
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
		System.out.println("服务器启动。");
	}

	public static void main(String[] args) throws IOException {
		new FileServer().servic(args[0]); // 启动服务
	}

	/**
	 * 传入客户端根目录，等待用户连接，一旦有用户连接就交给线程池维护
	 * @param 客户端根目录
	 */
	public void servic(String args) {
		Socket socket = null;
		while (true) {
			try {
				socket = server.accept(); // 等待用户连接
				executorService.execute(new Handler(socket, args)); // 把执行交给线程池来维护
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

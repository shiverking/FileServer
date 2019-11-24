package test;

import java.net.*;
import java.util.Scanner;
import java.io.*;

/**
 * @author 田野之弥
 * @version 1.0
 *客户端功能实现
 */
public class FileClient {
	private static final int MAX_RECEIVE_BUFFER = 8192; // 最大接收缓存
	private DatagramSocket server; // DatagramSocket类用来建立服务器和客户端
	private DatagramPacket packet; // DatagramPacket类用来存入和解包UDP数据
	byte[] buffer = new byte[MAX_RECEIVE_BUFFER];
	static String serverName ;
	private static String current_directory;// 客户端的当前目录
	static int TCPport = 2021;// 设置要连接的Server的TCP端口号
	private static int UDPport = 2020;// 设置Server的UDP端口号
	Socket client = new Socket();
	private int Length; // 要get的文件长度

	/**
	 * 建立TCP socket,与服务器端进行连接
	 * 建立UDP随机端口，向服务器发送一段报文告知服务器自己的UDP端口号
	 */
	public FileClient(String servername) {
		try {
			client = new Socket();// TCP随机端口
			serverName =servername; //将服务器IP地址赋值给servename
			client.connect(new InetSocketAddress(serverName, TCPport));// 连接服务器
			packet = new DatagramPacket(buffer, buffer.length);
			server = new DatagramSocket();// UDP随机端口
			SocketAddress socketAddres = new InetSocketAddress(serverName, UDPport);
			byte[] info = "".getBytes();// 向服务器发送一个请求报文，让服务器知道自己的端口号
			DatagramPacket dp = new DatagramPacket(info, info.length, socketAddres);
			server.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 客户端业务代码
	 */
	public void send() {
		try {
			System.out.println(client.getInetAddress() + ">连接成功");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));// 将获取到的值加载到输出流
			BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));// 输入流，接收服务器的消息
			PrintWriter pw = new PrintWriter(bw, true); // 装饰输出流，及时刷新
			Scanner sc = new Scanner(System.in);
			String msg = null;
			while ((msg = sc.nextLine()) != null) {
				pw.println(msg);
				String[] tmp = cmdparsing(msg); // 解析输入的命令
				String line = br.readLine(); // 保存readLine的值
				String[] answer = huanhangparsing(line);// 从返回的值中解析换行标志
				for (String str : answer) {
					System.out.println(str); // 根据解析完的数据逐行输出
				}
				if (tmp[0].equals("get")) {// 如果输入的命令是第一段是get
					if (!line.equals("unknown File") && !line.equals("unknown cmd")) {// 如果返回的不是unknown File，则开始准备接收文件
						String[] res = sizeparsing(line);
						Length = Integer.valueOf(res[1]);// 获取要接收的文件长度
						System.out.println("准备接收中...");
						UDPrec(current_directory + "\\" + tmp[1]);// 将文件路径传过去
						System.out.println("接收完毕");
					}
				}
				if (msg.equals("bye")) {
					break;// 关闭客户端
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {

		} finally {
			if (null != client) {
				try {
					client.close(); // 断开连接
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		current_directory = args[0];
		FileClient client = new FileClient(args[1]);
		client.send();
	}

	/**
	 * 解析换行关键字
	 * @param str
	 * @return 返回经解析后的string数组
	 */
	public static String[] huanhangparsing(String str) {// 解析换行关键字
		return str.split("HUANHANG");
	}

	/**
	 * 根据空格解析命令
	 * @param str
	 * @return 经解析后的的string数组
	 */
	public static String[] cmdparsing(String str) {// 根据空格解析命令，返回一个String数组
		return str.split(" ");
	}

	/**
	 * 根据服务器返回信息中的关键字进行切割，获取长度信息
	 * @param str
	 * @return经解析后的string数组
	 */
	public static String[] sizeparsing(String str) {// 解析服务器返回值，返回一个String数组，获取要收的文件的长度
		return str.split("Size: ");
	}

	/**
	 * 传入文件路径，接收服务器发送的udp package
	 * @param path
	 * receive udp package
	 */
	public void UDPrec(String path) {// 传入的是文件的路径
		try {
			File dest = new File(path);
			FileOutputStream output = new FileOutputStream(dest);
			int len = 0; // 数据长度
			for (int i = 0; i < Length / MAX_RECEIVE_BUFFER + 1; i++) {// 循环次数=文件长度/最大缓存长度+1
				server.receive(packet);
				len = packet.getLength();
				output.write(buffer, 0, len);
				output.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

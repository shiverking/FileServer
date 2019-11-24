package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 线程池，用于维护对每一个用户的连接
 * @author 田野之弥
 * @version 1.0
 */
public class Handler implements Runnable {// 负责与单个用户通信的线程
	
	private static int TCPport = 2021;
	private static int UDPport = 2020;
	private static int ClientUdpPort; // 客户端的UDP端口号
	private static String current_directory;
	private static String root_directory;
	private String[] current_directory_filenames = null;// 储存当前路径下的所有文件夹的名称
	private String[] cmd;
	private Socket socket;
	private DatagramSocket datagramSocket;

	/**
	 * 线程池根据传入的socket为自己的sockt赋值，打开datagramsocket接收用户发来的udp数据包
	 * 根据数据包判断客户端的端口，此处有bug,当有多个用户同时连接时，无法保证先发送的先接收，应该开启一个新的handle维护udp
	 * @param socket
	 * @param root_directory
	 * @throws IOException
	 */
	public Handler(Socket socket, String Root_directory) throws IOException {
		this.socket = socket;
		root_directory=Root_directory;
		current_directory = Root_directory;
		datagramSocket = new DatagramSocket(UDPport);
		DatagramPacket dp = new DatagramPacket(new byte[512], 512);
		datagramSocket.receive(dp); // 接收客户端UDP发来的信息，以此获取端口号。
		ClientUdpPort = dp.getPort();// 得到客户端的UDP端口号
		datagramSocket.close();// 关闭端口
	}

	/**
	 *具体的业务代码，用于根据客户端传入的各种命令进行相应的响应和正确的处理
	 */
	public void run() {
		try {
			System.out.println("新连接，连接地址：" + socket.getInetAddress() + "：" + socket.getPort()); // 客户端信息
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));// 输出流，向客户端写信息
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			PrintWriter pw = new PrintWriter(bw, true); // 装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
			String info = null;
			while ((info = br.readLine()) != null) {
				cmd = cmdparsing(info);
				if (cmd.length > 2 || cmd.length == 0) { // 如果解析到的命令长度大于2或者等于0，则为无效命令
					pw.println("unknown cmd");
				} else if (cmd.length == 1) {// 如果解析到的命令长度为1，可能为有效命令，需要进行进一步判断
					if (cmd[0].equals("ls")) {
						String tmp = getFile(current_directory);// 进入当前目录
						pw.println(tmp);
					}
					if (cmd[0].equals("bye")) {
						break;
					}
					if (cmd[0].equals("cd..")) {
						pw.println(backDirectory());// 回退目录
					}
					if (!cmd[0].equals("ls") && !cmd[0].equals("bye") && !cmd[0].equals("cd..")) {
						pw.println("unknown cmd");// 如果解析到的命令长度符合，但是是无效命令，则返回
					}
				} else {// 如果解析到的命令长度为2，则可能是 get 或者是cd,还是其他无效内容，需要进行进一步判断
					if (cmd[0].equals("cd")) {
						pw.println(enterDirectory(cmd[1]));// 进入新目录
					}
					if (cmd[0].equals("get")) {
						if (isFile(cmd[1])) {
							pw.println(cmd[1] + " exists" + " Path: " + current_directory + "\\" + cmd[1] + " Size: "
									+ getSizeOfFile(current_directory + "\\" + cmd[1]));// 向客户端返回所请求的文件信息，也使客户端能够通过解析获取文件的大小
							UDPsend(current_directory + "\\" + cmd[1]);// 使用UDP发送文件
						} else {
							pw.println("unknown File");
						}
					}
					if (!cmd[0].equals("cd") && !cmd[0].equals("get")) {
						pw.println("unknown cmd");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					socket.close(); // 断开连接
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 递归实现计算文件夹的大小
	 * @param file
	 * @return size of a directory
	 */
	private static long getTotalSizeOfFilesInDir(File file) {// 递归实现计算文件夹大小
		if (file.isFile())
			return file.length();
		final File[] children = file.listFiles();
		long total = 0;
		if (children != null)
			for (final File child : children)
				total += getTotalSizeOfFilesInDir(child);
		return total;
	}

	/**
	 * 返回路径下的文件信息，返回形式是一长串的字符串，其中添加了换行关键字，在客户端再根据换行关键字进行切割输出
	 * @param path
	 * @return 需要获取的路径信息
	 */
	public static String getFile(String path) {// 根据路径获取路径下的文件，返回所有文件信息的一个string
		File file = new File(path);// 生成一个新的文件对象
		String res = "";
		String[] fileInfo = null;
		if (file.exists()) {
			File[] files = file.listFiles(); // 列出文件下所有子文件
			String[] names = file.list();
			fileInfo = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					fileInfo[i] = "<dir>" + "\t" + files[i].getName() + "\t\t"
							+ String.valueOf(getTotalSizeOfFilesInDir(files[i]));
				} else {
					fileInfo[i] = "<file>" + "\t" + files[i].getName() + "\t" + String.valueOf(files[i].length());
				}
			}
		} else {
			System.out.println("路径错误！");
		}
		for (String string : fileInfo) {
			res += string + "HUANHANG";// 每增加一行数据，添加一个换行关键标志，用来client切分返回的内容实现换行
		}
		return res;
	}

	/**
	 * 根据" "解析客户端传入的命令，返回一个String数组
	 * @param str
	 * @return int[] split by " "
	 */
	public static String[] cmdparsing(String str) {// 根据空解析命令，返回一个String数组
		return str.split(" ");
	}

	/**
	 * 进入下一层文件夹，修改当前路径
	 * @param destination
	 * Change the current directory
	 */
	public static String enterDirectory(String destination) {// 进入下一层文件夹,并修改当前路径
		File current_directory_file = new File(current_directory);
		File[] current_directory_files = current_directory_file.listFiles();
		for (int i = 0; i < current_directory_files.length; i++) {
			if (current_directory_files[i].isDirectory()) {
				if ((current_directory_files[i].getName()).equals(destination)) {
					current_directory += "\\" + destination;
					return destination + " >OK";
				}
			} else {
				continue;
			}
		}
		return "unknown dir";
	}

	/**
	 * 返回上一层文件夹，修改当前路径
	 * Change the current directory
	 */
	public static String backDirectory() {// 回退到上一层文件,并修改当前路径
		String[] directorys = current_directory.split("\\\\");
		String tmp_current_directory = "";
		if (current_directory.equals(root_directory)) {//如果当前目录就是根目录，则不能回退
			return "can't back";
		} else {
			for (int i = 0; i < directorys.length - 1; i++) {// 构造新的路径
				if (i != directorys.length - 2) {
					tmp_current_directory += directorys[i] + "\\";
				} else {
					tmp_current_directory += directorys[i];// 最后一个目录不用加反斜杠
				}
			}
			current_directory = tmp_current_directory;
		}
		return directorys[directorys.length - 2] + " >OK";
	}

	/**
	 * 判断当前路径下是否存在这个文件夹
	 * @param fileName
	 * if this file exits
	 */
	public static boolean isFile(String fileName) {// 判断当前路径下是否存在这个文件夹
		File current_directory_file = new File(current_directory+"\\"+fileName);
		return current_directory_file.isFile();
	}

	/**
	 * 根据路径返回一个文件的大小
	 * @param path 路径信息
	 * @return the size of the file文件夹的大小
	 */
	public static String getSizeOfFile(String path) {// 根据路径返回一个文件的大小
		File file = new File(path);
		return String.valueOf(file.length());
	}

	/**
	 * UDP的发送程序，根据传入的路径，将指定文件发送出去
	 * @param path
	 * send udp package
	 */
	public void UDPsend(String path) {
		try {
			datagramSocket = new DatagramSocket(UDPport);// 打开端口
			SocketAddress address = new InetSocketAddress(socket.getInetAddress(), ClientUdpPort);
			File file = new File(path);
			InputStream inputstream = new FileInputStream(file);
			byte[] data = new byte[8192];
			while ((inputstream.read(data)) != -1) {
				DatagramPacket pack = new DatagramPacket(data, data.length, address);
				datagramSocket.send(pack);
				TimeUnit.MICROSECONDS.sleep(1000); // 限制传输速度
			}
			System.out.println("发送完毕！");
			datagramSocket.close();// 关闭datagramSocket
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

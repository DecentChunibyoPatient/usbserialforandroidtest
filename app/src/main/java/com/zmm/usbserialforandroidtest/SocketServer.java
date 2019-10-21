package com.zmm.usbserialforandroidtest;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class SocketServer {
    int port;
    state state;
    HashMap<String,Socket>hashMap;
    /**
     * @param port
     */
    public  SocketServer(int port, state state) {
        // TODO Auto-generated method stub

        System.out.println("SocketServer");
        this.port=port;
        this.state=state;
        hashMap=new HashMap<>();
    }


    /**
     * 启动服务监听，等待客户端连接
     */
      void startService() {
        new Thread(){
            @Override
            public void run() {
                try {
                    // 创建ServerSocket
                    ServerSocket serverSocket = new ServerSocket(port);
                    System.out.println("--开启服务器，监听端口 "+port+"--");

                    // 监听端口，等待客户端连接
                    while (true) {
                        System.out.println("--等待客户端连接--");
                        Socket socket = serverSocket.accept(); //等待客户端连接
                        System.out.println("得到客户端连接：" + socket);
                        hashMap.put(socket.getInetAddress().getHostAddress(),socket);
                        state.Connect(socket);
                        startReader(socket);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 从参数的Socket里获取最新的消息
     */
      void startReader(final Socket socket) {
        new Thread(){
            @Override
            public void run() {
                String ip=socket.getInetAddress().getHostAddress();
                try {
                    // 从Socket当中得到InputStream对象
                    InputStream inputStream = socket.getInputStream();
                    byte buffer[] = new byte[1024 * 4];
                    int temp = 0;
                    // 从InputStream当中读取客户端所发送的数据
                    while ((temp = inputStream.read(buffer)) != -1) {
                        state.Message(ip,buffer,0,temp);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
    public void write(String ip,byte[] data) throws IOException {
          hashMap.get(ip).getOutputStream().write(data);
    }
    public static String getIp(Context context){
        WifiManager wm=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        //检查Wifi状态
        if(!wm.isWifiEnabled())
            wm.setWifiEnabled(true);
        WifiInfo wi=wm.getConnectionInfo();
        //获取32位整型IP地址
        int ipAdd=wi.getIpAddress();
        //把整型地址转换成“*.*.*.*”地址
        String ip=intToIp(ipAdd);
        return ip;
    }
    private static String intToIp(int i) {
        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    interface state{
          public void Connect( Socket socket);
        public void Message( String ip,byte[]data);
        public void Message( String ip,byte[] b, int off, int len);
    }
}
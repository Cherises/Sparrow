package com.controldesktop;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

public class ServerFunction {
    public static void sendHeadMessage(Socket socket, HeadMessage hm){
        try {
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(hm);
            oos.flush();
        }catch (Exception e){
            new OutputLog(e.toString());
        }
    }

    public static String executeCommand(String cmd){
        Runtime runtime = Runtime.getRuntime();
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(runtime.exec(cmd).getInputStream(),"GB2312"));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = br.readLine()) != null){
                builder.append(line).append("\n");
            }
            return builder.toString();
        }catch (Exception e){
            return e.toString();
        }
    }

    public static void sendErrorMessage(Socket controlSocket,String message){
        if (controlSocket != null) {
            HeadMessage hm = new HeadMessage();
            hm.setType("RETURN_ERROR_MESSAGE");
            hm.setIpInfo(new String[]{"114.115.153.152"});
            hm.setValue(new String[]{message});
            sendHeadMessage(controlSocket, hm);
        }else {
            new OutputLog("控制端没有上线，发送取消:"+message);
        }
    }

    public static void receiveFile(HeadMessage hm,ServerSocket SSocket) throws IOException {
        //在文件接收完成后再转发给控制端
        class receiveFileThread extends Thread{
            private final ServerSocket SSocket;
            private File file;
            private final FileOutputStream fos;

            public receiveFileThread(HeadMessage hm,ServerSocket SSocket) throws IOException {
                //SSocket = new ServerSocket(9575);
                this.SSocket = SSocket;
                file = new File("./"+hm.getFileInfo()[0]);
                fos = new FileOutputStream(file);
            }

            @Override
            public void run() {
                try {
                    Socket socket = SSocket.accept();
                    InputStream is = socket.getInputStream();
                    byte[] data = new byte[1024];
                    int length = 0;
                    while ((length = is.read(data)) != -1){
                        fos.write(data,0, length);
                    }
                    fos.close();
                    is.close();
                    System.out.println("文件接收完成");
                    //接下来告诉控制端，文件已经获取完毕，准备发送给控制端
                    ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                    socket = SSocket.accept();
                    OutputStream os = socket.getOutputStream();
                    data = new byte[1024];
                    file = new File("./"+hm.getFileInfo()[0]);
                    FileInputStream fis = new FileInputStream(file);
                    length = 0;
                    while ((length = fis.read(data)) != -1) {
                        os.write(data, 0, length);
                    }
                    socket.shutdownOutput();
                    System.out.println("文件已发送完毕");
                    os.close();
                    fis.close();
                    if(file.delete()){
                        System.out.println("文件已删除");
                    }else {System.out.println("文件删除失败");}
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        new receiveFileThread(hm,SSocket).start();
    }

    public static void noTimeOut(Socket socket,String ipAddress){
        class noTimeOut extends Thread{
            final Socket socket;
            final HeadMessage hm;
            final String ipAddress;
            boolean maintain = true;
            public noTimeOut(Socket socket,String ipAddress){
                this.socket=socket;
                hm = new HeadMessage();
                hm.setType("CONFIRM");
                this.ipAddress=ipAddress;
            }

            @Override
            public void run() {
                while (maintain) {
                    try {
                        OutputStream os = socket.getOutputStream();
                        BufferedOutputStream bos = new BufferedOutputStream(os);
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(hm);
                        oos.flush();
                        System.out.println("向"+ipAddress+"发送证实消息成功");
                        Thread.sleep(15000);
                        //每5秒给客户端发送一次确认，确保客户端一直保持在线状态，不能超时
                    } catch (Exception e) {
                        new OutputLog(e.toString());
                        maintain = false;
                        try {
                            socket.close();
                            if (Objects.equals(ipAddress, ServerSocketStart.ControlIPAddress)){
                                //如果IP地址等于控制端，则移除控制端的IP地址
                                ServerSocketStart.ControlSocket=null;
                                ServerSocketStart.ControlIPAddress=null;
                                maintain =false;
                                System.out.println("控制端证实线程重置");
                            }else {
                                //否则移除客户端的IP列表
                                ServerSocketStart.clientSocketPackage.remove(ipAddress);
                                System.out.println("客户端证实线程已重置");
                                maintain =false;
                            }
                            break;
                        } catch (IOException ex) {
                            new OutputLog(e.toString());
                            maintain =false;
                            break;
                        }
                    }
                }
                System.out.println(ipAddress+"证实线程已结束！");
            }
        }
        new noTimeOut(socket,ipAddress).start();
    }
}

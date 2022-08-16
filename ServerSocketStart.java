package com.controldesktop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class HeadMessage implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L;
    private String device = "SERVER";
    private String type;
    private String[] value;
    private String[] ipInfo;
    //index 0 is fromIP, 1 is toIP
    private String[] fileInfo;
    //index 0 is fileName, 1 is filePath, 2 is fileByteSize
    private byte[] fileToByte;

    public HeadMessage() {

    }
    public void setDevice(String device){
        this.device = device;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setIpInfo(String[] ipInfo){
        this.ipInfo = ipInfo;
    }

    public void setValue(String[] value) {
        this.value = value;
    }

    public void setFileInfo(String[] fileInfo) {
        this.fileInfo = fileInfo;
    }


    public void setFileToByte(byte[] fileToByte){
        this.fileToByte = fileToByte;
    }


    public String getDevice() {
        return device;
    }

    public String getType() {
        return type;
    }

    public String[] getIpInfo() {
        return ipInfo;
    }

    public String[] getFileInfo() {
        return fileInfo;
    }
    public String[] getValue() {
        return value;
    }

    public byte[] getFileToByte(){
        return fileToByte;
    }
}

//用于存放客户端的Socket以及IP地址
class ClientSocketList{
    Socket socket;
    String ipAddress;

    public ClientSocketList(Socket socket, String ipAddress){
        this.socket = socket;
        this.ipAddress = ipAddress;
    }

    public ClientSocketList() {

    }

    public Socket getSocket(){
        return socket;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}

class OutputLog{
    public OutputLog(String value){
        byte[] message = new byte[0];
        //自动为每一条语句加上回车，方便阅读
        value += "\n";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss >");
        Date date = new Date(System.currentTimeMillis());
        String nowTime = formatter.format(date);
        //打印结果到屏幕
        System.out.print(nowTime+value);
        File file = new File("./ServerLog.log");
        if (file.exists()){
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true)));
                bw.write(nowTime+value);
                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            try {
                file.createNewFile();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true)));
                String newValue = nowTime + "新文件创建成功！\n";
                bw.write(newValue);
                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

//服务端创建客户端新线程类
class ThreadStart extends Thread{
    private final Socket socket;
    private final String ipAddress;

    private boolean maintain = true;
    private ServerSocket FSocket;
    public ThreadStart(String ipAddress,Socket socket,ServerSocket FSocket) {
        this.socket = socket;
        this.ipAddress = ipAddress;
        this.FSocket = FSocket;
    }


    @Override
    public void run() {
        ServerFunction.noTimeOut(socket,ipAddress);//每10秒发送一次确认，如果发送失败则结束
        while (maintain) {
            try {
                InputStream is = socket.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object obj = ois.readObject();
                ResultSet rs1;
                //以下是操作数据库案例
                LinkToMySQL LinkToSQL = new LinkToMySQL();


//                ResultSet rs1 = LinkToSQL.GetSQLValue("select * from student");
//                while (rs1.next()){
//                int id = rs1.getInt(1);
//                String name = rs1.getString(2);
//                String birthday = rs1.getString(3);
//                System.out.println(id +" "+ name+" "+birthday);
//                }
//                LinkToSQL.CloseAll();


                //ScreenSocketTest sst = new ScreenSocketTest();
                if (obj != null) {
                    HeadMessage hm = (HeadMessage) obj;
                    hm.setDevice("SERVER");//将收到的报头信息的设备提前转换为自己的服务端
                    String ipAdd;
                    File file;
                    String sql;
                    switch (hm.getType()) {
                        case "EXIT":
                            socket.close();
                            bis.close();
                            ois.close();
                            ServerSocketStart.clientSocketPackage.remove(ipAddress);
                            maintain = false;//结束循环
                            break;
                        case "GET_SCREEN":
                            ipAdd = hm.getIpInfo()[1];//0 is Control ip, 1 is Client ip
                            if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                try {
                                    Socket clientSocket = ServerSocketStart.clientSocketPackage.get(ipAdd);
                                    ServerFunction.sendHeadMessage(clientSocket,hm);
                                }catch (Exception e){
                                    new OutputLog(e.toString());
                                }
                            }else {
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"没有在客户端Map中找到对应:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。");
//                                hm.setType("RETURN_ERROR_MESSAGE");
//                                hm.setValue(new String[]{"没有在客户端Map中找到对应:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
//                                ServerFunction.sendHeadMessage(socket,hm);
                            }
                            break;
                        case "RETURN_GET_SCREEN":
                            //将获取到的屏幕信息转发给控制端
                            ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                            break;
                        case "CLOSE_GET_SCREEN":
                            //sst.Stop();
                            System.out.println("已收到停止信号");
                            break;
                        case "DIALOG":
                            System.out.println("来自客户端: " + hm.getValue()[0]);
                            break;
                        case "DOWNLOAD_FILE":
                            ipAdd = hm.getIpInfo()[1];
                            hm.setIpInfo(new String[]{ServerSocketStart.ControlIPAddress,ipAdd});
                            if (Objects.equals(ipAdd, "114.115.153.152")){
                                file = new File(hm.getFileInfo()[1]);
                                hm.setFileInfo(new String[]{hm.getFileInfo()[0], hm.getFileInfo()[1], String.valueOf(file.length())});
                                hm.setType("RETURN_DOWNLOAD_FILE");

                                ServerFunction.sendHeadMessage(socket,hm);
                                //new SendFile(hm).start();
                                Thread.sleep(1000);
                                new SendFileTCP(hm).start();
                            }else if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                hm.setIpInfo(new String[]{ipAddress,ipAdd});
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                hm.setType("RETURN_ERROR_MESSAGE");
                                hm.setValue(new String[]{"没有在客户端Map中找到对应:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
                                ServerFunction.sendHeadMessage(socket,hm);
                            }
                            break;
                        case "RETURN_DOWNLOAD_FILE":
                            ServerFunction.receiveFile(hm,FSocket);
                            //在收到返回文件信息后，先进行获取，然后再转发给控制端
                                //ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                            break;
                        case "GET_ARTICLE_LIST":
                            rs1 = LinkToSQL.GetSQLValue("select title from article");
                            //String[] value = new String[0];
                            List<String> titleList = new ArrayList<>();
                            while (rs1.next()){
                                String title = rs1.getString(1);
                                titleList.add(title);
                            }
                            LinkToSQL.CloseAll();
                            hm.setValue(titleList.toArray(new String[0]));

                            hm.setType("RETURN_GET_ARTICLE_LIST");
                            ServerFunction.sendHeadMessage(socket,hm);
                            break;
                        case "LIST_PATH":
                            ipAdd = hm.getIpInfo()[1];
                            if (Objects.equals(ipAdd, "114.115.153.152")) {
                                String FilePath = hm.getValue()[0];
                                file = new File(FilePath);
                                String[] AllFile = file.list();
                                File item;
                                assert AllFile != null;
                                for (int i = 0; i < AllFile.length; i++) {
                                    item = new File(FilePath + "\\" + AllFile[i]);
                                    if (item.isDirectory()) {
                                        AllFile[i] += "\\";
                                    }
                                }
                                hm.setValue(AllFile);
                                hm.setType("RETURN_LIST_PATH");
                                hm.setFileInfo(new String[]{"", FilePath});
                                ServerFunction.sendHeadMessage(socket, hm);
                            }else if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。");
//                                hm.setType("RETURN_ERROR_MESSAGE");
//                                hm.setValue(new String[]{"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
//                                ServerFunction.sendHeadMessage(socket,hm);
                            }
                            break;
                        case "RETURN_LIST_PATH":
                            ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                            break;
                        case "GET_MY_IP":
                            hm.setIpInfo(new String[]{ServerSocketStart.ControlIPAddress});
                            hm.setType("RETURN_GET_MY_IP");
                            ServerFunction.sendHeadMessage(socket,hm);
                            break;
                        case "GET_CLIENT_IP":
                            hm.setType("RETURN_CLIENT_IP");
                            String[] ipAdds = ServerSocketStart.clientSocketPackage.keySet().toArray(new String[0]);
                            hm.setValue(ipAdds);
                            ServerFunction.sendHeadMessage(socket,hm);
                            break;
                        case "RETURN_ERROR_MESSAGE":
                            hm.setIpInfo(new String[]{ipAddress});
                            ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                            break;
                        case "EXECUTE_CMD":
                            ipAdd = hm.getIpInfo()[1];
                            if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。");
                                //hm.setType("RETURN_ERROR_MESSAGE");
                                //hm.setValue(new String[]{"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
                                //ServerFunction.sendHeadMessage(socket,hm);
                            }
                            break;
                        case "RETURN_EXECUTE_CMD":
                            //转发报头信息
                            ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket,hm);
                            break;
                        case "CHECK_UPDATE":
                            try {
                                hm.setType("RETURN_CHECK_UPDATE");
                                hm.setValue(new String[]{"1.0", "2.0版本新增下载文件的功能。"});
                                //hm.setIpInfo(new String[]{ipAddress});
                                ServerFunction.sendHeadMessage(socket, hm);
                            }catch (Exception e){
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"客户端"+ipAddress+"在检查更新的时候出错"+e);
                                new OutputLog("客户端"+ipAddress+"在检查更新的时候出错"+e);
                            }
                            break;
                        case "GET_NEW_VERSION":
                            //将本地新版本客户端发送给客户端
                            try {
                                hm.setIpInfo(new String[]{ipAddress});
                                file = new File("ServerLog.log");//发送log日志文件是为了测试，等正式发布后有规定的名字
                                hm.setFileInfo(new String[]{"ServerLog.log", "./ServerLog.log", String.valueOf(file.length())});
                                hm.setType("RETURN_GET_NEW_VERSION");
                                ServerFunction.sendHeadMessage(socket, hm);
                                Thread.sleep(2000);
                                new SendFileTCP(hm).start();
                            }catch (Exception e){
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"客户端"+ipAddress+"获取新版本的时候出错"+e);
                                new OutputLog("客户端"+ipAddress+"获取新版本的时候出错"+e);
                            }
                            break;
                        case "CONTROL_SAY_MESSAGE":
                            ipAdd = hm.getIpInfo()[1];
                            if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                hm.setType("RETURN_ERROR_MESSAGE");
                                hm.setValue(new String[]{"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
                                ServerFunction.sendHeadMessage(socket,hm);
                                //new OutputLog("")
                            }
                            break;
                        case "NEW_ARTICLE":
                            try {
                                int readType;
                                if (Objects.equals(hm.getValue()[2], "true")) {
                                    readType = 1;
                                } else {
                                    readType = 0;
                                }
                                sql = "insert into article values ('" + hm.getValue()[0] + "','" + hm.getValue()[1] + "','" + readType + "')";
                                //System.out.println(sql);
                                String value = LinkToSQL.insertIntoSQL(sql);
                                hm.setType("RETURN_NEW_ARTICLE");
                                hm.setValue(new String[]{value});
                                LinkToSQL.CloseAll();
                                ServerFunction.sendHeadMessage(socket, hm);
                            }catch (Exception e){
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"保存新文章的过程出错"+e);
                                new OutputLog("保存新文章的过程出错");
                            }
                            break;
                        case "GET_ARTICLE":
                            String title = hm.getValue()[0];
                            sql = "select * from article where title='"+title+"'";
                            try {
                                rs1 = LinkToSQL.GetSQLValue(sql);
                                int Type;
                                while (rs1.next()) {
                                    System.out.println(rs1.getInt("type"));
                                    Type = rs1.getInt("type");
                                    if (Type == 0) {
                                        hm.setType("CONTROL_SAY_MESSAGE");
                                        hm.setValue(new String[]{"文章>" + title + "<暂不可被阅读。"});
                                        ServerFunction.sendHeadMessage(socket, hm);
                                    } else {
                                        String value = rs1.getString("value");
                                        hm.setType("RETURN_GET_ARTICLE");
                                        hm.setValue(new String[]{title, value});
                                        ServerFunction.sendHeadMessage(socket, hm);
                                    }
                                    break;
                                }
                            }catch (Exception e){
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"读取数据库文章的时候出错"+e);
                                new OutputLog("读取数据库文章的时候出错"+e);
                            }
                            break;
                        case "CHAT":
                            try {
                                hm.setType("RETURN_CHAT");
                                hm.setIpInfo(new String[]{ipAddress});//将发信息人的IP地址写入报头文件中，方便接收端使用

                                Set<String> set = ServerSocketStart.clientSocketPackage.keySet();
                                for (String s : set) {
                                    ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(s), hm);
                                }
                                new OutputLog("收到来自" + ipAddress + "的消息，内容为:" + hm.getValue()[0] + "已全部转发");
                                if (ServerSocketStart.ControlSocket != null) {
                                    ServerFunction.sendHeadMessage(ServerSocketStart.ControlSocket, hm);
                                }
                            }catch (Exception e){
                                ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"转发信息过程出错"+ e);
                                new OutputLog("转发信息的过程出错："+e.toString());
                            }
                            break;
                        case "CLIENT_EXIT":
                            ipAdd = hm.getIpInfo()[1];
                            if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                hm.setType("EXIT");
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                hm.setType("RETURN_ERROR_MESSAGE");
                                hm.setValue(new String[]{"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
                                ServerFunction.sendHeadMessage(socket,hm);
                                //new OutputLog("")
                            }
                            break;
                        case "JOKE_TO_CLIENT":
                            ipAdd = hm.getIpInfo()[1];
                            if (ServerSocketStart.clientSocketPackage.containsKey(ipAdd)){
                                ServerFunction.sendHeadMessage(ServerSocketStart.clientSocketPackage.get(ipAdd),hm);
                            }else {
                                hm.setType("RETURN_ERROR_MESSAGE");
                                hm.setValue(new String[]{"没有在客户端Map中找到对应IP地址:"+ipAdd+" 请检查IP是否有误，或者此IP已下线。"});
                                ServerFunction.sendHeadMessage(socket,hm);
                                //new OutputLog("")
                            }
                            break;
                        default:
                            ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"没有找到匹配的Type类型");
                            //hm.setType("RETURN_ERROR_MESSAGE");
                            //hm.setValue(new String[]{"没有找到匹配的类型"});
                            //ServerFunction.sendHeadMessage(socket,hm);
                            new OutputLog("getType没有找到匹配的类型");
                    }
                }

            } catch (Exception e) {
                //throw new RuntimeException(e);
                new OutputLog("IP为"+ipAddress+"的服务端接收线程异常结束"+e);
                ServerSocketStart.clientSocketPackage.remove(ipAddress);
                maintain = false;
                break;
            }
        }
        //结束
        if (Objects.equals(ipAddress, ServerSocketStart.ControlIPAddress)){
            //如果当前通信的socket为控制端，那么在结束的时候就应该重置IP地址和socket
            ServerSocketStart.ControlIPAddress = null;
            ServerSocketStart.ControlSocket = null;
        } else {
            //否则就是客户端，需要在客户端列表中删除此IP与socket
            ServerSocketStart.clientSocketPackage.remove(ipAddress);
        }
        ServerFunction.sendErrorMessage(ServerSocketStart.ControlSocket,"客户端"+ipAddress+"已下线");
        new OutputLog("客户端"+ipAddress+"已下线！");
    }

}


public class ServerSocketStart{
    public static String ControlIPAddress;
    public static Socket ControlSocket = null;
    public static Map<String,Socket> clientSocketPackage;

    String ipAddress;
    public ServerSocketStart(){
        clientSocketPackage = new HashMap<>();
        class PrintIP extends Thread{
            final Map<String,Socket> clientSocketPackage;
            public PrintIP(Map<String,Socket> clientSocketPackage){
                this.clientSocketPackage= clientSocketPackage;
            }

            @Override
            public void run() {
                try{
                    while (true) {
                        Set<String> set = clientSocketPackage.keySet();
                        for (String s : set) {
                            System.out.println(s);
                        }
                        Thread.sleep(3000);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        try {
            System.out.println("*************************");
            System.out.println("* Control Server Client *");
            System.out.println("*    Welcome to CSC     *");
            System.out.println("*     Version 1.0       *");
            System.out.println("*  Author @Ni Chenyang  *");
            System.out.println("*************************");
            System.out.println("正在启动服务...");
            ServerSocket ss = new ServerSocket(9574);
            System.out.println("收发信息服务已开启");
            ServerSocket FileSocket = new ServerSocket(9575);
            System.out.println("收发文件服务已开启");
            System.out.println("等待客户端连接...");
            //new PrintIP(clientSocketPackage).start();
//            clientSocketPackage.put("/114.115.153.152",new Socket());
//            clientSocketPackage.put("/12.23.111.220",new Socket());
//            clientSocketPackage.put("/222.45.122.33",new Socket());
            while (true) {
                Socket socket = ss.accept();

                //在第一次连接的时候先互相发送报头文件以确认对方身份
                InputStream is = socket.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object obj = ois.readObject();

                if (obj != null){
                    HeadMessage hm = (HeadMessage) obj;
                    try {
                        ipAddress = socket.getInetAddress().getHostAddress() + ">" + hm.getIpInfo()[0];
                    }catch (Exception e){
                        hm.setType("EXIT");
                        ServerFunction.sendHeadMessage(socket,hm);
                        new OutputLog("客户端"+socket.getInetAddress().getHostAddress()+"打开了旧版本小程序，线程重置");
                        continue;
                    }
                    if (Objects.equals(hm.getDevice(), "CONTROL")){
                        ControlIPAddress = ipAddress;
                        ControlSocket = socket;
                        //System.out.println("控制端"+ipAddress+"已上线");
                        new OutputLog("控制端"+ipAddress+"已上线");
                    }
                    if (Objects.equals(hm.getDevice(),"CLIENT")){
                        //System.out.println("客户端"+ipAddress+"已上线");
                        new OutputLog("客户端"+ipAddress+"已上线");
                        String[] ipList = clientSocketPackage.keySet().toArray(new String[0]);
                        if (ControlIPAddress != null) {
                            try {//尝试将新客户端上线的信息发送给控制端
                                hm.setDevice("SERVER");
                                hm.setType("NEW_CLIENT");
                                hm.setIpInfo(new String[]{ipAddress});
                                ServerFunction.sendHeadMessage(ControlSocket, hm);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if(Arrays.asList(ipList).contains(ipAddress)){
                            hm.setType("EXIT");
                            ServerFunction.sendHeadMessage(socket,hm);
                            new OutputLog("客户端已存在，自动跳过新建线程，使客户端退出");
                            continue;
                        }
                        clientSocketPackage.put(ipAddress, socket);
                    }
                    new ThreadStart(ipAddress, socket, FileSocket).start();
                }
                //可以在这里写入日志，告知一个连接已建立
                //ClientSocketList csl = new ClientSocketList(socket, ipAddress);
                //将此客户端的Socket添加到字典中去并启用新线程

            }
        }catch (Exception e){
            e.printStackTrace();
            //如果运行在服务器上需要把这些内容写入到日志文件中
            new OutputLog(e.toString());
        }
    }
}

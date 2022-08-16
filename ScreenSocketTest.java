package com.controldesktop;

import org.bytedeco.javacv.CanvasFrame;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.io.IOException;

import java.net.*;

class Head{
    int length;
    byte[] value;
    public Head(int length,byte[] value){
        this.value=value;
        this.length=length;
    }

    public int getLength() {
        return length;
    }
    public byte[] getValue() {
        return value;
    }
}

class ThreadStartTest extends Thread{
    //ServerSocket SSocket;
    public volatile boolean exits = true;
    DatagramSocket DSocket;

    String ipAdd;
    public ThreadStartTest(DatagramSocket DSocket, String ipAdd){
        this.DSocket = DSocket;
        this.ipAdd = ipAdd;
    }

    @Override
    public void run() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();// 获取当前屏幕大小
        Rectangle rectangle = new Rectangle(screenSize);// 指定捕获屏幕区域大小，这里使用全屏捕获

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();//本地环境
        GraphicsDevice[] gs = ge.getScreenDevices();//获取本地屏幕设备列表
        System.err.println("找到"+gs.length+"个屏幕设备");
        Robot robot=null;
        int ret=-1;
        for(int index=0;index<10;index++){
            GraphicsDevice g=gs[index];
            try {
                robot= new Robot(g);
                BufferedImage img=robot.createScreenCapture(rectangle);
                if(img!=null&&img.getWidth()>1){
                    ret=index;
                    break;
                }
            } catch (AWTException e) {
                System.err.println("打开第"+index+"个屏幕设备失败，尝试打开第"+(index+1)+"个屏幕设备");
            }
        }
        System.err.println("打开的屏幕序号："+ret);

        while (exits) {
            assert robot != null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedImage image = robot.createScreenCapture(rectangle);// 从当前屏幕中读取的像素图像，该图像不包括鼠标光标
            try {
                ImageIO.write(image,"png",baos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] datas = baos.toByteArray();

            DatagramPacket packet = null;
            try {
                packet = new DatagramPacket(datas,datas.length, InetAddress.getByName(ipAdd),9575);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            try {
                DSocket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(datas.length);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


public class ScreenSocketTest {
    ServerSocket SSocket;
    Socket socket;
    ThreadStartTest tst;

    public boolean exits = true;
    public ScreenSocketTest(){
    }
    public void Start() throws IOException, InterruptedException {
        int port = 9575;
        DatagramSocket DSocket = new DatagramSocket(9575);

        tst = new ThreadStartTest(DSocket,"127.0.0.1");
        tst.start();
        while (this.exits){
            Thread.sleep(50);
        }
        System.out.println("发出结束信号");
        tst.exits = false;
        socket.close();
    }

    public void Stop() throws IOException {
        this.exits = false;
        socket.close();
    }
}

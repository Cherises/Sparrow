package com.controldesktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;


public class SendFile extends Thread{
    private DatagramSocket sender;
    HeadMessage hm;
    public SendFile(HeadMessage hm){
        this.hm = hm;
    }

    @Override
    public void run() {
        try{
            sender = new DatagramSocket();
            File file = new File(hm.getFileInfo()[1]);

            InputStream is = new FileInputStream(file);


            byte[] data = new byte[1024];
            int length;
            while ((length = is.read(data)) != -1){
                DatagramPacket pack = new DatagramPacket(data,data.length, InetAddress.getByName("127.0.0.1"),9575);
                sender.send(pack);
                //System.out.println(length);
                //Thread.sleep(1);
                //TimeUnit.MICROSECONDS.sleep(5);
                if (length < 1024){
                    sender.close();
                    //System.out.println("跳出");
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

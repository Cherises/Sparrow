package com.controldesktop;

import java.io.*;
import java.net.Socket;

public class SendFileTCP extends Thread{
    Socket socket;
    File file;
    FileInputStream fis;
    OutputStream os;
    public SendFileTCP(HeadMessage hm) throws IOException {
        socket = new Socket(hm.getIpInfo()[0],9575);
        file = new File(hm.getFileInfo()[1]);
        fis = new FileInputStream(file);
    }

    @Override
    public void run() {
        try {
            os = socket.getOutputStream();
            System.out.println("准备发送文件");
            byte[] data = new byte[1024];
            int length = 0;
            while ((length = fis.read(data)) != -1) {
                os.write(data, 0, length);
            }
            socket.shutdownOutput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                fis.close();
                socket.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

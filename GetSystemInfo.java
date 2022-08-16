package com.controldesktop;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GetSystemInfo {
    public String executeCMD(String cmd){
        Runtime mt = Runtime.getRuntime();
        try {
            Process pro = mt.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream(),"GB2312"));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null){
                sb.append(line).append("\n");
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
}

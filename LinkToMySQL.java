package com.controldesktop;

import java.sql.*;

public class LinkToMySQL {
    Connection con;
    Statement stat;
    ResultSet rs;
    public LinkToMySQL() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/controlserver","controlserver","AWpfnCDrGTshSJJ4");
        stat = con.createStatement();
    }
    //实例化此类后可以通过语句直接调用此函数来获取返回值，返回值即为ResultSet接口
    public ResultSet GetSQLValue(String sql_value) throws SQLException {
        rs = stat.executeQuery(sql_value);
        return rs;
    }

    public String insertIntoSQL(String sql_value){
        try {
            stat.executeUpdate(sql_value);
        }catch (Exception e){
            return e.toString();
        }
        return "语句成功执行";
    }


    //关闭所有接口
    public void CloseAll(){
        try{
            con.close();
            stat.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}


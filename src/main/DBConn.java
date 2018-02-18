/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.awt.Component;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 *
 * @author TechTac
 */
public class DBConn {
    public static int updateSQLite(String update) {
        Connection c = null;
        Statement stmt = null;
        int rs = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:config");
            c.setAutoCommit(true);
            System.out.println("Database connection established (Update SQLite)");

            stmt = c.createStatement();
            rs = stmt.executeUpdate(update);
            //c.commit();
            //rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

        System.out.println("Database connection terminated (Update SQLite)");
        return rs;
    }
    
    public static int getDpl() {
        Connection c = null;
        Statement stmt = null;
        int dpl = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:config");
            c.setAutoCommit(false);
            System.out.println("getDpl opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT display FROM `app_sets`;");
            
            while(rs.next()){
                dpl = rs.getInt("display");
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            //System.exit(0);
        }

        System.out.println("getDpl done successfully");
        
        return dpl;
    }
    
    public static void setDpl(int display) {
        String dpl = Integer.toString(display);
        updateSQLite("UPDATE `app_sets` SET display = " + dpl + "");
    }
    
    public static int getState() {
        Connection c = null;
        Statement stmt = null;
        int wstate = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:config");
            c.setAutoCommit(false);
            System.out.println("getState opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT wstate FROM `app_sets`;");
            
            while(rs.next()){
                wstate = rs.getInt("wstate");
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            //System.exit(0);
        }

        System.out.println("getState done successfully");
        
        return wstate;
    }
    
    public static void setState(int wstate) {
        String state = Integer.toString(wstate);
        updateSQLite("UPDATE `app_sets` SET wstate = " + state + "");
    }
    
    public static int[] getSizePos() {
        Connection c = null;
        Statement stmt = null;
        int[] bounds = {0,0,0,0};
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:config");
            c.setAutoCommit(false);
            System.out.println("getSizePos opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM `app_sets`;");
            
            while(rs.next()){
                bounds[0] = rs.getInt("pos_x");
                bounds[1] = rs.getInt("pos_y");
                bounds[2] = rs.getInt("width");
                bounds[3] = rs.getInt("height");
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            //System.exit(0);
        }

        System.out.println("getSizePos done successfully");
        
        return bounds;
    }
    
    public static void setSizePos(int x, int y, int w, int h) {
        updateSQLite("UPDATE `app_sets` SET pos_x = " + x + ", pos_y = " + y + ", width = " + w + ", height = " + h + ";");
    }
    
    public static boolean updateTrackers(String[] trcks, Component parent) {
        int res1 = updateSQLite("DELETE FROM `trackers`;");
        int res2 = 0;
        for (int i = 0; i < trcks.length; i++) {
            int no = i+1;
            res2 = res2 + updateSQLite("INSERT INTO `trackers`(`no`,`trcks`) VALUES (" + no + ",'" + trcks[i] + "');");
        }
        if (res1 != 0 && res2 != 0) {
            return true;
        } else {
            return false;
        }
    }
    
    public static String loadTrackers(Component parent) {
        Connection c = null;
        Statement stmt = null;
        String trcks = "";
        
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:config");
            c.setAutoCommit(false);
            System.out.println("loadTrackers opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM `trackers`;");
            
            int count = 0;
            while (rs.next()) {
                count = rs.getInt(1);
            }
            
            ArrayList<Integer> nums = new ArrayList<Integer>();
            for (int i = 1; i <= count; i++) {
                nums.add(i);
            }
            Collections.shuffle(nums);
            
            rs = stmt.executeQuery("SELECT trcks FROM `trackers` WHERE "
                    + "no=" + nums.get(0) + " OR "
                    + "no=" + nums.get(1) + " OR "
                    + "no=" + nums.get(2) + " OR "
                    + "no=" + nums.get(3) + " OR "
                    + "no=" + nums.get(4) + ";");
            
            
            
            while (rs.next()) {
                trcks = trcks + "&tr=" + URLEncoder.encode(rs.getString("trcks"), "UTF8");
            }
            
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            errbox(parent, "Error loading Trackers. Please contact TechTac");
            //System.exit(0);
        }

        System.out.println("loadTrackers done successfully");
        
        return trcks;
        
    }
    
    public static void msgbox(Component parent, String s) {
        JOptionPane.showMessageDialog(parent, s);
    }

    public static void errbox(Component parent, String s) {
        JOptionPane.showMessageDialog(parent, s, "Error", JOptionPane.ERROR_MESSAGE);
    }
}

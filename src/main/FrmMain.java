/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import net.proteanit.sql.DbUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.relique.jdbc.csv.CsvDriver;

/**
 *
 * @author TechTac
 */
public class FrmMain extends javax.swing.JFrame {

    /**
     * Creates new form FrmMain
     */
    public FrmMain() {
        initComponents();
        tblTorrents.setAutoCreateRowSorter(true);
    }
    
    private void search(String qry){
        
        //qry = qry.replace("\"", "\"\"");
        qry = qry.replaceAll("'", "''");
        qry = qry.trim();
        qry = qry.replaceAll("(\\s+)", " ");
        String ready = "";
        //System.out.println(qry);
        
        if (chkSS.isSelected()) {
            String[] qryParts = qry.split(" ");
            ready = "UPPER(NAME) LIKE UPPER('%" + qryParts[0] + "%')";

            int c = 1;
            while (c < qryParts.length) {
                ready = ready + " AND UPPER(NAME) LIKE UPPER('%" + qryParts[c] + "%')";
                c++;
            }
        } else {
            ready = "UPPER(NAME) LIKE UPPER('%" + qry + "%')";
        }
        
        //System.out.println(ready);

        try {
            System.out.println("Search Started");
            //System.out.println("SELECT * FROM dump WHERE \"NAME\" LIKE '" + qry + "' LIMIT 10000;");
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Properties props = new java.util.Properties();
            props.put("separator", ";");
            props.put("ignoreNonParseableLines", true);
            
            Class.forName("org.relique.jdbc.csv.CsvDriver");
            Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + System.getProperty("user.dir"), props);
            Statement stmt = conn.createStatement();
            //ResultSet results = stmt.executeQuery("SELECT * FROM dump WHERE \"#ADDED\"='2011-Sep-08 05:09:05';");
            ResultSet results = stmt.executeQuery("SELECT * FROM dump WHERE " + ready + " LIMIT 20000;");
            
            tblTorrents.setModel(DbUtils.resultSetToTableModel(results));
            tblTorrents.getColumnModel().removeColumn(tblTorrents.getColumnModel().getColumn(1));
            tblTorrents.getColumnModel().getColumn(0).setHeaderValue("Date Added");
            tblTorrents.getColumnModel().getColumn(1).setHeaderValue("Torrent Name");
            tblTorrents.getColumnModel().getColumn(2).setHeaderValue("Torrent Size");
            
            tblTorrents.getColumnModel().getColumn(0).setMaxWidth(200);
            tblTorrents.getColumnModel().getColumn(0).setPreferredWidth(200);
            tblTorrents.getColumnModel().getColumn(2).setMaxWidth(100);
            tblTorrents.getColumnModel().getColumn(2).setPreferredWidth(100);
            
            formatSizes();
//            while (results.next()) {
//                System.out.println(results.getString(3));
//            }
            System.out.println("Search Ended");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void importDump() throws Exception {
        try {
            File someFile = new File("dump.csv");
            File temp = File.createTempFile(someFile.getName(), null);
            BufferedReader reader = null;
            PrintStream writer = null;

            try {
                reader = new BufferedReader(new FileReader(someFile));
                writer = new PrintStream(temp);

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replaceAll("(?!\";)(?<!;)\"", "\"\"");
                    writer.println(line);
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            }
            if (!someFile.delete()) {
                throw new Exception("Failed to remove " + someFile.getName());
            }
            if (!temp.renameTo(someFile)) {
                throw new Exception("Failed to replace " + someFile.getName());
            }
        } catch (IOException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void formatSizes(){
        
        int count = tblTorrents.getModel().getRowCount();
        for (int row = 0; row < count; row++) {
            String val_string = (String) tblTorrents.getModel().getValueAt(row, 3);
            //System.out.println(row);
            Long val = Long.parseLong(val_string);
            String fin_val = humanReadableByteCount(val, false);
            tblTorrents.getModel().setValueAt(fin_val, row, 3);
        }

    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "kMGTPE").charAt(exp - 1) + (si ? "" : "");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void magnet(){
        String guid = "4QQSPpi9goBxPIOrcbDuurFQwBs=";
        byte[] decoded = Base64.decodeBase64(guid);
        String hexString = Hex.encodeHexString(decoded);
        System.out.println(hexString);
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnlMain = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblTorrents = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        btnCpyDesc = new javax.swing.JButton();
        btnCpyTitle = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        chkSS = new javax.swing.JCheckBox();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jLabel4 = new javax.swing.JLabel();
        txtFilter = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuTools = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        pnlMain.setLayout(new java.awt.GridBagLayout());

        txtSearch.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        txtSearch.setText("\"bright\"");
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtSearchKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 14;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 12, 6, 10);
        pnlMain.add(txtSearch, gridBagConstraints);

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/search-1.png"))); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 3, 12);
        pnlMain.add(btnSearch, gridBagConstraints);

        tblTorrents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblTorrents.setRowHeight(27);
        jScrollPane1.setViewportView(tblTorrents);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(17, 12, 13, 12);
        pnlMain.add(jScrollPane1, gridBagConstraints);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/tech_tac_alpha_127.png"))); // NOI18N
        jLabel2.setToolTipText("Visit TechTac :)");
        jLabel2.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel2MouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 13, 10);
        pnlMain.add(jLabel2, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel1.setText("By");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 8, 0);
        pnlMain.add(jLabel1, gridBagConstraints);

        btnCpyDesc.setText("Copy");
        btnCpyDesc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCpyDescActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 12);
        pnlMain.add(btnCpyDesc, gridBagConstraints);

        btnCpyTitle.setText("Copy");
        btnCpyTitle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCpyTitleActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.insets = new java.awt.Insets(17, 0, 0, 12);
        pnlMain.add(btnCpyTitle, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        chkSS.setSelected(true);
        chkSS.setText("Smart Search");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(chkSS, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(filler1, gridBagConstraints);

        jLabel4.setText("Filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
        jPanel2.add(jLabel4, gridBagConstraints);

        txtFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtFilterKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(txtFilter, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 12, 0, 12);
        pnlMain.add(jPanel2, gridBagConstraints);

        getContentPane().add(pnlMain, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Ready");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(6, 13, 10, 0);
        jPanel1.add(jLabel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        jPanel1.add(jSeparator1, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        mnuTools.setText("Tools");

        jMenuItem1.setText("Import Data");
        mnuTools.add(jMenuItem1);

        jMenuBar1.add(mnuTools);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        search(txtSearch.getText());
    }//GEN-LAST:event_btnSearchActionPerformed

    private void jLabel2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel2MouseClicked
        try {
            Desktop.getDesktop().browse(new URI("https://www.youtube.com/c/techtac?sub_confirmation=1"));
        } catch (IOException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jLabel2MouseClicked

    private void btnCpyDescActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCpyDescActionPerformed
        
    }//GEN-LAST:event_btnCpyDescActionPerformed

    private void btnCpyTitleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCpyTitleActionPerformed
        
    }//GEN-LAST:event_btnCpyTitleActionPerformed

    private void txtFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtFilterKeyReleased

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(((DefaultTableModel) tblTorrents.getModel()));
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Matcher.quoteReplacement(txtFilter.getText())));
        tblTorrents.setRowSorter(sorter);

    }//GEN-LAST:event_txtFilterKeyReleased

    private void txtSearchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyTyped
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_ENTER) {
            search(txtSearch.getText());
        }
    }//GEN-LAST:event_txtSearchKeyTyped

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            UIManager.setLookAndFeel(new SyntheticaBlackEyeLookAndFeel());
            //SwingUtilities.updateComponentTreeUI(this);
            //</editor-fold>
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new FrmMain().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JButton btnCpyDesc;
    protected javax.swing.JButton btnCpyTitle;
    protected javax.swing.JButton btnSearch;
    protected javax.swing.JCheckBox chkSS;
    protected javax.swing.Box.Filler filler1;
    protected javax.swing.JLabel jLabel1;
    protected javax.swing.JLabel jLabel2;
    protected javax.swing.JLabel jLabel3;
    protected javax.swing.JLabel jLabel4;
    protected javax.swing.JMenuBar jMenuBar1;
    protected javax.swing.JMenuItem jMenuItem1;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JScrollPane jScrollPane1;
    protected javax.swing.JSeparator jSeparator1;
    protected javax.swing.JMenu mnuTools;
    protected javax.swing.JPanel pnlMain;
    protected javax.swing.JTable tblTorrents;
    protected javax.swing.JTextField txtFilter;
    protected javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}

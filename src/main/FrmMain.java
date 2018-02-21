/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import static main.DBConn.errbox;
import static main.DBConn.msgbox;
import net.proteanit.sql.DbUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.relique.jdbc.csv.CsvDriver;
import techtac.LiveTracker;

/**
 *
 * @author TechTac
 */
public class FrmMain extends javax.swing.JFrame {
    
    private boolean start = true;
    private final String USER_AGENT = "Mozilla/5.0";
    public int display = 0;
    public int wstate = 6;
    private PnlOL pnlOL = new PnlOL();
    private volatile boolean isLoading = false;
    private JPanel glassPane = new JPanel() {
            @Override
            public boolean contains(int x, int y) {
                Component[] components = getComponents();
                for (int i = 0; i < components.length; i++) {
                    Component component = components[i];
                    Point containerPoint = SwingUtilities.convertPoint(
                            this,
                            x, y,
                            component);
                    if (component.contains(containerPoint)) {
                        return true;
                    }
                }
                return false;
            }
        };
    private Thread not = new Thread();
    private SwingWorker<Void, Void> seedCounter;
    private volatile boolean scraping = false;
    private volatile int rs_count = 10000;
    

    /**
     * Creates new form FrmMain
     */
    public FrmMain() {
        display = DBConn.getDpl();
        wstate = DBConn.getState();
        rs_count = DBConn.getRsCount();
        
        initComponents();
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/main/res/icon_32.png")));
        glassPane.setOpaque(false);
        lblStatus2.setVisible(false);
        jSeparator2.setVisible(false);
        hideSeedsPanel();
        tblTorrents.setAutoCreateRowSorter(true);
        txtRsCount.setTransferHandler(null);
        txtRsCount.setText(Integer.toString(rs_count));
        
        showOnScreen(display, this);
        setVisible(true);
        if (this.getRootPane().getUI() instanceof SyntheticaRootPaneUI) {
            ((SyntheticaRootPaneUI) this.getRootPane().getUI()).setMaximizedBounds(this);
        }
        this.setExtendedState(wstate);
        
    }
    
    public void showOnScreen(int screen, JFrame frame) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        if (screen > -1 && screen < gd.length) {
            frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x, frame.getY());
            setStartBounds(gd[screen], frame);
        } else if (gd.length > 0) {
            frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x, frame.getY());
            setStartBounds(gd[screen], frame);
        } else {
            throw new RuntimeException("No Screens Found");
        }
    }
    
    public void setStartBounds(GraphicsDevice gd, JFrame frm) {
        if (wstate == 0) {
            int[] bounds = DBConn.getSizePos();
            double width  = gd.getDefaultConfiguration().getBounds().getWidth() + 20;
            double height = gd.getDefaultConfiguration().getBounds().getHeight() + 20;
            if (width > (Math.abs(bounds[0]) + Math.abs(bounds[2]))) {
                frm.setLocation(bounds[0], frm.getY());
                frm.setSize(bounds[2], frm.getHeight());
                //System.out.println((Math.abs(bounds[0]) + Math.abs(bounds[2])));
            }
            if (height > (Math.abs(bounds[1]) + Math.abs(bounds[3]))) {
                frm.setLocation(frm.getX(), bounds[1]);
                frm.setSize(frm.getWidth(), bounds[3]);
                //System.out.println("Came to 2nd condition");
            }
        }
        //System.out.println(gd.getDefaultConfiguration().getBounds().getHeight());
    }
    
    public void startOL(String stat) {
        isLoading = true;
        //glassPane = getGlassPane();
        setGlassPane(pnlOL);
        pnlOL.revalidate();
        pnlOL.setAlpha(0.5f);
        setCompEnable(false);
        pnlOL.lblStat.setText(stat);
        pnlOL.setVisible(true);
    }
    
    public void endOL() {
        setGlassPane(glassPane);
        setCompEnable(true);
        isLoading = false;
        pnlOL.lblStat.setText("Waiting..");
        pnlOL.lblProg.setText("");
        pnlOL.lblProg.setVisible(false);
    }
    
    private void setCompEnable(boolean enable) {
        enableComponents(pnlMain, enable);
        enableComponents(jMenuBar1, enable);
        if (enable) {
            if (tblTorrents.getSelectedRow() != -1) {
                btnOpenMag.setEnabled(true);
                btnCpyMag.setEnabled(true);
                btnCpyHash.setEnabled(true);
            } else {
                btnOpenMag.setEnabled(false);
                btnCpyMag.setEnabled(false);
                btnCpyHash.setEnabled(false);
            }
        }
    }
    
    private void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container)component, enable);
            }
        }
    }

    private void sendNtf(String msg, String mode) {

        SwingWorker<Void, Void> backgroundProcess;
        backgroundProcess = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    not.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
                }
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected void done() {
                if (!isLoading) {
                    //final Component glassPane = getGlassPane();
                    final PnlNtf panel = new PnlNtf();
                    panel.lblMsg.setText(msg);
                    if (mode == "err"){
                        panel.lblMsg.setBackground(new Color(133, 0, 0));
                    } else if (mode == "scs"){
                        panel.lblMsg.setBackground(new Color(0, 90, 0));
                    } else if (mode == "warn"){
                        panel.lblMsg.setBackground(new Color(153, 102, 0));
                    }
                    setGlassPane(panel);
                    panel.revalidate();
                    panel.setOpaque(false);
                    panel.setVisible(true);

                    not = new Thread() {
                        @Override
                        public void run() {

                            try {
                                float alp = 0.0f;
                                while (alp <= 1.0f) {
                                    panel.lblMsg.setAlpha(alp);
                                    alp = alp + 0.01f;
                                    Thread.sleep(2);
                                }
                                Thread.sleep(5000);
                                while (alp >= 0.0f) {
                                    panel.lblMsg.setAlpha(alp);
                                    alp = alp - 0.01f;
                                    Thread.sleep(2);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // loading finished
                            if (!isLoading) {
                                setGlassPane(glassPane);
                            }
                        }
                    };
                    not.start();
                    super.done(); //To change body of generated methods, choose Tools | Templates.
                }
            }
        };
        backgroundProcess.execute();

    }
    
    private void search(String qry){
        txtFilter.setText("");
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
            startOL("Searching..");
            System.out.println("Search Started");
            //System.out.println("SELECT * FROM dump WHERE \"NAME\" LIKE '" + qry + "' LIMIT 10000;");
            //this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            Properties props = new java.util.Properties();
            props.put("separator", ";");
            props.put("ignoreNonParseableLines", true);
            
            Class.forName("org.relique.jdbc.csv.CsvDriver");
            Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + System.getProperty("user.dir"), props);
            Statement stmt = conn.createStatement();
            ResultSet results = null;
            //ResultSet results = stmt.executeQuery("SELECT * FROM dump WHERE \"#ADDED\"='2011-Sep-08 05:09:05';");
            if(start){
                results = stmt.executeQuery("SELECT * FROM dump LIMIT 100;");
            }else{
                results = stmt.executeQuery("SELECT * FROM dump WHERE " + ready + " LIMIT " + rs_count + ";");
            }
            
            
            tblTorrents.setModel(DbUtils.resultSetToTableModel(results));
            tblTorrents.getColumnModel().removeColumn(tblTorrents.getColumnModel().getColumn(1));
            tblTorrents.getColumnModel().getColumn(0).setHeaderValue("Date Added");
            tblTorrents.getColumnModel().getColumn(1).setHeaderValue("Torrent Name");
            tblTorrents.getColumnModel().getColumn(2).setHeaderValue("Torrent Size");
            
            tblTorrents.getColumnModel().getColumn(0).setMaxWidth(200);
            tblTorrents.getColumnModel().getColumn(0).setPreferredWidth(200);
            tblTorrents.getColumnModel().getColumn(2).setMaxWidth(120);
            tblTorrents.getColumnModel().getColumn(2).setPreferredWidth(120);
            
            int count = tblTorrents.getRowCount();
            
            if (count == 10000){
                lblStatus.setForeground(Color.white);
                if (chkSS.isSelected()){
                    lblStatus.setText("<html><span style=\"color:red\">More than 10,000 Results.</span> Please refine your Search or try turning off Smart Search</html>");
                } else {
                    lblStatus.setText("<html><span style=\"color:red\">More than 10,000 Results.</span> Please refine your Search</html>");
                }
            } else {
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                String countForm = numberFormat.format(count);
                lblStatus.setForeground(Color.white);
                lblStatus.setText(countForm + " Results found");
            }
            
            formatTbl();
//            while (results.next()) {
//                System.out.println(results.getString(3));
//            }
            System.out.println("Search Ended");
            conn.close();
        } catch (Exception e) {
            //System.out.println("ERROR OCCURED");
            lblStatus.setForeground(Color.red);
            lblStatus.setText("Search error occured");
            sendNtf("Search error occured!! Try importing a clean dump file", "err");
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            endOL();
            //this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void importDump(String path) throws Exception {
        pnlOL.lblProg.setText("Setting up..");
        pnlOL.lblProg.setVisible(true);
        startOL("Importing..");
        try {
            Path sourchPath = Paths.get(path);
            long lineCount = Files.lines(sourchPath).count();

            File sourceCSV = new File(path);
            File curCSV = new File("dump.csv");
            if (!curCSV.exists()) {
                if (!curCSV.createNewFile()) {
                    endOL();
                    sendNtf("Failed to Import. Unable to create new files. Try running as Administrator", "err");
                    return;
                }
            }
            File temp = File.createTempFile(sourceCSV.getName(), null);
            BufferedReader reader = null;
            PrintStream writer = null;

            try {
                reader = new BufferedReader(new FileReader(sourceCSV));
                writer = new PrintStream(temp);

                String line;
                long curLine = 0;
                int percentage = 0;
                while ((line = reader.readLine()) != null) {
                    if (curLine == 0){
                        if (line.equals("#ADDED;HASH(B64);NAME;SIZE(BYTES)")){
                            System.out.println("FILE OK");
                        } else {
                            temp.delete();
                            endOL();
                            sendNtf("Failed to Import. Selected CSV has a mismatching format!!", "err");
                            return;
                        }
                    } 
                    line = line.replaceAll("(?!\";)(?<!;)\"", "\"\"");
                    writer.println(line);
                    curLine++;
                    percentage = Math.round((100.0f / lineCount) * curLine);
                    pnlOL.lblProg.setText("Processing dump file - " + percentage + "%");
                }
            } finally {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
            }
            if (!curCSV.delete()) {
                temp.delete();
                endOL();
                throw new Exception("Failed to remove " + curCSV.getName());
            }
            if (!temp.renameTo(curCSV)) {
                endOL();
                throw new Exception("Failed to replace " + sourceCSV.getName());
            }
            endOL();
            sendNtf("Importing successfully finished!", "scs");
            lblStatus.setForeground(Color.white);
            lblStatus.setText("Ready");
        } catch (IOException ex) {
            endOL();
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
            sendNtf("Failed to Import. Unable to perform a file operation", "err");
        } 
    }
    
    private void formatTbl(){
        
        int count = tblTorrents.getModel().getRowCount();
        for (int row = 0; row < count; row++) {
            String val_string = (String) tblTorrents.getModel().getValueAt(row, 3);
            //System.out.println(row);
            Long val = Long.parseLong(val_string);
            String fin_val = humanReadableByteCount(val, false);
            tblTorrents.getModel().setValueAt(fin_val, row, 3);
            tblTorrents.getModel().setValueAt(new TableObj((String) tblTorrents.getModel().getValueAt(row, 2), (String) tblTorrents.getModel().getValueAt(row, 1)), row, 2);
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

    private String getMagnet(){
        
        String basic = "magnet:?xt=urn:btih:" + getInfoHash();
        String withName = "";
        try {
            withName = basic + "&dn=" + URLEncoder.encode(((TableObj) tblTorrents.getValueAt(tblTorrents.getSelectedRow(), 1)).name, "UTF8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        String ready = withName + DBConn.loadTrackers(this);
        
        //System.out.println(ready);
        return ready;
    }
    
    private String getInfoHash(){
        
        //String b64hash = (String) tblTorrents.getModel().getValueAt(tblTorrents.getSelectedRow(), 1);
        String b64hash = ((TableObj) tblTorrents.getValueAt(tblTorrents.getSelectedRow(), 1)).hash;
        byte[] decoded = Base64.decodeBase64(b64hash);
        String hexString = Hex.encodeHexString(decoded);
        //System.out.println(hexString);
        
        return hexString;
    }
    
    private void showSeedsPanel(){
        lblPeers.setText("Updating..");
        lblSeeds.setText("Updating..");
        lblPeers.setVisible(true);
        lblPeersIcon.setVisible(true);
        lblPeersLoading.setVisible(true);
        lblSeeds.setVisible(true);
        lblSeedsIcon.setVisible(true);
        lblSeedsLoading.setVisible(true);
        lblSPPanelTitle.setVisible(true);
    }
    
    private void hideSeedsPanel(){
        lblPeers.setVisible(false);
        lblPeersIcon.setVisible(false);
        lblPeersLoading.setVisible(false);
        lblSeeds.setVisible(false);
        lblSeedsIcon.setVisible(false);
        lblSeedsLoading.setVisible(false);
        lblSPPanelTitle.setVisible(false);
        lblPeers.setText("Updating..");
        lblSeeds.setText("Updating..");
    }
    
    private void startSeedCounter() {
        if (scraping){
            seedCounter.cancel(true);
            while(scraping){
               //WAIT 
            }  
            showSeedsPanel();
            getSeeds();
        } else {
            showSeedsPanel();
            getSeeds();
        }
    }
    
    private void getSeeds() {
        scraping = true;
        seedCounter = new SwingWorker<Void, Void>() {
            
            long seeds_tot = 0;
            long peers_tot = 0;

            @Override
            protected Void doInBackground() throws Exception {
                ArrayList<Map.Entry<String, String>> trcks = DBConn.loadAllTrackers();

                //String b64hash = (String) tblTorrents.getModel().getValueAt(tblTorrents.getSelectedRow(), 1);
                String b64hash = ((TableObj) tblTorrents.getValueAt(tblTorrents.getSelectedRow(), 1)).hash;
                byte[] decoded = Base64.decodeBase64(b64hash);

                for (Map.Entry<String, String> curr : trcks) {
                    if (!scraping) {
                        return null;
                    } else {
                        if(seedCounter == this){
                        LiveTracker track = new LiveTracker(curr.getKey(), curr.getValue(), decoded, 3);

                        boolean success = false;

                        try {
                            //System.out.println(curr.getKey());
                            success = track.scrapeTracker();
                        } catch (Exception e) {
                            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, e);
                            sendNtf("Having trouble updating Seeds/Peers with some Trackers", "warn");
                        }

                        if (success) {
                            //System.out.println(success);
                            //System.out.println("Seeds - " + track.getSeeds());
                            //System.out.println("Peers - " + track.getPeers());
                            seeds_tot = seeds_tot + track.getSeeds();
                            peers_tot = peers_tot + track.getPeers();
                            publish();
                        }
                        } else {
                            return null;
                        }
                    }
                    //System.out.print(curr.getKey() + "    ");
                    //System.out.println(curr.getValue());
                }
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected void done() {
                //System.err.println("FINISHED");
                scraping = false;
                lblPeersLoading.setVisible(false);
                lblSeedsLoading.setVisible(false);
 
                if (lblPeers.getText().equals("Updating..")) {
                    if (!this.isCancelled()) {
                        hideSeedsPanel();
                        sendNtf("Unable to update Seeds/Peers. Please check your internet connection", "err");
                    }
                }
            }

            @Override
            protected void process(List<Void> chunks) {
                lblSeeds.setText(Long.toString(seeds_tot));
                lblPeers.setText(Long.toString(peers_tot));
            }
        };
        seedCounter.execute();

    }
    
    private void updTrackers() {
        
        FrmMain main = this;

        SwingWorker<Void, Void> backgroundProcess;
        backgroundProcess = new SwingWorker<Void, Void>() {

            boolean success = false;

            @Override
            protected Void doInBackground() throws Exception {

                try {
                    //this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    startOL("Updating Trackers..");
                    String url = "https://newtrackon.com/api/stable";

                    URL url_obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) url_obj.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", USER_AGENT);

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        if (!inputLine.isEmpty()) {
                            //System.out.print(inputLine);
                            response.append(inputLine + " ");
                            //System.out.println(c++);
                        }

                    }
                    in.close();

                    String[] lines = response.toString().split(" ");
                    success = DBConn.updateTrackers(lines, main);

                    //System.out.println(lines[0]);
                    //System.out.println(response);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
                    success = false;
                    //errbox(main, "Error updating Trackers(API). Check your internet connection and Please contact TechTac");
                } catch (IOException ex) {
                    Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
                    success = false;
                    //errbox(main, "Error updating Trackers(API). Check your internet connection and Please contact TechTac");
                } finally {
                    //this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    endOL();
                    lblStatus2.setVisible(false);
                    jSeparator2.setVisible(false);
                }
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected void done() {
                if (success) {
                    sendNtf("Trackers updated successfully", "scs");
                    //msgbox(this, "Trackers updated successfully");
                } else {
                    sendNtf("Error updating Trackers. Please check your internet connection and contact TechTac", "err");
                    //errbox(main, "Error updating Trackers(DB). Please contact TechTac");
                }
            }
        };
        backgroundProcess.execute();

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
        btnCpyHash = new javax.swing.JButton();
        btnOpenMag = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        chkSS = new javax.swing.JCheckBox();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jLabel4 = new javax.swing.JLabel();
        txtFilter = new javax.swing.JTextField();
        txtRsCount = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        btnCpyMag = new javax.swing.JButton();
        lblSPPanelTitle = new javax.swing.JLabel();
        lblPeersLoading = new javax.swing.JLabel();
        lblPeersIcon = new javax.swing.JLabel();
        lblPeers = new javax.swing.JLabel();
        lblSeedsLoading = new javax.swing.JLabel();
        lblSeeds = new javax.swing.JLabel();
        lblSeedsIcon = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lblStatus2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuTools = new javax.swing.JMenu();
        mnuImport = new javax.swing.JMenuItem();
        mnuUpdTrackers = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        mnuContact = new javax.swing.JMenuItem();
        mnuAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("OfflineBay by TechTac");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentMoved(java.awt.event.ComponentEvent evt) {
                formComponentMoved(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnlMain.setLayout(new java.awt.GridBagLayout());

        txtSearch.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        txtSearch.setText("big bang theory");
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

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/search-icon.png"))); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 3, 12);
        pnlMain.add(btnSearch, gridBagConstraints);

        tblTorrents = new javax.swing.JTable(){

            @Override
            public boolean isCellEditable(int row, int column) {
                //all cells false
                return false;
            }
        };
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
        tblTorrents.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                hideSeedsPanel();
                if (scraping){
                    seedCounter.cancel(true);
                    while(scraping){
                        //WAIT
                    }
                }
                if(tblTorrents.getSelectedRow() != -1){
                    btnOpenMag.setEnabled(true);
                    btnCpyMag.setEnabled(true);
                    btnCpyHash.setEnabled(true);
                }else{
                    btnOpenMag.setEnabled(false);
                    btnCpyMag.setEnabled(false);
                    btnCpyHash.setEnabled(false);
                }
            }
        });
        tblTorrents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblTorrentsMouseClicked(evt);
            }
        });
        tblTorrents.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tblTorrentsKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(tblTorrents);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 10;
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
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 13, 10);
        pnlMain.add(jLabel2, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        jLabel1.setText("By");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 8, 10);
        pnlMain.add(jLabel1, gridBagConstraints);

        btnCpyHash.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/price-tag.png"))); // NOI18N
        btnCpyHash.setText("Copy Info Hash");
        btnCpyHash.setEnabled(false);
        btnCpyHash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCpyHashActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 12);
        pnlMain.add(btnCpyHash, gridBagConstraints);

        btnOpenMag.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/magnet-arrow-icon.png"))); // NOI18N
        btnOpenMag.setText("Open Magnet");
        btnOpenMag.setEnabled(false);
        btnOpenMag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenMagActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.insets = new java.awt.Insets(17, 0, 0, 12);
        pnlMain.add(btnOpenMag, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        chkSS.setSelected(true);
        chkSS.setText("Smart Search");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        jPanel2.add(chkSS, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(filler1, gridBagConstraints);

        jLabel4.setText("Filter:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
        jPanel2.add(jLabel4, gridBagConstraints);

        txtFilter.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtFilterKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(txtFilter, gridBagConstraints);

        txtRsCount.setText("10000");
        txtRsCount.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRsCountKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtRsCountKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        jPanel2.add(txtRsCount, gridBagConstraints);

        jLabel5.setText("Max. Results:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 0, 0);
        jPanel2.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 12, 0, 12);
        pnlMain.add(jPanel2, gridBagConstraints);

        btnCpyMag.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/magnet-plus-icon.png"))); // NOI18N
        btnCpyMag.setText("Copy Magnet");
        btnCpyMag.setEnabled(false);
        btnCpyMag.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCpyMagActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 55;
        gridBagConstraints.ipady = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 12);
        pnlMain.add(btnCpyMag, gridBagConstraints);

        lblSPPanelTitle.setText("Seeds/Peers");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(17, 0, 0, 0);
        pnlMain.add(lblSPPanelTitle, gridBagConstraints);

        lblPeersLoading.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/updating.gif"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(9, 11, 0, 12);
        pnlMain.add(lblPeersLoading, gridBagConstraints);

        lblPeersIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/users.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(12, 8, 0, 0);
        pnlMain.add(lblPeersIcon, gridBagConstraints);

        lblPeers.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lblPeers.setText("Updating..");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        pnlMain.add(lblPeers, gridBagConstraints);

        lblSeedsLoading.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/updating.gif"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(9, 11, 0, 12);
        pnlMain.add(lblSeedsLoading, gridBagConstraints);

        lblSeeds.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lblSeeds.setText("Updating..");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        pnlMain.add(lblSeeds, gridBagConstraints);

        lblSeedsIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main/res/user.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(12, 8, 0, 0);
        pnlMain.add(lblSeedsIcon, gridBagConstraints);

        getContentPane().add(pnlMain, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        lblStatus.setText("Ready");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 13, 10, 6);
        jPanel1.add(lblStatus, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weightx = 0.1;
        jPanel1.add(jSeparator1, gridBagConstraints);

        lblStatus2.setText("Updating Trackers....");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 10, 7);
        jPanel1.add(lblStatus2, gridBagConstraints);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 0);
        jPanel1.add(jSeparator2, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        mnuTools.setText("Tools");

        mnuImport.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        mnuImport.setText("Import Data");
        mnuImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuImportActionPerformed(evt);
            }
        });
        mnuTools.add(mnuImport);

        mnuUpdTrackers.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        mnuUpdTrackers.setText("Update Trackers");
        mnuUpdTrackers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuUpdTrackersActionPerformed(evt);
            }
        });
        mnuTools.add(mnuUpdTrackers);

        jMenuBar1.add(mnuTools);

        mnuHelp.setText("Help");

        mnuContact.setText("Contact TechTac");
        mnuContact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuContactActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuContact);

        mnuAbout.setText("About");
        mnuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mnuAbout);

        jMenuBar1.add(mnuHelp);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        lblStatus.setForeground(Color.white);
        lblStatus.setText("Searching....");
        
        SwingWorker<Void, Void> backgroundProcess;
        backgroundProcess = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                search(txtSearch.getText());
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
        backgroundProcess.execute();
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

    private void btnCpyHashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCpyHashActionPerformed
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(getInfoHash()),
                null
        );
        sendNtf("Info Hash copied to Clipboard..", null);
    }//GEN-LAST:event_btnCpyHashActionPerformed

    private void btnOpenMagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenMagActionPerformed
        
        try {
            Desktop.getDesktop().browse(new URI(getMagnet()));
            sendNtf("Opening Magnet link in default Torrent Client..", null);
        } catch (IOException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnOpenMagActionPerformed

    private void txtFilterKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtFilterKeyReleased

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(((DefaultTableModel) tblTorrents.getModel()));
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(txtFilter.getText())));
        tblTorrents.setRowSorter(sorter);

    }//GEN-LAST:event_txtFilterKeyReleased

    private void txtSearchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyTyped
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_ENTER) {
            lblStatus.setForeground(Color.white);
            lblStatus.setText("Searching....");

            SwingWorker<Void, Void> backgroundProcess;
            backgroundProcess = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    search(txtSearch.getText());
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

            };
            backgroundProcess.execute();
        }
    }//GEN-LAST:event_txtSearchKeyTyped

    private void mnuUpdTrackersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuUpdTrackersActionPerformed
        
        lblStatus2.setVisible(true);
        jSeparator2.setVisible(true);
        SwingWorker<Void, Void> backgroundProcess;
        backgroundProcess = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                updTrackers();
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
        backgroundProcess.execute();
    }//GEN-LAST:event_mnuUpdTrackersActionPerformed

    private void btnCpyMagActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCpyMagActionPerformed
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(getMagnet()),
                null
        );
        sendNtf("Magnet link copied to Clipboard..", null);
    }//GEN-LAST:event_btnCpyMagActionPerformed

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
        int st = this.getExtendedState();
        if ((st == 0 || st == 6) && (st != wstate)){
            wstate = st;
            DBConn.setState(wstate);
        }
    }//GEN-LAST:event_formWindowStateChanged

    private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
        String dpl = this.getGraphicsConfiguration().getDevice().getIDstring();
        int num = Integer.parseInt("" + dpl.charAt(dpl.length() - 1));
        if (num != display){
            display = num;
            DBConn.setDpl(display);
        }
    }//GEN-LAST:event_formComponentMoved

    private void tblTorrentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblTorrentsMouseClicked
        if (evt.getClickCount() == 2) {
            startSeedCounter();
        }
    }//GEN-LAST:event_tblTorrentsMouseClicked

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
//        System.out.println(this.getX());
//        System.out.println(this.getY());
//        System.out.println(this.getWidth());
//        System.out.println(this.getHeight());
        DBConn.setSizePos(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }//GEN-LAST:event_formWindowClosing

    private void mnuImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuImportActionPerformed
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fc.setFileFilter(filter);
        fc.setDialogTitle("Open thePirateBay Dump CSV file");
        fc.setAcceptAllFileFilterUsed(false);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            SwingWorker<Void, Void> backgroundProcess;
            backgroundProcess = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        importDump(path);
                    } catch (Exception ex) {
                        endOL();
                        sendNtf("Importing Failed. Unable to do changes to the current dump", "err");
                        Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

            };
            backgroundProcess.execute();

        } else {
            sendNtf("Importing cancelled by User..", null);
        }
    }//GEN-LAST:event_mnuImportActionPerformed

    private void mnuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuAboutActionPerformed
        DlgAbout dlg = new DlgAbout(this, true);
        dlg.setVisible(true);
    }//GEN-LAST:event_mnuAboutActionPerformed

    private void mnuContactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuContactActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("https://www.youtube.com/c/techtac?sub_confirmation=1"));
        } catch (IOException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FrmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mnuContactActionPerformed

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
        if (start) {
            lblStatus.setForeground(Color.white);
            lblStatus.setText("Searching....");

            SwingWorker<Void, Void> backgroundProcess;
            backgroundProcess = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    search(txtSearch.getText());
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                protected void done() {
                    start = false;
                }
            };
            backgroundProcess.execute();
        }
    }//GEN-LAST:event_formComponentShown

    private void tblTorrentsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tblTorrentsKeyTyped
        char c = evt.getKeyChar();
        if (Character.isLetter(c)) {
            txtFilter.requestFocusInWindow();
            txtFilter.setText(Character.toString(c));
        }
    }//GEN-LAST:event_tblTorrentsKeyTyped

    private void txtRsCountKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRsCountKeyReleased
        if (!txtRsCount.getText().trim().equals("")) {
            int count = Integer.parseInt(txtRsCount.getText());
            if (count > 10000 || count == 0) {
                rs_count = 10000;
                txtRsCount.setText("10000");
            } else {
                char c = evt.getKeyChar();
                if (Character.isDigit(c)) {
                    rs_count = count;
                    DBConn.setRsCount(count);
                }
            }
        } else {
            txtRsCount.setText(Integer.toString(rs_count));
        }

    }//GEN-LAST:event_txtRsCountKeyReleased

    private void txtRsCountKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRsCountKeyTyped
        char c = evt.getKeyChar();
        if (!Character.isDigit(c)) {
            if (c != KeyEvent.VK_BACK_SPACE) {
                evt.consume();
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }//GEN-LAST:event_txtRsCountKeyTyped

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
    
    class TableObj  {
        public String name;
        public String hash;
        
        public TableObj(String nm, String b64){
            name = nm;
            hash = b64;
        }

        @Override
        public String toString() {
            return name; //To change body of generated methods, choose Tools | Templates.
        }
    }
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JButton btnCpyHash;
    protected javax.swing.JButton btnCpyMag;
    protected javax.swing.JButton btnOpenMag;
    protected javax.swing.JButton btnSearch;
    protected javax.swing.JCheckBox chkSS;
    protected javax.swing.Box.Filler filler1;
    protected javax.swing.JLabel jLabel1;
    protected javax.swing.JLabel jLabel2;
    protected javax.swing.JLabel jLabel4;
    protected javax.swing.JLabel jLabel5;
    protected javax.swing.JMenuBar jMenuBar1;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JScrollPane jScrollPane1;
    protected javax.swing.JSeparator jSeparator1;
    protected javax.swing.JSeparator jSeparator2;
    protected javax.swing.JLabel lblPeers;
    protected javax.swing.JLabel lblPeersIcon;
    protected javax.swing.JLabel lblPeersLoading;
    protected javax.swing.JLabel lblSPPanelTitle;
    protected javax.swing.JLabel lblSeeds;
    protected javax.swing.JLabel lblSeedsIcon;
    protected javax.swing.JLabel lblSeedsLoading;
    protected javax.swing.JLabel lblStatus;
    protected javax.swing.JLabel lblStatus2;
    protected javax.swing.JMenuItem mnuAbout;
    protected javax.swing.JMenuItem mnuContact;
    protected javax.swing.JMenu mnuHelp;
    protected javax.swing.JMenuItem mnuImport;
    protected javax.swing.JMenu mnuTools;
    protected javax.swing.JMenuItem mnuUpdTrackers;
    protected javax.swing.JPanel pnlMain;
    protected javax.swing.JTable tblTorrents;
    protected javax.swing.JTextField txtFilter;
    protected javax.swing.JTextField txtRsCount;
    protected javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}

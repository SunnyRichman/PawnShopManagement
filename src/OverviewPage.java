import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class OverviewPage extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel netSummary = new JLabel();
    private JLabel todayPawn = new JLabel();
    private JLabel todayWithdraw = new JLabel();

    public OverviewPage() {
        setTitle("ระบบจัดการตั๋วจำนำ");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel for button and summary
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Create "Add Ticket" button
        JButton addButton = new JButton("เพิ่มตั๋วใหม่");
        addButton.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
        addButton.setPreferredSize(new Dimension(150, 40));
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.addActionListener(e -> addTicket());

        JButton refresh = new JButton("รีเฟรช");
        refresh.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
        refresh.setPreferredSize(new Dimension(150, 40));
        refresh.setAlignmentX(Component.CENTER_ALIGNMENT);
        refresh.addActionListener(e -> loadTicket()) ;

        buttonPanel.add(addButton);
        buttonPanel.add(refresh);

        // Load summary data
        netSummary.setAlignmentX(Component.CENTER_ALIGNMENT);

        todayPawn.setAlignmentX(Component.CENTER_ALIGNMENT);

        todayWithdraw.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components with spacing
        topPanel.add(buttonPanel);
        topPanel.add(Box.createVerticalStrut(10)); // Space between button and summary
        topPanel.add(netSummary);
        topPanel.add(todayPawn);
        topPanel.add(todayWithdraw);

        // Fetch and display summary details
        Object[] columns = new String[]{
            "เลขที่", "วันที่ออกตั๋ว", "ชื่อจริง", "นามสกุล", "เบอร์โทรศัพท์", "ราคา", "ค่าไถ่", "ระยะเวลา", "วันครบกำหนด", "วันไถ่ถอน","สถานะ", "เลขที่เก่า", "เลขที่ใหม่", "", "", ""
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.table = new JTable(tableModel);
        this.table.setRowHeight(25);
        this.table.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
        this.table.getTableHeader().setFont(new Font("TH Sarabun New", Font.BOLD, 20));

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(5).setPreferredWidth(40);
        columnModel.getColumn(6).setPreferredWidth(40);
        columnModel.getColumn(11).setPreferredWidth(40);
        columnModel.getColumn(12).setPreferredWidth(40);
        columnModel.getColumn(13).setPreferredWidth(20);
        columnModel.getColumn(14).setPreferredWidth(60);
        columnModel.getColumn(15).setPreferredWidth(60);

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setResizable(false);
        }
        
        // Prevent users from resizing the table itself
        table.getTableHeader().setResizingAllowed(false);
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                String ticketNo = table.getValueAt(row, 0).toString();
                String status = table.getValueAt(row, 10).toString();

                if(column == 10){
                    JLabel statusLabel = new JLabel(status);
                    statusLabel.setFont(new Font("TH SArabun New", Font.PLAIN, 20));
                    JOptionPane.showMessageDialog(null, statusLabel, "สถานะ", JOptionPane.INFORMATION_MESSAGE);
                }else if (column == 13 && status.equals("อยู่ระหว่างจำนำ")) {
                    editTicket(row);
                } else if (column == 14 && status.equals("อยู่ระหว่างจำนำ")) {
                    actionPanel(ticketNo);
                } else if (column == 15) {
                    viewTicket(ticketNo, status);
                }
            }
        });

        loadTicket();

        JScrollPane tableScrollPane = new JScrollPane(table);

        // Main Panel Layout
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add to Frame
        add(mainPanel);
        setVisible(true);
    }

    private void loadTicket() {
        
        overdueUpdate();
        getSummary();

        try (Connection conn = db_Connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ticket")) {

            this.tableModel.setRowCount(0); // Clear existing rows

            while (rs.next()) {
                
                Object[] row = {
                    rs.getString("_No"),
                    ADtoBE(rs.getDate("issueDate").toString()),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("phoneNumber"),
                    rs.getInt("totalPrice"),
                    rs.getInt("redemptPrice"),
                    rs.getInt("duration"),
                    ADtoBE(rs.getDate("dueDate").toString()),
                    rs.getDate("redemptDate") != null ? ADtoBE(rs.getDate("redemptDate").toString()) : "",
                    rs.getString("status"),
                    rs.getString("old_ticket_No"),
                    rs.getString("new_ticket_No"),
                    "แก้ไข",
                    "ดำเนินการตั๋ว",
                    "ดูรายละเอียด"
                };
                this.tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JLabel fail = new JLabel("โหลดข้อมูลไม่สำเร็จ");
            fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTicket(){
        int ticketNo = getLatestNo();
        String today = getToday();
        
        JLabel noField = new JLabel(add0s(ticketNo));
        JLabel issueDateField = new JLabel(today);
        JTextField firstNameField = new JTextField();
        JTextField lastNameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField durationField = new JTextField();
        // JTextField dueDateField = new JTextField();
    
        // Set font for text fields
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 20);
        noField.setFont(plain);
        issueDateField.setFont(plain);
        firstNameField.setFont(plain);
        lastNameField.setFont(plain);
        phoneField.setFont(plain);
        priceField.setFont(plain);
        durationField.setFont(plain);
        // dueDateField.setFont(plain);
    
        // Create labels
        JLabel noLabel = new JLabel("เลขที่ตั๋ว:");
        JLabel issueDateLabel = new JLabel("วันที่ออกตั๋ว (วว/ดด/ปปปป):");
        JLabel firstNameLabel = new JLabel("ชื่อ:");
        JLabel lastNameLabel = new JLabel("นามสกุล:");
        JLabel phoneLabel = new JLabel("เบอร์โทรศัพท์:");
        JLabel priceLabel = new JLabel("ราคารวม:");
        JLabel durationLabel = new JLabel("ระยะเวลา:");
        // JLabel dueDateLabel = new JLabel("วันครบกำหนด:");
    
        // Set font for labels
        Font bold = new Font("TH Sarabun New", Font.BOLD, 20);
        noLabel.setFont(bold);
        issueDateLabel.setFont(bold);
        firstNameLabel.setFont(bold);
        lastNameLabel.setFont(bold);
        phoneLabel.setFont(bold);
        priceLabel.setFont(bold);
        durationLabel.setFont(bold);
        // dueDateLabel.setFont(bold);
    
        // Create form layout
        Object[] message = {
            noLabel, noField,
            issueDateLabel, issueDateField,
            firstNameLabel, firstNameField,
            lastNameLabel, lastNameField,
            phoneLabel, phoneField,
            priceLabel, priceField,
            durationLabel, durationField,
            // dueDateLabel, dueDateField
        };
    
        // Show the form
        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มตั๋วใหม่", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = db_Connection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, duration, dueDate, status) VALUES ("+"\'"+add0s(ticketNo)+"\'"+", CURDATE(), "+"\'"+firstNameField.getText()+"\'"+", "+"\'"+lastNameField.getText()+"\'"+", "+"\'"+phoneField.getText()+"\'"+", "+Integer.parseInt(priceField.getText())+", "+Integer.parseInt(durationField.getText())+", ADDDATE(CURDATE(), INTERVAL "+Integer.parseInt(durationField.getText())+" MONTH), 'อยู่ระหว่างจำนำ')")) {
    
                pstmt.executeUpdate();
                JLabel success = new JLabel("เพิ่มตั๋วสำเร็จ");
                success.setFont(plain);
                
                JOptionPane.showMessageDialog(this, success, "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);
    
                loadTicket();
    
            } catch (SQLException | IllegalArgumentException e) {
                JLabel fail = new JLabel("เพิ่มตั๋วไม่สำเร็จ");
                fail.setFont(plain);
                JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

    private void editTicket(int row) {
        String ticketNo = tableModel.getValueAt(row, 0).toString();
        String issueDate = tableModel.getValueAt(row, 1).toString();
        String firstName = tableModel.getValueAt(row, 2).toString();
        String lastName = tableModel.getValueAt(row, 3).toString();
        String phoneNumber = tableModel.getValueAt(row, 4).toString();
        int totalPrice = (int) tableModel.getValueAt(row, 5);
        int duration = (int) tableModel.getValueAt(row, 7);
        // String dueDate = tableModel.getValueAt(row, 8).toString();
        String old_ticket_No = tableModel.getValueAt(row, 11) != null ? tableModel.getValueAt(row, 11).toString() : "";
        String new_ticket_No = tableModel.getValueAt(row, 12) != null ? tableModel.getValueAt(row, 12).toString() : "";
        
        // Create text fields and populate them with existing data
        JLabel noField = new JLabel(ticketNo);
        JLabel issueDateField = new JLabel(issueDate);
        JTextField firstNameField = new JTextField(firstName);
        JTextField lastNameField = new JTextField(lastName);
        JTextField phoneField = new JTextField(phoneNumber);
        JTextField priceField = new JTextField(String.valueOf(totalPrice));
        JTextField durationField = new JTextField(String.valueOf(duration));
        // JTextField dueDateField = new JTextField(dueDate);
        JLabel old_ticket_NoField = new JLabel(old_ticket_No);
        JLabel new_ticket_NoField = new JLabel(new_ticket_No);

        // Set font for each text field
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 20);
        noField.setFont(plain);
        issueDateField.setFont(plain);
        firstNameField.setFont(plain);
        lastNameField.setFont(plain);
        phoneField.setFont(plain);
        priceField.setFont(plain);
        durationField.setFont(plain);
        // dueDateField.setFont(plain);
        old_ticket_NoField.setFont(plain);
        new_ticket_NoField.setFont(plain);

        // Create labels
        JLabel noLabel = new JLabel("เลขที่ตั๋ว:");
        JLabel issueDateLabel = new JLabel("วันที่ออกตั๋ว (วว/ดด/ปปปป):");
        JLabel firstNameLabel = new JLabel("ชื่อ:");
        JLabel lastNameLabel = new JLabel("นามสกุล:");
        JLabel phoneLabel = new JLabel("เบอร์โทรศัพท์:");
        JLabel priceLabel = new JLabel("ราคารวม:");
        JLabel durationLabel = new JLabel("ระยะเวลา:");
        // JLabel dueDateLabel = new JLabel("วันครบกำหนด:");
        JLabel old_ticket_NoLabel = new JLabel("เลขที่ตั๋วเก่า:");
        JLabel new_ticket_NoLabel = new JLabel("เลขที่ตั๋วใหม่:");

        // Set font for each label
        Font bold = new Font("TH Sarabun New", Font.BOLD, 20);
        noLabel.setFont(bold);
        issueDateLabel.setFont(bold);
        firstNameLabel.setFont(bold);
        lastNameLabel.setFont(bold);
        phoneLabel.setFont(bold);
        priceLabel.setFont(bold);
        durationLabel.setFont(bold);
        // dueDateLabel.setFont(bold);
        old_ticket_NoLabel.setFont(bold);
        new_ticket_NoLabel.setFont(bold);

        // Create the message array for JOptionPane
        Object[] message = {
            noLabel, noField,
            issueDateLabel, issueDateField,
            firstNameLabel, firstNameField,
            lastNameLabel, lastNameField,
            phoneLabel, phoneField,
            priceLabel, priceField,
            durationLabel, durationField,
            // dueDateLabel, dueDateField,
            old_ticket_NoLabel, old_ticket_NoField,
            new_ticket_NoLabel, new_ticket_NoField
        };

        // Show the form to the user
        int option = JOptionPane.showConfirmDialog(null, message, "แก้ไขข้อมูล", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            // Ask the user if they want to update the ticket
            JLabel isConfirm = new JLabel("คุณต้องการอัปเดตข้อมูลหรือไม่");
            isConfirm.setFont(plain);
            int confirm = JOptionPane.showConfirmDialog(this, isConfirm, "ยืนยันการอัปเดต", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = db_Connection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE ticket SET issueDate = "+"\'"+Date.valueOf(BEtoAD(issueDateField.getText()))+"\'"+", firstName = "+"\'"+firstNameField.getText()+"\'"+", lastName = "+"\'"+lastNameField.getText()+"\'"+", phoneNumber = "+"\'"+phoneField.getText()+"\'"+", totalPrice = "+Integer.parseInt(priceField.getText())+", duration = "+Integer.parseInt(durationField.getText())+", dueDate = ADDDATE("+"\'"+Date.valueOf(BEtoAD(issueDateField.getText()))+"\'"+", INTERVAL duration MONTH) WHERE _No = "+ticketNo)) {

                    
                    pstmt.executeUpdate();

                    // Show success message
                    JLabel success = new JLabel("อัปเดตข้อมูลสำเร็จ");
                    success.setFont(plain);
                    JOptionPane.showMessageDialog(this, success);

                    // Reload the data to refresh the table
                    loadTicket();
                } catch (SQLException | IllegalArgumentException e) {
                    // JLabel fail = new JLabel("อัปเดตข้อมูลไม่สำเร็จ");
                    JLabel fail = new JLabel(e.getMessage());
                    fail.setFont(plain);
                    JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void viewTicket(String ticketNo, String status) {
        SwingUtilities.invokeLater(() -> new TicketDetailPage(ticketNo, status));
    }    

    private void actionPanel(String ticketNo){
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "การดำเนินการตั๋ว", true);
        dialog.setLayout(new BoxLayout(dialog, BoxLayout.Y_AXIS));
        dialog.setSize(280, 300);  // Adjust the size of the popup
        dialog.setLayout(new FlowLayout()); // Use FlowLayout for better control
        dialog.setLocationRelativeTo(this);

        // Button size
        Dimension buttonSize = new Dimension(150, 35); // Width x Height

        // Create buttons
        JButton withdrawAllBtn = new JButton("ถอนออกทั้งหมด");
        JButton renewTicketBtn = new JButton("ต่ออายุ");
        JButton addMoreBtn = new JButton("เพิ่มเงินต้น");
        JButton payBackBtn = new JButton("จ่ายคืนเงินต้น");
        JButton withdrawSomeBtn = new JButton("แบ่งไถ่ถอน");

        // Set button font
        Font bold = new Font("TH Sarabun New", Font.BOLD, 20);
        withdrawAllBtn.setFont(bold);
        renewTicketBtn.setFont(bold);
        addMoreBtn.setFont(bold);
        payBackBtn.setFont(bold);
        withdrawSomeBtn.setFont(bold);

        // Set button size
        withdrawAllBtn.setPreferredSize(buttonSize);
        renewTicketBtn.setPreferredSize(buttonSize);
        addMoreBtn.setPreferredSize(buttonSize);
        payBackBtn.setPreferredSize(buttonSize);
        withdrawSomeBtn.setPreferredSize(buttonSize);

        JLabel fail = new JLabel("ยังไม่เปิดใช้งานโหมดนี้ ขออภัยในความไม่สะดวก");
        fail.setFont(bold);

        // Add action listeners
        withdrawAllBtn.addActionListener(e -> {
            withdrawAll(ticketNo);
            dialog.dispose();
        });

        renewTicketBtn.addActionListener(e -> {
            renewTicket(ticketNo);
            dialog.dispose();
        });

        addMoreBtn.addActionListener(e -> {
            gainPrincipal(ticketNo);
            dialog.dispose();
        });

        payBackBtn.addActionListener(e -> {
            payBackPrincipal(ticketNo);
            dialog.dispose();
        });

        withdrawSomeBtn.addActionListener(e -> {
            withdrawSomeObjects(ticketNo);
            dialog.dispose();
        });

    // Add buttons to dialog
    dialog.add(withdrawAllBtn);
    dialog.add(renewTicketBtn);
    dialog.add(addMoreBtn);
    dialog.add(payBackBtn);
    dialog.add(withdrawSomeBtn);

    // Show dialog
    dialog.setVisible(true);
    }
    
    private String ADtoBE(String _date) {
        // yyyy-mm-dd
        if (_date == null || _date.length() < 10) {
            return "Invalid date";
        }
        String date = _date.substring(8, 10);
        String month = _date.substring(5, 7);
        String year = _date.substring(0, 4);
        int BE = Integer.parseInt(year) + 543;
        return date + "/" + month + "/" + Integer.toString(BE);
    }
    
    private String BEtoAD(String _date) {
        // dd/mm/yyyy
        if (_date == null || _date.length() < 10) {
            return "Invalid date";
        }
        String date = _date.substring(0, 2);
        String month = _date.substring(3, 5);
        String year = _date.substring(6, 10);
        int AD = Integer.parseInt(year) - 543;
        return Integer.toString(AD) + "-" + month + "-" + date;
    }

    private String add0s(int _n){
        String ticketNo = "";
        String n = Integer.toString(_n+1);
        for(int i=0;i<5-n.length();i++){
            ticketNo += "0";
        }
        ticketNo += n;
        return ticketNo;
    }

    private void withdrawAll(String ticketNo){
        JLabel isConfirm = new JLabel("คุณต้องการไถ่ถอนตั๋วหมายเลข "+ticketNo+" หรือไม่");
        isConfirm.setFont(new Font("TH Sarabun New",Font.PLAIN, 20));
        int confirm = JOptionPane.showConfirmDialog(this, isConfirm, "ยืนยันการอัปเดต", JOptionPane.YES_NO_OPTION);
        if(confirm == JOptionPane.YES_OPTION){
            try (Connection conn = db_Connection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(MONTH, issueDate, CURDATE()) AS datediff, totalPrice FROM ticket WHERE _No = ?")) { // Fixed column name
            pstmt.setString(1, ticketNo);
            ResultSet rs = pstmt.executeQuery();
            int datediff = 0;
            int totalPrice = 0;
            String today = getToday();

            if (rs.next()) {    
                datediff = rs.getInt("datediff");
                totalPrice = rs.getInt("totalPrice");
            }

            try (Connection conn1 = db_Connection.getConnection();
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ? WHERE _No = ?")) { // Fixed column name
                    pstmt1.setInt(1, computeInterest(totalPrice, datediff));
                    pstmt1.setString(2, BEtoAD(today));
                    pstmt1.setString(3, "ไถ่ถอนแล้ว");
                    pstmt1.setString(4, ticketNo);
                    pstmt1.executeUpdate();

                } catch (SQLException e) {
                    // JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    JLabel fail = new JLabel(e.getMessage());
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                JLabel success = new JLabel("<html>ไถ่ถอนตั๋วหมายเลข "+ticketNo+" สำเร็จ<br>จำนวนเงินต้น: "+totalPrice+" บาท<br>ดอกเบี้ย: "+((computeInterest(totalPrice, datediff))-totalPrice)+" บาท<br>รวมทั้งหมด "+computeInterest(totalPrice, datediff)+" บาท</html>");
                success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                
                JOptionPane.showMessageDialog(this, success);

                loadTicket();
            } catch (SQLException e) {
                JLabel fail = new JLabel("ไถ่ถอนไม่สำเร็จ");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renewTicket(String ticketNo){
        JLabel isConfirm = new JLabel("คุณต้องการต่ออายุตั๋วหมายเลข "+ticketNo+" หรือไม่");
        isConfirm.setFont(new Font("TH Sarabun New",Font.PLAIN, 20));
        int confirm = JOptionPane.showConfirmDialog(this, isConfirm, "ยืนยันการอัปเดต", JOptionPane.YES_NO_OPTION);
        if(confirm == JOptionPane.YES_OPTION){
            int _No = getLatestNo();

            try (Connection conn = db_Connection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(MONTH, issueDate, CURDATE()) AS datediff, totalPrice FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                int datediff = 0;
                int totalPrice = 0;
                String today = getToday();

                if (rs.next()) {    
                    datediff = rs.getInt("datediff");
                    totalPrice = rs.getInt("totalPrice");
                }

                try (Connection conn1 = db_Connection.getConnection();
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = ?")) { // Fixed column name
                    pstmt1.setInt(1, computeInterest(totalPrice, datediff));
                    pstmt1.setString(2, BEtoAD(today));
                    pstmt1.setString(3, "ต่ออายุแล้ว");
                    pstmt1.setString(4, add0s(_No));
                    pstmt1.setString(5, ticketNo);
                    pstmt1.executeUpdate();

                } catch (SQLException e) {
                    JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                try (Connection conn2 = db_Connection.getConnection();
                    PreparedStatement pstmt2 = conn.prepareStatement("SELECT CURDATE() AS today, firstName, lastName, phoneNumber, totalPrice, duration, ADDDATE(CURDATE(), INTERVAL duration MONTH) AS dueDate FROM ticket WHERE _No = ?")) {
                        pstmt2.setString(1, ticketNo);
                    ResultSet rs1 = pstmt2.executeQuery();

                    if(rs1.next()){
                        Object[] row = {
                            rs1.getDate("today"),
                            rs1.getString("firstName"),
                            rs1.getString("lastName"),
                            rs1.getString("phoneNumber"),
                            rs1.getInt("totalPrice"),
                            rs1.getInt("duration"),
                            rs1.getDate("dueDate")
                        };
                        try (Connection conn3 = db_Connection.getConnection();
                            PreparedStatement pstmt3 = conn3.prepareStatement(
                                "INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                            )) {
                                
                                pstmt3.setString(1, add0s(_No));
                                pstmt3.setDate(2, Date.valueOf(row[0].toString()));
                                pstmt3.setString(3, row[1].toString());
                                pstmt3.setString(4, row[2].toString());
                                pstmt3.setString(5, row[3].toString());
                                pstmt3.setInt(6, (int) row[4]);
                                pstmt3.setInt(7, (int) row[5]);
                                pstmt3.setDate(8, Date.valueOf(row[6].toString()));
                                pstmt3.setString(9, "อยู่ระหว่างจำนำ");
                                pstmt3.setString(10, ticketNo);

                                pstmt3.executeUpdate();
                        }
                    }

                } catch (SQLException e) {
                    JLabel fail = new JLabel(e.getMessage());
                    // JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                copyObjs(ticketNo, add0s(_No));

                JLabel success = new JLabel("<html>ต่ออายุตั๋วหมายเลข "+ticketNo+" สำเร็จ<br>จำนวนเงินต้น: "+totalPrice+" บาท<br>ดอกเบี้ย: "+((computeInterest(totalPrice, datediff))-totalPrice)+" บาท<br>รวมทั้งหมด "+computeInterest(totalPrice, datediff)+" บาท</html>");
                success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                
                JOptionPane.showMessageDialog(this, success);

                loadTicket();
            } catch (SQLException e) {
                JLabel fail = new JLabel("ต่ออายุไม่สำเร็จ");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
        
    private void gainPrincipal(String ticketNo){
        JLabel reqLabel = new JLabel("จำนวนเงินที่ขอเพิ่ม:");
        JLabel durLabel = new JLabel("ระยะเวลา:");

        reqLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        durLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));

        JTextField reqField = new JTextField();
        JTextField durField = new JTextField();

        reqField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        durField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));

        int req = 0,dur = 0;
        
        Object message[] = {reqLabel, reqField,
                            durLabel, durField};
        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มเงินต้น", JOptionPane.YES_NO_OPTION);

        
        if (option == JOptionPane.YES_OPTION) {
            req = Integer.parseInt(reqField.getText());
            dur = Integer.parseInt(durField.getText());

            int _No = getLatestNo();
            

            try (Connection conn = db_Connection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(MONTH, issueDate, CURDATE()) AS datediff, totalPrice FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                int datediff = 0;
                int totalPrice = 0;
                String today = getToday();

                if (rs.next()) {    
                    datediff = rs.getInt("datediff");
                    totalPrice = rs.getInt("totalPrice");
                }

                try (Connection conn1 = db_Connection.getConnection();
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = ?")) { // Fixed column name
                    pstmt1.setInt(1, computeInterest(totalPrice, datediff));
                    pstmt1.setString(2, BEtoAD(today));
                    pstmt1.setString(3, "เพิ่มเงินต้นจำนวน "+req+" บาทแล้ว");
                    pstmt1.setString(4, add0s(_No));
                    pstmt1.setString(5, ticketNo);
                    pstmt1.executeUpdate();

                } catch (SQLException e) {
                    JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                try (Connection conn2 = db_Connection.getConnection();
                    PreparedStatement pstmt2 = conn.prepareStatement("SELECT CURDATE() AS today, firstName, lastName, phoneNumber FROM ticket WHERE _No = ?")) {
                        pstmt2.setString(1, ticketNo);
                    ResultSet rs1 = pstmt2.executeQuery();

                    if(rs1.next()){
                        Object[] row = {
                            rs1.getDate("today"),
                            rs1.getString("firstName"),
                            rs1.getString("lastName"),
                            rs1.getString("phoneNumber"),
                        };
                        try (Connection conn3 = db_Connection.getConnection();
                            PreparedStatement pstmt3 = conn.prepareStatement("INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ADDDATE(CURDATE(), INTERVAL "+dur+" MONTH), ?, ?)")) {
                                
                            pstmt3.setString(1, add0s(_No));
                            pstmt3.setDate(2, Date.valueOf(row[0].toString()));
                            pstmt3.setString(3, row[1].toString());
                            pstmt3.setString(4, row[2].toString());
                            pstmt3.setString(5, row[3].toString());
                            pstmt3.setInt(6, totalPrice+req);
                            pstmt3.setInt(7, dur);
                            pstmt3.setString(8, "อยู่ระหว่างจำนำ");
                            pstmt3.setString(9, ticketNo);

                            pstmt3.executeUpdate();
                        }
                    }

                } catch (SQLException e) {
                    JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                copyObjs(ticketNo, add0s(_No));

                
                JLabel success = new JLabel("<html>เพิ่มเงินตั๋วหมายเลข " + ticketNo + "<br>" +
                        "จำนวน " + req + " สำเร็จ<br>" +
                        "จำนวนเงินต้นหลังเพิ่ม: " + (totalPrice + req) + " บาท<br>" +
                        "จำนวนเงินต้นที่เพิ่มหลังหักดอกเบี้ย: " + ((req) - ((computeInterest(totalPrice, datediff)) - totalPrice)) + " บาท<br>" +
                        "ดอกเบี้ย: " + ((computeInterest(totalPrice, datediff)) - totalPrice) + " บาท<br>" +
                        "รวมทั้งหมด " + computeInterest(totalPrice, datediff) + " บาท</html>");

                success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                JOptionPane.showMessageDialog(this, success);

                loadTicket();
            } catch (SQLException e) {
                JLabel fail = new JLabel("เพิ่มเงินต้นไม่สำเร็จ");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } 

    }

    private void payBackPrincipal(String ticketNo){
        int principal = 0;
        try (Connection conn = db_Connection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT totalPrice FROM ticket WHERE _No = "+ticketNo)) {

            if (rs.next()) {
                principal = rs.getInt("totalPrice");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        JLabel reqLabel = new JLabel("จำนวนเงินที่จะจ่ายคืน:");
        JLabel durLabel = new JLabel("ระยะเวลา:");

        reqLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        durLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));

        JTextField reqField = new JTextField();
        JTextField durField = new JTextField();

        reqField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        durField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));

        int req = 0,dur = 0;
        int option;
        
        do{
            Object message[] = {reqLabel, reqField, durLabel, durField};    
            option = JOptionPane.showConfirmDialog(this, message, "จ่ายคืนเงินต้น", JOptionPane.YES_NO_OPTION);
        }while((Integer.parseInt(reqField.getText())>=principal)&&(option == JOptionPane.YES_OPTION));
        
        if (option == JOptionPane.YES_OPTION) {
            req = Integer.parseInt(reqField.getText());
            dur = Integer.parseInt(durField.getText());

            int _No = getLatestNo();

            try (Connection conn = db_Connection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(MONTH, issueDate, CURDATE()) AS datediff, totalPrice FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                int datediff = 0;
                int totalPrice = 0;
                String today = getToday();

                if (rs.next()) {    
                    datediff = rs.getInt("datediff");
                    totalPrice = rs.getInt("totalPrice");
                }

                try (Connection conn1 = db_Connection.getConnection();
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = "+ticketNo)) { // Fixed column name
                    pstmt1.setInt(1, computeInterest(totalPrice, datediff));
                    pstmt1.setString(2, BEtoAD(today));
                    pstmt1.setString(3, "ลดเงินต้นจำนวน "+req+" บาทแล้ว");
                    pstmt1.setString(4, add0s(_No));
                    pstmt1.executeUpdate();

                } catch (SQLException e) {
                    JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                try (Connection conn2 = db_Connection.getConnection();
                    PreparedStatement pstmt2 = conn.prepareStatement("SELECT CURDATE() AS today, firstName, lastName, phoneNumber FROM ticket WHERE _No = ?")) {
                        pstmt2.setString(1, ticketNo);
                    ResultSet rs1 = pstmt2.executeQuery();

                    if(rs1.next()){
                        Object[] row = {
                            rs1.getDate("today"),
                            rs1.getString("firstName"),
                            rs1.getString("lastName"),
                            rs1.getString("phoneNumber"),
                        };
                        try (Connection conn3 = db_Connection.getConnection();
                            PreparedStatement pstmt3 = conn.prepareStatement("INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ADDDATE(CURDATE(), INTERVAL "+dur+" MONTH), ?, ?)")) {
                                
                                pstmt3.setString(1, add0s(_No));
                                pstmt3.setDate(2, Date.valueOf(row[0].toString()));
                                pstmt3.setString(3, row[1].toString());
                                pstmt3.setString(4, row[2].toString());
                                pstmt3.setString(5, row[3].toString());
                                pstmt3.setInt(6, totalPrice-req);
                                pstmt3.setInt(7, dur);
                                // pstmt3.setDate(8, Date.valueOf(row[5].toString()));
                                pstmt3.setString(8, "อยู่ระหว่างจำนำ");
                                pstmt3.setString(9, ticketNo);

                                pstmt3.executeUpdate();
                        }
                    }

                } catch (SQLException e) {
                    JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                copyObjs(ticketNo, add0s(_No));                

                JLabel success = new JLabel("<html>ลดเงินต้นตั๋วหมายเลข " + ticketNo + "<br>" +
                        "จำนวน " + req + " สำเร็จ<br>" +
                        "จำนวนเงินต้นหลังลด: " + (totalPrice - req) + " บาท<br>" +
                        "รับเงินทั้งหมด: " + (req + ((computeInterest(totalPrice, datediff)) - totalPrice)) + " บาท<br>" +
                        "ดอกเบี้ย: " + ((computeInterest(totalPrice, datediff)) - totalPrice) + " บาท<br>" +
                        "รวมทั้งหมด " + computeInterest(totalPrice, datediff) + " บาท</html>");

                success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                JOptionPane.showMessageDialog(this, success);

                loadTicket();
            } catch (SQLException e) {
                JLabel fail = new JLabel("เพิ่มเงินต้นไม่สำเร็จ");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void withdrawSomeObjects(String ticketNo){
        String msg="รายการสิ่งของที่อยู่ในระหว่างการจำนำ <br><table><tr><th>ลำดับที่</th><th>จำนวน</th><th>สินค้า</th><th>น้ำหนัก</th><th>ราคา</th></tr>";
        try (Connection conn = db_Connection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT obj_id, amount, object, weight, price FROM objects WHERE ticketNo = "+ticketNo)) {

            if (rs.next()) {
                int obj_id = rs.getInt("obj_id");
                int amount = rs.getInt("amount");
                String object = rs.getString("object");
                double weight = rs.getDouble("weight");
                int price = rs.getInt("price");
                msg += "<tr>"+
                "<td>"+obj_id+"</td>"+"<td>"+amount+"</td>"+"<td>"+object+"</td>"+"<td>"+weight+"</td>"+"<td>"+price+"</td>"+
                "</tr>";
            }

            msg += "</table>";
                
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int computeInterest(int principal, int _datediff) {
//         SELECT
//     TIMESTAMPDIFF(MONTH, '2025-02-01', CURDATE()) AS months_total,
//     DATEDIFF(CURDATE(), DATE_ADD('2025-02-01', INTERVAL TIMESTAMPDIFF(MONTH, '2025-02-01', CURDATE()) MONTH)) AS extra_days
// FROM ticket WHERE _No = '00001';
        if (principal >= 10000) {
            // 1-3 วัน = 0.75%, 4-10 วัน = 1%
            return principal + roundUp((int)(principal * 0.0125)) * (_datediff > 0 ? _datediff : 1);
        } else if (principal < 10000 && principal > 2500) {
            // ไม่เกิน 10 วัน = 1%
            return principal + roundUp((int)(principal * 0.015)) * (_datediff > 0 ? _datediff : 1);
        } else {
            // เกินมา 1 วัน คิดเต็มเดือน
            return principal + roundUp((int)(principal * 0.02)) * (_datediff > 0 ? _datediff : 1);
        }
    }

    private int roundUp(int interest) {
        return (interest % 10 == 0) ? interest : (interest + (10 - interest % 10));
    }

    private int getLatestNo(){

        int _No = 0;

        try (Connection conn = db_Connection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(_No) AS cnt FROM ticket")) {

            if (rs.next()) {
                _No = rs.getInt("cnt");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return _No;
    }

    private String getToday(){
        String today = "";
        try (Connection conn = db_Connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURDATE() as today")) {

            if (rs.next()) {
                today = rs.getDate("today").toString();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return ADtoBE(today);
    }

    private void copyObjs(String old_ticket_No, String new_ticket_No){
        try (Connection conn = db_Connection.getConnection();
            PreparedStatement getObjects = conn.prepareStatement("SELECT amount, object, weight, price FROM objects WHERE ticketNo = "+old_ticket_No)) { // Fixed column name
            ResultSet rsObject = getObjects.executeQuery();
        
            while(rsObject.next()){
                Object[] allObject = {
                    rsObject.getInt("amount"), 
                    rsObject.getString("object"), 
                    rsObject.getDouble("weight"), 
                    rsObject.getInt("price")
                };

                try (Connection conn1 = db_Connection.getConnection();
                    PreparedStatement getObj_id = conn1.prepareStatement("SELECT COUNT(obj_id) AS cnt FROM objects WHERE ticketNo = "+new_ticket_No);
                    PreparedStatement insertObject = conn1.prepareStatement("INSERT INTO objects VALUE (?, ?, ?, ?, ?, ?)")) { // Fixed column name

                        ResultSet rsobj_id = getObj_id.executeQuery();

                        int obj_id = 0;
                        if(rsobj_id.next()) obj_id = rsobj_id.getInt("cnt");

                        insertObject.setString(1,new_ticket_No);
                        insertObject.setInt(2,obj_id+1);
                        insertObject.setInt(3, (int) allObject[0]);
                        insertObject.setString(4, allObject[1].toString());
                        insertObject.setDouble(5, (double) allObject[2]);
                        insertObject.setInt(6, (int) allObject[3]);

                        insertObject.executeUpdate();
                        
                }catch (SQLException e) {
                    JLabel fail = new JLabel("คัดลอกสินค้าจากตั๋วหมายเลข "+old_ticket_No+" ไม่สำเร็จ");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
            fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void getSummary(){
        try (Connection conn = db_Connection.getConnection();
             PreparedStatement stmtAll = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket WHERE status = 'อยู่ระหว่างจำนำ'");
             PreparedStatement stmtToday = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket WHERE issueDate = CURDATE()");
             PreparedStatement stmtToday1 = conn.prepareStatement("SELECT SUM(redemptPrice) AS todayPriceWithdraw, COUNT(_No) AS todayQtyWithdraw FROM ticket WHERE redemptDate = CURDATE() AND (status = 'ไถ่ถอนแล้ว' OR status = 'ต่ออายุแล้ว' OR status LIKE 'เพิ่มเงินต้นจำนวน%' OR status LIKE 'ลดเงินต้นจำนวน%')")) {

            int netTotalPriceAllDays = 0, totalQuantityAllDays = 0;
            int netTotalPriceToday = 0, totalQuantityToday = 0;
            int todayPriceWithdraw = 0, todayQtyWithdraw = 0;

            // Fetch all-time summary
            ResultSet rsAll = stmtAll.executeQuery();
            if (rsAll.next()) {
                netTotalPriceAllDays = rsAll.getInt("net_totalPrice");
                totalQuantityAllDays = rsAll.getInt("totalQuantity");
            }

            // Fetch today's summary
            ResultSet rsToday = stmtToday.executeQuery();
            if (rsToday.next()) {
                netTotalPriceToday = rsToday.getInt("net_totalPrice");
                totalQuantityToday = rsToday.getInt("totalQuantity");
            }

            ResultSet rsWithdraw = stmtToday1.executeQuery();
            if (rsWithdraw.next()) {
                todayPriceWithdraw = rsWithdraw.getInt("todayPriceWithdraw");
                todayQtyWithdraw = rsWithdraw.getInt("todayQtyWithdraw");
            }

            // Display the summary
            this.netSummary.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
            this.todayPawn.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
            this.todayWithdraw.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
            this.netSummary.setText("ยอดสุทธิ: " + netTotalPriceAllDays + " จำนวนห่อทั้งหมด: " + totalQuantityAllDays);
            this.todayPawn.setText("ยอดจำนำวันนี้: " + netTotalPriceToday + " จำนวนห่อวันนี้: " + totalQuantityToday);
            this.todayWithdraw.setText("ยอดไถ่คืนวันนี้: " + todayPriceWithdraw + " จำนวนห่อไถ่คืนวันนี้: " + todayQtyWithdraw);

        } catch (SQLException e) {
            JLabel fail = new JLabel("โหลดข้อมูลไม่สำเร็จ");
            fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void overdueUpdate(){
        try (Connection conn = db_Connection.getConnection();
            PreparedStatement pstmtUpdate = conn.prepareStatement(
            "UPDATE ticket SET status = 'เกินกำหนดแล้ว' WHERE dueDate < CURDATE() AND status = 'อยู่ระหว่างจำนำ'"
            )) {
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            JLabel fail = new JLabel("อัปเดตตั๋วที่เกินกำหนดล้มเหลว");
            fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
            JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


}
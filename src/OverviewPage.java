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

                if (column == 13 && status.equals("อยู่ระหว่างจำนำ")) {
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

        try (Connection conn = db_Connection.getConnection();
             PreparedStatement stmtAll = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket WHERE status = 'อยู่ระหว่างจำนำ'");
             PreparedStatement stmtToday = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket WHERE issueDate = CURDATE()");
             PreparedStatement stmtToday1 = conn.prepareStatement("SELECT SUM(redemptPrice) AS todayPriceWithdraw, COUNT(_No) AS todayQtyWithdraw FROM ticket WHERE redemptDate = CURDATE() AND status = 'ไถ่ถอนแล้ว'")) {

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
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }

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
                    rs.getDate("redemptDate") != null ? ADtoBE(rs.getDate("redemptDate").toString()) : "-",
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
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTicket(){
        int ticketNo = 0;
        try (Connection conn = db_Connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(_No) AS cnt FROM ticket")) {

            if (rs.next()) {
                ticketNo = rs.getInt("cnt");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        JLabel noField = new JLabel(add0s(ticketNo));
        JTextField issueDateField = new JTextField();
        JTextField firstNameField = new JTextField();
        JTextField lastNameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField durationField = new JTextField();
        JTextField dueDateField = new JTextField();
        JTextField old_ticket_NoField = new JTextField();
        JTextField new_ticket_NoField = new JTextField();
    
        // Set font for text fields
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 18);
        noField.setFont(plain);
        issueDateField.setFont(plain);
        firstNameField.setFont(plain);
        lastNameField.setFont(plain);
        phoneField.setFont(plain);
        priceField.setFont(plain);
        durationField.setFont(plain);
        dueDateField.setFont(plain);
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
        JLabel dueDateLabel = new JLabel("วันครบกำหนด:");
        JLabel old_ticket_NoLabel = new JLabel("เลขที่ตั๋วเก่า:");
        JLabel new_ticket_NoLabel = new JLabel("เลขที่ตั๋วใหม่:");
    
        // Set font for labels
        Font bold = new Font("TH Sarabun New", Font.BOLD, 18);
        noLabel.setFont(bold);
        issueDateLabel.setFont(bold);
        firstNameLabel.setFont(bold);
        lastNameLabel.setFont(bold);
        phoneLabel.setFont(bold);
        priceLabel.setFont(bold);
        durationLabel.setFont(bold);
        dueDateLabel.setFont(bold);
        old_ticket_NoLabel.setFont(bold);
        new_ticket_NoLabel.setFont(bold);
    
        // Create form layout
        Object[] message = {
            noLabel, noField,
            issueDateLabel, issueDateField,
            firstNameLabel, firstNameField,
            lastNameLabel, lastNameField,
            phoneLabel, phoneField,
            priceLabel, priceField,
            durationLabel, durationField,
            dueDateLabel, dueDateField,
            old_ticket_NoLabel, old_ticket_NoField,
            new_ticket_NoLabel, new_ticket_NoField
        };
    
        // Show the form
        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มตั๋วใหม่", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            try (Connection conn = db_Connection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, duration, dueDate, status, old_ticket_No, new_ticket_No) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
    
                // Validate input values
                pstmt.setString(1, add0s(ticketNo));
                pstmt.setDate(2, Date.valueOf(BEtoAD(issueDateField.getText()))); // Ensure valid date format
                pstmt.setString(3, firstNameField.getText());
                pstmt.setString(4, lastNameField.getText());
                pstmt.setString(5, phoneField.getText());
                pstmt.setInt(6, Integer.parseInt(priceField.getText())); // Ensure valid integer
                pstmt.setInt(7, Integer.parseInt(durationField.getText())); // Ensure valid integer
                pstmt.setDate(8, Date.valueOf(BEtoAD(dueDateField.getText()))); // Ensure valid date format
                pstmt.setString(9, "อยู่ระหว่างจำนำ");
                pstmt.setString(10, old_ticket_NoField.getText());
                pstmt.setString(11, new_ticket_NoField.getText());
    
                // Execute the insert operation
                pstmt.executeUpdate();
                JLabel success = new JLabel("เพิ่มตั๋วสำเร็จ");
                success.setFont(plain);
                
                JOptionPane.showMessageDialog(this, success, "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);
    
                // Refresh the table
                loadTicket();
    
            } catch (SQLException | IllegalArgumentException e) {
                JLabel fail = new JLabel("เพิ่มตั๋วไม่สำเร็จ");
                fail.setFont(plain);
                
                JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

    private void editTicket(int row) {
        // Get the data from the ticket at the clicked row
        String ticketNo = tableModel.getValueAt(row, 0).toString();
        String issueDate = tableModel.getValueAt(row, 1).toString();
        String firstName = tableModel.getValueAt(row, 2).toString();
        String lastName = tableModel.getValueAt(row, 3).toString();
        String phoneNumber = tableModel.getValueAt(row, 4).toString();
        int totalPrice = (int) tableModel.getValueAt(row, 5);
        int duration = (int) tableModel.getValueAt(row, 7);
        String dueDate = tableModel.getValueAt(row, 8).toString();
        String old_ticket_No = "";
        String new_ticket_No = "";
        if(tableModel.getValueAt(row, 11).toString() != null){
            old_ticket_No = tableModel.getValueAt(row, 11).toString();    
        }
        if(tableModel.getValueAt(row, 12).toString() != null){
            new_ticket_No = tableModel.getValueAt(row, 12).toString();
        }
        

        // Create text fields and populate them with existing data
        JLabel noField = new JLabel(ticketNo);
        JTextField issueDateField = new JTextField(issueDate);
        JTextField firstNameField = new JTextField(firstName);
        JTextField lastNameField = new JTextField(lastName);
        JTextField phoneField = new JTextField(phoneNumber);
        JTextField priceField = new JTextField(String.valueOf(totalPrice));
        JTextField durationField = new JTextField(String.valueOf(duration));
        JTextField dueDateField = new JTextField(dueDate);
        JTextField old_ticket_NoField = new JTextField(old_ticket_No);
        JTextField new_ticket_NoField = new JTextField(new_ticket_No);

        // Set font for each text field
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 18);
        noField.setFont(plain);
        issueDateField.setFont(plain);
        firstNameField.setFont(plain);
        lastNameField.setFont(plain);
        phoneField.setFont(plain);
        priceField.setFont(plain);
        durationField.setFont(plain);
        dueDateField.setFont(plain);
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
        JLabel dueDateLabel = new JLabel("วันครบกำหนด:");
        JLabel old_ticket_NoLabel = new JLabel("เลขที่ตั๋วเก่า:");
        JLabel new_ticket_NoLabel = new JLabel("เลขที่ตั๋วใหม่:");

        // Set font for each label
        Font bold = new Font("TH Sarabun New", Font.BOLD, 18);
        noLabel.setFont(bold);
        issueDateLabel.setFont(bold);
        firstNameLabel.setFont(bold);
        lastNameLabel.setFont(bold);
        phoneLabel.setFont(bold);
        priceLabel.setFont(bold);
        durationLabel.setFont(bold);
        dueDateLabel.setFont(bold);
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
            dueDateLabel, dueDateField,
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
                        "UPDATE ticket SET issueDate = ?, firstName = ?, lastName = ?, phoneNumber = ?, totalPrice = ?, duration = ?, dueDate = ?, old_ticket_No = ?, new_ticket_No = ? WHERE _No = ?")) {

                    // Set the new values from the text fields
                    pstmt.setDate(1, Date.valueOf(BEtoAD(issueDateField.getText())));
                    pstmt.setString(2, firstNameField.getText());
                    pstmt.setString(3, lastNameField.getText());
                    pstmt.setString(4, phoneField.getText());
                    pstmt.setInt(5, Integer.parseInt(priceField.getText()));
                    pstmt.setInt(6, Integer.parseInt(durationField.getText()));
                    pstmt.setDate(7, Date.valueOf(BEtoAD(dueDateField.getText())));
                    pstmt.setString(8, old_ticket_NoField.getText());
                    pstmt.setString(9, new_ticket_NoField.getText());
                    pstmt.setString(10, ticketNo);

                    // Execute the update
                    pstmt.executeUpdate();

                    // Show success message
                    JLabel success = new JLabel("อัปเดตข้อมูลสำเร็จ");
                    success.setFont(plain);
                    JOptionPane.showMessageDialog(this, success);

                    // Reload the data to refresh the table
                    loadTicket();
                } catch (SQLException | IllegalArgumentException e) {
                    // Show error message
                    e.printStackTrace();
                    JLabel fail = new JLabel("อัปเดตข้อมูลไม่สำเร็จ");
                    fail.setFont(plain);
                    // JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            // renewTicket(ticketNo);
            // dialog.dispose();
        });

        addMoreBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            // gainPrincipal(ticketNo);
            // dialog.dispose();
        });

        payBackBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            // payBackPrincipal(ticketNo);
            // dialog.dispose();
        });

        withdrawSomeBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            // withdrawSomeObjects(ticketNo);
            // dialog.dispose();
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
        try (Connection conn = db_Connection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT TIMESTAMPDIFF(MONTH, issueDate, CURDATE()) AS datediff, CURDATE() AS today, totalPrice FROM ticket WHERE _No = ?")) { // Fixed column name
            pstmt.setString(1, ticketNo);
            ResultSet rs = pstmt.executeQuery();
            int datediff = 0;
            int totalPrice = 0;
            String today = "";

            if (rs.next()) {    
                datediff = rs.getInt("datediff");
                totalPrice = rs.getInt("totalPrice");
                today = rs.getDate("today").toString();
            }

            try (Connection conn1 = db_Connection.getConnection();
            PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ? WHERE _No = ?")) { // Fixed column name
                pstmt1.setInt(1, computeInterest(totalPrice, datediff));
                pstmt1.setString(2, today);
                pstmt1.setString(3, "ไถ่ถอนแล้ว");
                pstmt1.setString(4, ticketNo);
                pstmt1.executeUpdate();

            } catch (SQLException e) {
                JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }

            JLabel success = new JLabel("ไถ่ถอนสำเร็จ");
            success.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            JOptionPane.showMessageDialog(this, success);

            loadTicket();
        } catch (SQLException e) {
            e.printStackTrace();
            JLabel fail = new JLabel("ไถ่ถอนไม่สำเร็จ");
            fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            // JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private int computeInterest(int principal, int _datediff) {
        if (principal >= 10000) {
            return principal + roundUp((int)(principal * 0.0125)) * (_datediff > 0 ? _datediff : 1);
        } else if (principal < 10000 && principal >= 4000) {
            return principal + roundUp((int)(principal * 0.015)) * (_datediff > 0 ? _datediff : 1);
        } else {
            return principal + roundUp((int)(principal * 0.02)) * (_datediff > 0 ? _datediff : 1);
        }
    }

    private int roundUp(int interest) {
        return (interest % 10 == 0) ? interest : (interest + (10 - interest % 10));
    }
}

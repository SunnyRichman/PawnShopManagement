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

    private void addTicket() {
        int ticketNo = getLatestNo();
        String today = getToday();
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 20);

        JLabel noField = new JLabel(add0s(ticketNo));   noField.setFont(plain);
        JLabel issueDateField = new JLabel(today);      issueDateField.setFont(plain);
        JTextField firstNameField = new JTextField();   firstNameField.setFont(plain);
        JTextField lastNameField = new JTextField();    lastNameField.setFont(plain);
        JTextField phoneField = new JTextField();       phoneField.setFont(plain);
        JTextField priceField = new JTextField();       priceField.setFont(plain);
        JTextField rateField = new JTextField();        rateField.setFont(plain);
        JTextField durationField = new JTextField();    durationField.setFont(plain);

        // Create labels
        Font bold = new Font("TH Sarabun New", Font.BOLD, 20);
        JLabel noLabel = new JLabel("เลขที่ตั๋ว:"); noLabel.setFont(bold);
        JLabel issueDateLabel = new JLabel("วันที่ออกตั๋ว (วว/ดด/ปปปป):");   issueDateLabel.setFont(bold);
        JLabel firstNameLabel = new JLabel("ชื่อ:"); firstNameLabel.setFont(bold);
        JLabel lastNameLabel = new JLabel("นามสกุล:"); lastNameLabel.setFont(bold);
        JLabel phoneLabel = new JLabel("เบอร์โทรศัพท์:"); phoneLabel.setFont(bold);
        JLabel priceLabel = new JLabel("ราคารวม:"); priceLabel.setFont(bold);
        JLabel rateLabel = new JLabel("อัตราดอกเบี้ย (%):"); rateLabel.setFont(bold);
        JLabel durationLabel = new JLabel("ระยะเวลา (เดือน):"); durationLabel.setFont(bold);

        // Layout
        Object[] message = {
            noLabel, noField,
            issueDateLabel, issueDateField,
            firstNameLabel, firstNameField,
            lastNameLabel, lastNameField,
            phoneLabel, phoneField,
            priceLabel, priceField,
            rateLabel, rateField,
            durationLabel, durationField,
        };

        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มตั๋วใหม่", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                // Input validation
                if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                    || phoneField.getText().isEmpty() || priceField.getText().isEmpty()
                    || durationField.getText().isEmpty()) {
                    throw new IllegalArgumentException("กรุณากรอกข้อมูลให้ครบถ้วน");
                }

                int totalPrice = Integer.parseInt(priceField.getText());
                int duration = Integer.parseInt(durationField.getText());
                double rate;

                if ((rateField.getText()).isEmpty()) {
                    // Default rate based on totalPrice
                    if (totalPrice >= 10000) {
                        rate = 1.25;
                    } else if (totalPrice > 2500) {
                        rate = 1.5;
                    } else {
                        rate = 2.0;
                    }
                } else {
                    rate = Double.parseDouble(rateField.getText());
                }

                try (Connection conn = db_Connection.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, rate, duration, dueDate, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ADDDATE(CURDATE(), INTERVAL ? MONTH), 'อยู่ระหว่างจำนำ')")) {

                    pstmt.setString(1, add0s(ticketNo));
                    pstmt.setString(2, BEtoAD(today));
                    pstmt.setString(3, firstNameField.getText());
                    pstmt.setString(4, lastNameField.getText());
                    pstmt.setString(5, phoneField.getText());
                    pstmt.setInt(6, totalPrice);
                    pstmt.setDouble(7, rate);
                    pstmt.setInt(8, duration);
                    pstmt.setInt(9, duration); // for dueDate

                    pstmt.executeUpdate();

                    JLabel success = new JLabel("เพิ่มตั๋วสำเร็จ");
                    success.setFont(plain);
                    JOptionPane.showMessageDialog(this, success, "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);

                    loadTicket();
                }

            } catch (NumberFormatException e) {
                JLabel fail = new JLabel("กรุณากรอกข้อมูลตัวเลขให้ถูกต้อง");
                fail.setFont(plain);
                JOptionPane.showMessageDialog(this, fail, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);

            } catch (IllegalArgumentException | SQLException e) {
                JLabel fail = new JLabel("<html>เพิ่มตั๋วไม่สำเร็จ<br>ข้อผิดพลาด: " + e.getMessage() + "</html>");
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
        String old_ticket_No = tableModel.getValueAt(row, 11) != null ? tableModel.getValueAt(row, 11).toString() : "";
        String new_ticket_No = tableModel.getValueAt(row, 12) != null ? tableModel.getValueAt(row, 12).toString() : "";
        
        // Create text fields and populate them with existing data
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 20);
        Font bold = new Font("TH Sarabun New", Font.BOLD, 20);

        JLabel noField = new JLabel(ticketNo);                                  noField.setFont(plain);
        JLabel issueDateField = new JLabel(issueDate);                          issueDateField.setFont(plain);
        JTextField firstNameField = new JTextField(firstName);                  firstNameField.setFont(plain);
        JTextField lastNameField = new JTextField(lastName);                    lastNameField.setFont(plain);
        JTextField phoneField = new JTextField(phoneNumber);                    phoneField.setFont(plain);
        JTextField priceField = new JTextField(String.valueOf(totalPrice));     priceField.setFont(plain);
        JTextField rateField = new JTextField();                                rateField.setFont(plain);
        JTextField durationField = new JTextField(String.valueOf(duration));    durationField.setFont(plain);
        JLabel old_ticket_NoField = new JLabel(old_ticket_No);                  old_ticket_NoField.setFont(plain);
        JLabel new_ticket_NoField = new JLabel(new_ticket_No);                  new_ticket_NoField.setFont(plain);

        JLabel noLabel = new JLabel("เลขที่ตั๋ว:"); noLabel.setFont(bold);
        JLabel issueDateLabel = new JLabel("วันที่ออกตั๋ว (วว/ดด/ปปปป):");   issueDateLabel.setFont(bold);
        JLabel firstNameLabel = new JLabel("ชื่อ:");  firstNameLabel.setFont(bold);
        JLabel lastNameLabel = new JLabel("นามสกุล:");   lastNameLabel.setFont(bold);
        JLabel phoneLabel = new JLabel("เบอร์โทรศัพท์:");  phoneLabel.setFont(bold);
        JLabel priceLabel = new JLabel("ราคารวม:"); priceLabel.setFont(bold);
        JLabel rateLabel = new JLabel("อัตราดอกเบี้ย (%):"); rateLabel.setFont(bold);
        JLabel durationLabel = new JLabel("ระยะเวลา:"); durationLabel.setFont(bold);
        JLabel old_ticket_NoLabel = new JLabel("เลขที่ตั๋วเก่า:");   old_ticket_NoLabel.setFont(bold);
        JLabel new_ticket_NoLabel = new JLabel("เลขที่ตั๋วใหม่:");   new_ticket_NoLabel.setFont(bold);

        // Create the message array for JOptionPane
        Object[] message = {
            noLabel, noField,
            issueDateLabel, issueDateField,
            firstNameLabel, firstNameField,
            lastNameLabel, lastNameField,
            phoneLabel, phoneField,
            priceLabel, priceField,
            durationLabel, durationField,
            old_ticket_NoLabel, old_ticket_NoField,
            new_ticket_NoLabel, new_ticket_NoField
        };

        double rate = 0.0;
        if ((rateField.getText()).isEmpty()) {
            // Default rate based on totalPrice
            if (totalPrice >= 10000) {
                rate = 1.25;
            } else if (totalPrice >= 2500) {
                rate = 1.5;
            } else {
                rate = 2.0;
            }
        } else {
            rate = Double.parseDouble(rateField.getText());
        }
        // Show the form to the user
        int option = JOptionPane.showConfirmDialog(null, message, "แก้ไขข้อมูล", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            // Ask the user if they want to update the ticket

            JLabel isConfirm = new JLabel("คุณต้องการอัปเดตข้อมูลหรือไม่");
            isConfirm.setFont(plain);
            int confirm = JOptionPane.showConfirmDialog(this, isConfirm, "ยืนยันการอัปเดต", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {

                try (Connection conn = db_Connection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement("UPDATE ticket SET issueDate = ?, firstName = ?, lastName = ?, phoneNumber = ?, totalPrice = ?, duration = ?, rate = ?, dueDate = ADDDATE(?, INTERVAL ? MONTH) WHERE _No = ?")){;

                    pstmt.setString(1, BEtoAD(issueDateField.getText()));
                    pstmt.setString(2, firstNameField.getText());
                    pstmt.setString(3, lastNameField.getText());
                    pstmt.setString(4, phoneField.getText());
                    pstmt.setInt(5, Integer.parseInt(priceField.getText()));
                    pstmt.setInt(6, Integer.parseInt(durationField.getText()));
                    pstmt.setDouble(7, rate);  // <- update only if needed
                    pstmt.setString(8, BEtoAD(issueDateField.getText())); // for dueDate
                    pstmt.setInt(9, Integer.parseInt(durationField.getText())); // for dueDate interval
                    pstmt.setString(10, ticketNo);

                    pstmt.executeUpdate();

                    // Show success message
                    JLabel success = new JLabel("อัปเดตข้อมูลสำเร็จ");
                    success.setFont(plain);
                    JOptionPane.showMessageDialog(this, success);

                    // Reload the data to refresh the table
                    loadTicket();
                } catch (SQLException | IllegalArgumentException e) {
                    JLabel fail = new JLabel("อัปเดตข้อมูลไม่สำเร็จ<br>ข้อผิดพลาด: "+e.getMessage());
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
            // withdrawSomeObjects(ticketNo);
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
        
        if (_date == null || _date.length() < 10) { return "Invalid date";}
        String date = _date.substring(8, 10);
        String month = _date.substring(5, 7);
        String year = _date.substring(0, 4);
        int BE = Integer.parseInt(year) + 543;
        return date + "/" + month + "/" + Integer.toString(BE);
    }
    
    private String BEtoAD(String _date) {
        
        if (_date == null || _date.length() < 10) { return "Invalid date";}
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
            PreparedStatement pstmt = conn.prepareStatement("SELECT totalPrice, rate FROM ticket WHERE _No = ?")) { // Fixed column name
            pstmt.setString(1, ticketNo);
            ResultSet rs = pstmt.executeQuery();
            String today = BEtoAD(getToday());
            int datediff[] = getDateDiff(today);
            int price = 0;
            int total = 0;
            double rate = 0.0;

            if (rs.next()) {    
                price = rs.getInt("totalPrice");
                rate = rs.getDouble("rate");
            }

            total = computeInterest(price, rate, datediff);

            if(total != price){
                try (Connection conn1 = db_Connection.getConnection();
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ? WHERE _No = ?")) { // Fixed column name
                    pstmt1.setInt(1, total);
                    pstmt1.setString(2, today);
                    pstmt1.setString(3, "ไถ่ถอนแล้ว");
                    pstmt1.setString(4, ticketNo);
                    pstmt1.executeUpdate();

                } catch (SQLException e) {
                    // JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                    JLabel fail = new JLabel(e.getMessage());
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                }

                JLabel success = new JLabel("<html>ไถ่ถอนตั๋วหมายเลข "+ticketNo+" สำเร็จ" +
                "<br>ระยะเวลาจำนำ: "+ datediff[0] + " เดือน " + + datediff[1] + " วัน" +
                "<br>จำนวนเงินต้น: " +price+ " บาท" + 
                "<br>ดอกเบี้ย: "+(total-price)+" บาท" +
                "<br>รวมทั้งหมด "+total+" บาท</html>");
                success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                
                JOptionPane.showMessageDialog(this, success);

                loadTicket();
            }else{
                loadTicket();
            }
            
            } catch (Exception e) {
                JLabel fail = new JLabel("ไถ่ถอนไม่สำเร็จ ข้อผิดพลาด: "+e.getMessage());
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
                PreparedStatement pstmt = conn.prepareStatement("SELECT issueDate, totalPrice, rate FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                int[] datediff = new int[2];
                int pawnPrice = 0;
                double rate = 0.0;
                String today = getToday();
                String issueDate = "";

                if (rs.next()) {    
                    issueDate = rs.getDate("issueDate").toString();
                    pawnPrice = rs.getInt("totalPrice");
                    rate = rs.getDouble("rate");
                }

                datediff = getDateDiff(issueDate);
                if(datediff[1] > 3){
                    datediff[1] = datediff[1];
                }else{
                    datediff[1] = 0;
                    try (Connection conn0 = db_Connection.getConnection();
                        PreparedStatement pstmt0 = conn0.prepareStatement("SELECT ADDDATE(CURDATE(), INTERVAL ? DAY) as today")) { 
                        
                        pstmt0.setInt(1, -1*(datediff[1]));
                        ResultSet rs0 = pstmt0.executeQuery();

                        if(rs0.next()){today = rs.getString("today");}
                        today = ADtoBE(today);

                    } catch (SQLException e) {
                        JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง");
                        fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                        JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                    }


                }

                int totalPrice = computeInterest(pawnPrice, rate, datediff);

                
                if(totalPrice == pawnPrice){
                    loadTicket();
                }else{    
                    try (Connection conn1 = db_Connection.getConnection();
                    PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = ?")) { // Fixed column name
                        pstmt1.setInt(1, totalPrice);
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
                                    "INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, rate, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                                )) {
                                    
                                    pstmt3.setString(1, add0s(_No));
                                    pstmt3.setDate(2, Date.valueOf(row[0].toString()));
                                    pstmt3.setString(3, row[1].toString());
                                    pstmt3.setString(4, row[2].toString());
                                    pstmt3.setString(5, row[3].toString());
                                    pstmt3.setInt(6, (int) row[4]);
                                    pstmt3.setDouble(7, rate);
                                    pstmt3.setInt(8, (int) row[5]);
                                    pstmt3.setDate(9, Date.valueOf(row[6].toString()));
                                    pstmt3.setString(10, "อยู่ระหว่างจำนำ");
                                    pstmt3.setString(11, ticketNo);

                                    pstmt3.executeUpdate();
                            }
                        }

                    } catch (SQLException e) {
                        JLabel fail = new JLabel(e.getMessage());
                        fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                        JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    copyObjs(ticketNo, add0s(_No));

                    JLabel success = new JLabel("<html>ต่ออายุตั๋วหมายเลข "+ticketNo+" สำเร็จ"+
                    "<br>ระยะเวลาจำนำ: "+ datediff[0] + " เดือน " + + datediff[1] + " วัน" +
                    "<br>จำนวนเงินต้น: "+pawnPrice+" บาท"+
                    "<br>ดอกเบี้ย: "+(totalPrice-pawnPrice)+" บาท"+
                    "<br>รวมทั้งหมด "+totalPrice+" บาท</html>");
                    success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                    
                    JOptionPane.showMessageDialog(this, success);

                    loadTicket();
                }
            } catch (SQLException e) {
                JLabel fail = new JLabel("ต่ออายุไม่สำเร็จ<br>ข้อผิดพลาด"+e.getMessage());
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
        
    private void gainPrincipal(String ticketNo){
        JLabel reqLabel = new JLabel("จำนวนเงินที่ขอเพิ่ม:");
        JLabel rateLabel = new JLabel("อัตราดอกเบี้ย (%):");
        JLabel durLabel = new JLabel("ระยะเวลา:");

        reqLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        rateLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        durLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));

        JTextField reqField = new JTextField();
        JTextField rateField = new JTextField();
        JTextField durField = new JTextField();

        reqField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        rateField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        durField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));

        int req = 0, dur = 0; String newRate = "";

        Object message[] = {reqLabel, reqField, rateLabel, rateField, durLabel, durField};
        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มเงินต้น", JOptionPane.YES_NO_OPTION);

        
        if (option == JOptionPane.YES_OPTION) {
            req = Integer.parseInt(reqField.getText());
            dur = Integer.parseInt(durField.getText());
            newRate = rateField.getText();

            int _No = getLatestNo();

            try (Connection conn = db_Connection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT issueDate, totalPrice, rate FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                String issueDate = "";
                int pawnPrice = 0;
                double rate = 0.0;
                String today = getToday();

                if (rs.next()) {    
                    issueDate = rs.getString("issueDate");
                    pawnPrice = rs.getInt("totalPrice");
                    rate = rs.getDouble("rate");
                }

                int datediff[] = getDateDiff(issueDate);
                int totalPrice = computeInterest(pawnPrice, rate, datediff);

                if (newRate.isEmpty()) {
                    if (pawnPrice+req >= 10000) {
                        rate = 1.25;
                    } else if (pawnPrice+req > 2500) {
                        rate = 1.5;
                    } else {
                        rate = 2.0;
                    }
                } else {
                    rate = Double.parseDouble(rateField.getText());
                }

                if(totalPrice == pawnPrice){
                    loadTicket();
                }else{
                    
                    try (Connection conn1 = db_Connection.getConnection();
                        PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = ?")) { // Fixed column name
                        pstmt1.setInt(1, totalPrice);
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
                        PreparedStatement pstmt2 = conn.prepareStatement("SELECT firstName, lastName, phoneNumber FROM ticket WHERE _No = ?")) {
                            pstmt2.setString(1, ticketNo);
                        ResultSet rs1 = pstmt2.executeQuery();

                        if(rs1.next()){
                            Object[] row = {
                                rs1.getString("firstName"),
                                rs1.getString("lastName"),
                                rs1.getString("phoneNumber"),
                            };
                            try (Connection conn3 = db_Connection.getConnection();
                                PreparedStatement pstmt3 = conn.prepareStatement("INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, rate, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ADDDATE(CURDATE(), INTERVAL duration MONTH), ?, ?)")) {
                                    
                                pstmt3.setString(1, add0s(_No));
                                pstmt3.setString(2, BEtoAD(getToday()));
                                pstmt3.setString(3, row[0].toString());
                                pstmt3.setString(4, row[1].toString());
                                pstmt3.setString(5, row[2].toString());
                                pstmt3.setInt(6, pawnPrice+req);
                                pstmt3.setDouble(7, rate);
                                pstmt3.setInt(8, dur);
                                pstmt3.setString(9, "อยู่ระหว่างจำนำ");
                                pstmt3.setString(10, ticketNo);

                                pstmt3.executeUpdate();
                            }
                        }

                    } catch (SQLException e) {
                        JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง<br>ข้อผิดพลาด: "+e.getMessage());
                        fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                        JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    copyObjs(ticketNo, add0s(_No));

                    
                    JLabel success = new JLabel("<html>เพิ่มเงินตั๋วหมายเลข " + ticketNo + "<br>" +
                        "จำนวน " + req + " สำเร็จ<br>" +
                        "ระยะเวลาจำนำ: "+ datediff[0] + " เดือน " + + datediff[1] + " วัน<br>" +
                        "จำนวนเงินต้นหลังเพิ่ม: " + (pawnPrice + req) + " บาท<br>" +
                        "จำนวนเงินต้นที่เพิ่มหลังหักดอกเบี้ย: " + ((req) - (totalPrice - pawnPrice)) + " บาท<br>" +
                        "ดอกเบี้ย: " + ((totalPrice - pawnPrice)) + " บาท<br>" +
                        "รวมทั้งหมด " + totalPrice + " บาท</html>");

                    success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                    JOptionPane.showMessageDialog(this, success);

                    loadTicket();
                }
               
            } catch (SQLException e) {
                JLabel fail = new JLabel("เพิ่มเงินต้นไม่สำเร็จ<br>ข้อผิดพลาด: "+e.getMessage());
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void payBackPrincipal(String ticketNo){
        int principal = 0;
        String issueDate = "";
        try (Connection conn = db_Connection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT totalPrice, issueDate FROM ticket WHERE _No = "+ticketNo)) {

            if (rs.next()) {
                principal = rs.getInt("totalPrice");
                issueDate = rs.getString("issueDate");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        JLabel reqLabel = new JLabel("จำนวนเงินที่จ่ายคืน:");
        JLabel rateLabel = new JLabel("อัตราดอกเบี้ย (%):");
        JLabel durLabel = new JLabel("ระยะเวลา:");

        reqLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        rateLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
        durLabel.setFont(new Font("TH Sarabun New", Font.BOLD, 18));

        JTextField reqField = new JTextField();
        JTextField rateField = new JTextField();
        JTextField durField = new JTextField();

        reqField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        rateField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
        durField.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));

        int req = 0,dur = 0; String newRate = "";
        int option;
        
        do{
            Object message[] = {reqLabel, reqField, rateLabel, rateField, durLabel, durField};
            option = JOptionPane.showConfirmDialog(this, message, "จ่ายคืนเงินต้น", JOptionPane.YES_NO_OPTION);
        }while((Integer.parseInt(reqField.getText())>=principal)&&(option == JOptionPane.YES_OPTION));
        
        if (option == JOptionPane.YES_OPTION) {
            req = Integer.parseInt(reqField.getText());
            dur = Integer.parseInt(durField.getText());
            newRate = rateField.getText();

            int _No = getLatestNo();

            try (Connection conn = db_Connection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT totalPrice, rate FROM ticket WHERE _No = ?")) { // Fixed column name
                pstmt.setString(1, ticketNo);
                ResultSet rs = pstmt.executeQuery();
                
                int pawnPrice = 0;
                double rate = 0.0;
                String today = getToday();

                if (rs.next()) {    
                    pawnPrice = rs.getInt("totalPrice");
                    rate = rs.getDouble("rate");
                }

                int datediff[] = getDateDiff(issueDate);
                int totalPrice = computeInterest(pawnPrice, rate, datediff);

                if(totalPrice == pawnPrice){
                    loadTicket();
                }else{
                    if (newRate.isEmpty()) {
                
                        if (pawnPrice-req >= 10000) {
                            rate = 1.25;
                        } else if (pawnPrice-req > 2500) {
                            rate = 1.5;
                        } else {
                            rate = 2.0;
                        }
                    } else {
                        rate = Double.parseDouble(rateField.getText());
                    }

                    try (Connection conn1 = db_Connection.getConnection();
                    PreparedStatement pstmt1 = conn.prepareStatement("UPDATE ticket SET redemptPrice = ?, redemptDate = ?, status = ?, new_ticket_No = ? WHERE _No = "+ticketNo)) { // Fixed column name
                        pstmt1.setInt(1, totalPrice);
                        pstmt1.setString(2, BEtoAD(today));
                        pstmt1.setString(3, "ลดเงินต้นจำนวน "+req+" บาทแล้ว");
                        pstmt1.setString(4, add0s(_No));
                        pstmt1.executeUpdate();

                    } catch (SQLException e) {
                        JLabel fail = new JLabel("ไม่พบข้อมูลตั๋ว หรือข้อมูลไม่ถูกต้อง<br>ข้อผิดพลาด: "+e.getMessage());
                        fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                        JOptionPane.showMessageDialog(this, fail, "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    try (Connection conn2 = db_Connection.getConnection();
                        PreparedStatement pstmt2 = conn.prepareStatement("SELECT firstName, lastName, phoneNumber FROM ticket WHERE _No = ?")) {
                            pstmt2.setString(1, ticketNo);
                        ResultSet rs1 = pstmt2.executeQuery();

                        if(rs1.next()){
                            Object[] row = {
                                rs1.getString("firstName"),
                                rs1.getString("lastName"),
                                rs1.getString("phoneNumber"),
                            };
                            try (Connection conn3 = db_Connection.getConnection();
                                PreparedStatement pstmt3 = conn.prepareStatement("INSERT INTO ticket(_No, issueDate, firstName, lastName, phoneNumber, totalPrice, rate, duration, dueDate, status, old_ticket_No) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ADDDATE(CURDATE(), INTERVAL "+dur+" MONTH), ?, ?)")) {
                                    
                                    pstmt3.setString(1, add0s(_No));
                                    pstmt3.setString(2, BEtoAD(today));
                                    pstmt3.setString(3, row[0].toString());
                                    pstmt3.setString(4, row[1].toString());
                                    pstmt3.setString(5, row[2].toString());
                                    pstmt3.setInt(6, pawnPrice-req);
                                    pstmt3.setDouble(7, rate);
                                    pstmt3.setInt(8, dur);
                                    pstmt3.setString(9, "อยู่ระหว่างจำนำ");
                                    pstmt3.setString(10, ticketNo);

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
                        "จำนวน " + req + " สำเร็จ" +
                        "<br>ระยะเวลาจำนำ: "+ datediff[0] + " เดือน " + + datediff[1] + " วัน<br>" +
                        "จำนวนเงินต้นหลังลด: " + (pawnPrice - req) + " บาท<br>" +
                        "รับเงินทั้งหมด: " + (req + (totalPrice - pawnPrice)) + " บาท<br>" +
                        "ดอกเบี้ย: " + (totalPrice - pawnPrice) + " บาท<br>" +
                        "รวมทั้งหมด " + totalPrice + " บาท</html>");

                    success.setFont(new Font("TH Sarabun New", Font.BOLD, 18));
                    JOptionPane.showMessageDialog(this, success);

                    loadTicket();
                }
            } catch (SQLException e) {
                JLabel fail = new JLabel("เพิ่มเงินต้นไม่สำเร็จ<br>ข้อผิดพลาด: "+e.getMessage());
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

    private int[] getDateDiff(String issueDate) {
    int[] datediff = new int[2]; // [0] = months, [1] = days

    String sql = 
        "SELECT " +
        "  TIMESTAMPDIFF(MONTH, ?, CURDATE()) AS months, " +
        "  DATEDIFF(CURDATE(), DATE_ADD(?, INTERVAL TIMESTAMPDIFF(MONTH, ?, CURDATE()) MONTH)) AS days";

        try (Connection conn = db_Connection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set ? placeholders with dueDate
            stmt.setString(1, issueDate);
            stmt.setString(2, issueDate);
            stmt.setString(3, issueDate);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    datediff[0] = rs.getInt("months");
                    datediff[1] = rs.getInt("days");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return datediff;
    }


    private int computeInterest(int principal, double _rate, int[] _datediff) {
        double rate = _rate / 100;
        int months = _datediff[0];
        int days = _datediff[1];
        int interest = 0;

        // เฉพาะอัตราดอกเบี้ยที่กำหนดเท่านั้นที่คำนวณอัตโนมัติ
        if (rate == 0.02 || rate == 0.015 || rate == 0.0125) {

            // จำนำและไถ่คืนภายใน 3 วัน
            if (months == 0 && days <= 3) {
                if (principal >= 5000 && principal < 11000) {
                    interest = roundUp((int)(principal * 0.01));
                } else if (principal >= 11000 && principal <= 19000) {
                    interest = roundUp((int)(principal * 0.0075));
                } else if(principal > 19000){
                    interest = roundUp((int)(principal * 0.0063));
                } else {
                    interest = roundUp((int)(principal * rate));
                }
                
            }
            // ไถ่คืนภายใน 10 วัน
            else if (months == 0 && (days> 3 &&days <= 10)) {
                interest = roundUp((int)(principal * 0.01));
            }
            // ปกติ: คิดตามจำนวนเดือนและมีเศษวัน = +1 เดือน
            else {
                interest = roundUp((int)(principal * rate)) * months;
                if (days > 0) {
                    interest += roundUp((int)(principal * rate));
                }
            }

            return principal + interest;

        } else {
            // ไม่ใช่ 0.02, 0.015, 0.0125 -> ขอให้ผู้ใช้กรอกยอดรวมเอง
            JTextField input = new JTextField();
            JLabel label = new JLabel("กรุณากรอกยอดรวมเงินต้นและดอกเบี้ย (บาท):");
            label.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            input.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));

            Object[] message = { label, input };
            int option = JOptionPane.showConfirmDialog(null, message, "กรอกยอดรวม", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                try {
                    int grandTotal = Integer.parseInt(input.getText().trim());
                    if (grandTotal < principal) {
                        throw new NumberFormatException();
                    }
                    return grandTotal;
                } catch (NumberFormatException e) {
                    JLabel fail = new JLabel("กรุณากรอกตัวเลขที่ถูกต้องและมากกว่าหรือเท่ากับเงินต้น (" + principal + ")");
                    fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                    JOptionPane.showMessageDialog(null, fail, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
                    return computeInterest(principal, _rate, _datediff); // try again
                }
            } else {
                return principal; // If user cancels, return principal only
            }
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

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class OverviewPage extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel summary = new JLabel();

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

        // Create "Add Ticket" button
        JButton addButton = new JButton("เพิ่มตั๋วใหม่");
        addButton.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
        addButton.setPreferredSize(new Dimension(150, 40));
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addButton.addActionListener(e -> addTicket());

        // Load summary data
        loadSummary();
        summary.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
        summary.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components with spacing
        topPanel.add(addButton);
        topPanel.add(Box.createVerticalStrut(10)); // Space between button and summary
        topPanel.add(summary);

        // Fetch and display summary details
        loadSummary();

        Object[] columns = new String[]{
            "เลขที่", "วันที่ออกตั๋ว", "ชื่อจริง", "นามสกุล", "เบอร์โทรศัพท์", "ราคา", "ระยะเวลา", "วันครบกำหนด", "เลขที่บิลเก่า", "เลขที่บิลใหม่", "", "", ""
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

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                String ticketNo = table.getValueAt(row, 0).toString();

                if (column == 10) {
                    editTicket(row);
                } else if (column == 11) {
                    // Action for another button
                } else if (column == 12) {
                    viewTicket(ticketNo);
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

    private void loadSummary() {
        try (Connection conn = db_Connection.getConnection();
             PreparedStatement stmtAll = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket");
             PreparedStatement stmtToday = conn.prepareStatement("SELECT SUM(totalPrice) AS net_totalPrice, COUNT(_No) AS totalQuantity FROM ticket WHERE issueDate = CURDATE()")) {

            int netTotalPriceAllDays = 0, totalQuantityAllDays = 0;
            int netTotalPriceToday = 0, totalQuantityToday = 0;

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

            // Display the summary
            this.summary.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
            this.summary.setText("ยอดสุทธิ: " + netTotalPriceAllDays + " จำนวนห่อทั้งหมด: " + totalQuantityAllDays +
                    " ยอดสุทธิวันนี้: " + netTotalPriceToday + " จำนวนห่อวันนี้: " + totalQuantityToday);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "โหลดข้อมูลไม่สำเร็จ", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTicket() {
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
                    rs.getInt("duration"),
                    ADtoBE(rs.getDate("dueDate").toString()),
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

            if (rs.next()) ticketNo = rs.getInt("cnt");

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
                     "INSERT INTO ticket VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
    
                // Validate input values
                pstmt.setString(1, add0s(ticketNo));
                pstmt.setDate(2, Date.valueOf(BEtoAD(issueDateField.getText()))); // Ensure valid date format
                pstmt.setString(3, firstNameField.getText());
                pstmt.setString(4, lastNameField.getText());
                pstmt.setString(5, phoneField.getText());
                pstmt.setInt(6, Integer.parseInt(priceField.getText())); // Ensure valid integer
                pstmt.setInt(7, Integer.parseInt(durationField.getText())); // Ensure valid integer
                pstmt.setDate(8, Date.valueOf(BEtoAD(dueDateField.getText()))); // Ensure valid date format
                pstmt.setString(9, old_ticket_NoField.getText());
                pstmt.setString(10, new_ticket_NoField.getText());
    
                // Execute the insert operation
                pstmt.executeUpdate();
                JLabel success = new JLabel("เพิ่มตั๋วสำเร็จ");
                success.setFont(plain);
                // Show success message
                JOptionPane.showMessageDialog(this, success, "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);
    
                // Refresh the table
                loadTicket();
    
            } catch (SQLException | IllegalArgumentException e) {
                // Show error message if an exception occurs
                JOptionPane.showMessageDialog(this, "เพิ่มตั๋วไม่สำเร็จ", "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
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
        int duration = (int) tableModel.getValueAt(row, 6);
        String dueDate = tableModel.getValueAt(row, 7).toString();
        String old_ticket_No = "";
        String new_ticket_No = "";
        if(tableModel.getValueAt(row, 8).toString() != null){
            old_ticket_No = tableModel.getValueAt(row, 8).toString();    
        }
        if(tableModel.getValueAt(row, 9).toString() != null){
            new_ticket_No = tableModel.getValueAt(row, 9).toString();
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
                    JLabel fail = new JLabel("อัปเดตข้อมูลไม่สำเร็จ");
                    fail.setFont(plain);
                    JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void viewTicket(String ticketNo) {
        SwingUtilities.invokeLater(() -> new TicketDetailPage(ticketNo));
    }    

    private void actionPanel(){
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "การดำเนินการตั๋ว", true);
        dialog.setSize(280, 300);  // Adjust the size of the popup
        dialog.setLayout(new FlowLayout()); // Use FlowLayout for better control
        dialog.setLocationRelativeTo(this);

        // Button size
        Dimension buttonSize = new Dimension(240, 35); // Width x Height

        // Create buttons
        JButton withdrawAllBtn = new JButton("ถอนออกทั้งหมด");
        JButton renewTicketBtn = new JButton("ต่ออายุ");
        JButton addMoreBtn = new JButton("เพิ่มของเข้าไป");
        JButton payBackBtn = new JButton("จ่ายคืนต้น");
        JButton withdrawSomeBtn = new JButton("ถอนออกบางส่วน");

        // Set button font
        Font buttonFont = new Font("TH Sarabun New", Font.BOLD, 16);
        withdrawAllBtn.setFont(buttonFont);
        renewTicketBtn.setFont(buttonFont);
        addMoreBtn.setFont(buttonFont);
        payBackBtn.setFont(buttonFont);
        withdrawSomeBtn.setFont(buttonFont);

        // Set button size
        withdrawAllBtn.setPreferredSize(buttonSize);
        renewTicketBtn.setPreferredSize(buttonSize);
        addMoreBtn.setPreferredSize(buttonSize);
        payBackBtn.setPreferredSize(buttonSize);
        withdrawSomeBtn.setPreferredSize(buttonSize);

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
            addMoreObjects(ticketNo);
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
}

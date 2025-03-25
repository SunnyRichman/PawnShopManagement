import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class TicketDetailPage extends JFrame {
    
    private String ticketNo;
    private String status;
    private DefaultTableModel tableModel;
    private JTable table;

    public TicketDetailPage(String no, String _status) {
        this.ticketNo = no;
        this.status = _status;
        this.initialize();
    }

    private void initialize() {

        setTitle("Ticket Details");
        setSize(800, 600);
        setLayout(new GridLayout(1, 2));
        // Left side panel to show ticket details

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
    
        // Retrieve ticket details from the database using ticketNo
        try (Connection conn = db_Connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM ticket WHERE _No = ?")) { // Fixed column name
            pstmt.setString(1, this.ticketNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Font plain = new Font("TH Sarabun New", Font.PLAIN, 20);
                Font bold = new Font("TH Sarabun New", Font.BOLD, 20);
    
                String issueDate = rs.getDate("issueDate").toString();
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                String phoneNumber = rs.getString("phoneNumber");
                int totalPrice = rs.getInt("totalPrice");
                int duration = rs.getInt("duration");
                String dueDate = rs.getDate("dueDate").toString();
                String redemptDate = rs.getDate("dueDate") != null ? rs.getDate("redemptDate").toString() : "-";
                String old_ticket_No = rs.getString("old_ticket_No");
                String new_ticket_No = rs.getString("new_ticket_No");
    
                JLabel[] labels = {
                    new JLabel("เลขที่ตั๋ว: "), new JLabel(this.ticketNo),
                    new JLabel("วันที่ออกตั๋ว: "), new JLabel(ADtoBE(issueDate)),
                    new JLabel("ชื่อ: "), new JLabel(firstName),
                    new JLabel("นามสกุล: "), new JLabel(lastName),
                    new JLabel("เบอร์โทรศัพท์: "), new JLabel(phoneNumber),
                    new JLabel("ราคารวม: "), new JLabel(Integer.toString(totalPrice)),
                    new JLabel("ระยะเวลา: "), new JLabel(Integer.toString(duration)),
                    new JLabel("วันครบกำหนด: "), new JLabel(ADtoBE(dueDate)),
                    new JLabel("วันไถ่ถอน: "), new JLabel(ADtoBE(redemptDate)),
                    new JLabel("สถานะ: "), new JLabel(this.status),
                    new JLabel("เลขที่บิลเก่า: "), new JLabel(old_ticket_No),
                    new JLabel("เลขที่บิลใหม่: "), new JLabel(new_ticket_No)
                };
    
                for (int i = 0; i < labels.length; i++) {
                    labels[i].setFont((i % 2 == 0) ? bold : plain);
                }
    
                // Add ticket details to the left panel
                for (int i = 0; i < labels.length; i += 2) {
                    JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    rowPanel.add(labels[i]);
                    rowPanel.add(labels[i + 1]);
                    leftPanel.add(rowPanel);
                }
            }
        } catch (SQLException e) {
            JLabel jLabel = new JLabel("โหลดข้อมูลไม่สำเร็จ");
            jLabel.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            JOptionPane.showMessageDialog(this, jLabel, "Error", JOptionPane.ERROR_MESSAGE);
        }
    
        // Right side panel to show the table
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 20));
    
        // Assign tableModel to a class-level variable
        this.tableModel = new DefaultTableModel(new Object[]{"ชิ้นที่", "จำนวน", "สินค้า", "น้ำหนัก", "ราคา", ""}, 0);
        this.table = new JTable(this.tableModel); // Use this.tableModel
        this.table.setRowHeight(25);
        this.table.setFont(new Font("TH Sarabun New", Font.PLAIN, 20));
        this.table.getTableHeader().setFont(new Font("TH Sarabun New", Font.BOLD, 20));

        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());

                if (column == 5  && status.equals("อยู่ระหว่างจำนำ")) editObject(row);
            }
        });
    
        // Add a button to insert a new object
        JButton insertButton = new JButton("เพิ่มสินค้า");
        insertButton.setFont(new Font("TH Sarabun New", Font.BOLD, 20));
        insertButton.setPreferredSize(new Dimension(40, 40));
        insertButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        insertButton.addActionListener(e -> addObject());
    
        // Add components to the right panel
        rightPanel.add(insertButton, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(this.table), BorderLayout.CENTER);
    
        // Load table data
        loadObjects(ticketNo);
    
        // Add panels to the frame
        add(leftPanel);
        add(rightPanel);
    
        setVisible(true);
    }
    
    // Function to load object data into the table
    private void loadObjects(String ticketNo) {
        try (Connection connection = db_Connection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM objects WHERE ticketNo = ?")) {
            preparedStatement.setString(1, ticketNo);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            this.tableModel.setRowCount(0); // Clear table before loading new data
            
            while (resultSet.next()) {
                Object[] object = new Object[]{
                    resultSet.getString("obj_id"), 
                    resultSet.getInt("amount"), 
                    resultSet.getString("object"), 
                    resultSet.getDouble("weight"), 
                    resultSet.getInt("price"), 
                    "แก้ไข"
                };
                this.tableModel.addRow(object);
            }
        } catch (SQLException e) {
            JLabel jLabel = new JLabel("โหลดข้อมูลไม่สำเร็จ");
            jLabel.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
            JOptionPane.showMessageDialog(this, jLabel, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    private void addObject() {
        JTextField amountField = new JTextField();
        JTextField objectField = new JTextField();
        JTextField weightField = new JTextField();
        JTextField priceField = new JTextField();
        

        Font plain = new Font("TH Sarabun New", Font.PLAIN, 18);
        amountField.setFont(plain);
        objectField.setFont(plain);
        weightField.setFont(plain);
        priceField.setFont(plain);
           
        // Create labels
        JLabel amountLabel = new JLabel("จำนวน:");
        JLabel objectLabel = new JLabel("สินค้า:");
        JLabel weightLabel = new JLabel("น้ำหนัก:");
        JLabel priceLabel = new JLabel("ราคา:");
    
        // Set font for labels
        Font bold = new Font("TH Sarabun New", Font.BOLD, 18);
        amountLabel.setFont(bold);
        objectLabel.setFont(bold);
        weightLabel.setFont(bold);
        priceLabel.setFont(bold);
    
        // Create form layout
        Object[] message = {
            amountLabel, amountField,
            objectLabel, objectField,
            weightLabel, weightField,
            priceLabel, priceField
        };
    
        // Show the form
        int option = JOptionPane.showConfirmDialog(this, message, "เพิ่มตั๋วใหม่", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            int objectCount = 0; // Variable to store object count

            try (Connection conn = db_Connection.getConnection();
                 PreparedStatement getObj_id = conn.prepareStatement("SELECT COUNT(obj_id) AS cnt FROM objects WHERE ticketNo = ?");
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO objects VALUES (?, ?, ?, ?, ?, ?)")) {
            
                // Set ticketNo in the query
                getObj_id.setString(1, this.ticketNo);
                
                // Execute query
                try (ResultSet resultSet = getObj_id.executeQuery()) {
                    if (resultSet.next()) {
                        objectCount = resultSet.getInt("cnt"); // Retrieve count
                    }
                }
            
                // Set values for INSERT
                pstmt.setString(1, this.ticketNo);
                pstmt.setInt(2, objectCount + 1); // Assuming obj_id should be incremented
                pstmt.setInt(3, Integer.parseInt(amountField.getText()));
                pstmt.setString(4, objectField.getText());
                pstmt.setDouble(5, Double.parseDouble(weightField.getText()));
                pstmt.setInt(6, Integer.parseInt(priceField.getText()));
            
                // Execute insert
                pstmt.executeUpdate();
            
                // Show success message
                JLabel success = new JLabel("เพิ่มสินค้าสำเร็จ");
                success.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, success, "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);
    
                // Refresh the table
                loadObjects(ticketNo);
    
            } catch (SQLException | IllegalArgumentException e) {
                // Show error message if an exception occurs
                e.printStackTrace();
                JLabel fail = new JLabel("เพิ่มสินค้าไม่สำเร็จ");
                fail.setFont(new Font("TH Sarabun New", Font.PLAIN, 18));
                JOptionPane.showMessageDialog(this, fail, "ไม่สำเร็จ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editObject(int row) {
        String ticketNo = this.ticketNo;
        int no = Integer.parseInt(this.tableModel.getValueAt(row, 0).toString());
        int amount = Integer.parseInt(this.tableModel.getValueAt(row, 1).toString());
        String object = this.tableModel.getValueAt(row, 2).toString();
        double weight = Double.parseDouble(this.tableModel.getValueAt(row, 3).toString());
        int price = Integer.parseInt(this.tableModel.getValueAt(row, 4).toString());

        JTextField amtField = new JTextField(Integer.toString(amount));
        JTextField objField = new JTextField(object);
        JTextField wgtField = new JTextField(Double.toString(weight));
        JTextField priceField = new JTextField(Integer.toString(price));
        
        Font plain = new Font("TH Sarabun New", Font.PLAIN, 18);
        amtField.setFont(plain);
        objField.setFont(plain);
        wgtField.setFont(plain);
        priceField.setFont(plain);

        JLabel amtLabel = new JLabel("จำนวน:");
        JLabel objLabel = new JLabel("สินค้า:");
        JLabel wgtLabel = new JLabel("น้ำหนัก:");
        JLabel priceLabel = new JLabel("ราคา:");

        Font bold = new Font("TH Sarabun New", Font.BOLD, 18);
        amtLabel.setFont(bold);
        objLabel.setFont(bold);
        wgtLabel.setFont(bold);
        priceLabel.setFont(bold);
        Object[] form = new Object[]{
            amtLabel, amtField, 
            objLabel, objField, 
            wgtLabel, wgtField, 
            priceLabel, priceField};
        int n5 = JOptionPane.showConfirmDialog(this, form, "แก้ไขข้อมูล", 2);
        if (n5 == 0) {
            JLabel isConfirm = new JLabel("คุณต้องการแก้ไขสินค้าชิ้นที่ "+no+" หริอไม่");
            isConfirm.setFont(plain);
            int n6 = JOptionPane.showConfirmDialog(this, isConfirm, "แก้ไขข้อมูล", 0);
            if (n6 == 0) {
                try (Connection connection = db_Connection.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement("UPDATE objects SET amount = ?, object = ?, weight = ?, price = ? WHERE ticketNo = ? AND obj_id = ?");){
                    preparedStatement.setInt(1, Integer.parseInt(amtField.getText()));
                    preparedStatement.setString(2, objField.getText());
                    preparedStatement.setDouble(3, Double.parseDouble(wgtField.getText()));
                    preparedStatement.setInt(4, Integer.parseInt(priceField.getText()));
                    preparedStatement.setString(5, ticketNo);
                    preparedStatement.setInt(6, no);
                    preparedStatement.executeUpdate();
                    JLabel success = new JLabel("แก้ไขสินค้าหมายเลข "+no+" สำเร็จ");
                    success.setFont(plain);
                    JOptionPane.showMessageDialog(this, success);
                    this.loadObjects(this.ticketNo);
                }
                catch (NumberFormatException | SQLException exception) {
                    exception.printStackTrace();
                    JLabel fail = new JLabel("แก้ไขข้อมูลไม่สำเร็จ");
                    fail.setFont(plain);
                    JOptionPane.showMessageDialog(this, fail, "เกิดข้อผิดพลาด", 0);
                }
            }
        }
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

}


import db.SessionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;





/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author bihas
 */

public class PendingApprovalForm extends javax.swing.JFrame {

    public PendingApprovalForm() {
        initComponents();

        if (!SessionManager.isAdminLoggedIn) {
            JOptionPane.showMessageDialog(null, "Access denied. Please log in first.");
            new RoleSelectionForm().setVisible(true);
            dispose();
            return;
        }

        setLocationRelativeTo(null);
        MainDesktop.setLayout(new CardLayout());
        MainDesktop.add(AccountApprovalPanel, "AccountApprovalPanel");
        MainDesktop.add(PasswordResetPanel, "PasswordResetPanel");

        setupNavigation();
        setupSearchActions();
        loadPendingAccounts();
        loadPasswordResetRequests();
        styleTableHeader();
        stylePasswordResetTableHeader();
    }

    private void setupNavigation() {
        final JPanel[] lastClicked = {null};

        AccountApproval.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                AccountApproval.setBackground(Color.BLACK);
                switchPanel("AccountApprovalPanel", lastClicked, AccountApproval);
                loadPendingAccounts();
            }
        });

        PasswordReset.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                PasswordReset.setBackground(Color.BLACK);
                switchPanel("PasswordResetPanel", lastClicked, PasswordReset);
                loadPasswordResetRequests();
            }
        });
    }

    private void switchPanel(String panelName, JPanel[] lastClicked, JPanel current) {
        if (lastClicked[0] != null && lastClicked[0] != current) {
            lastClicked[0].setBackground(new Color(51, 51, 51));
        }
        lastClicked[0] = current;
        ((CardLayout) MainDesktop.getLayout()).show(MainDesktop, panelName);
    }

    private void setupSearchActions() {
        btnSearch.addActionListener(e -> searchPasswordResetRequests());
        btnSearch1.addActionListener(e -> searchPendingAccounts());
    }

    private void searchPasswordResetRequests() {
    String keyword = txtSearch.getText().trim();
    DefaultTableModel model = (DefaultTableModel) tblPasswordResetRequests.getModel();
    model.setRowCount(0);

    try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "")) {
        String query = "SELECT pr.id, pr.student_id, u.full_name, pr.new_password, pr.reason, pr.status " +
                       "FROM password_resets pr " +
                       "JOIN users u ON pr.student_id = u.student_id " +
                       "WHERE pr.status = 'pending'";

        if (!keyword.isEmpty()) {
            query += " AND (CAST(pr.id AS CHAR) LIKE ? OR pr.student_id LIKE ? OR u.full_name LIKE ? OR pr.new_password LIKE ? OR pr.reason LIKE ?)";
        }

        PreparedStatement stmt = con.prepareStatement(query);
        if (!keyword.isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                stmt.setString(i, "%" + keyword + "%");
            }
        }

        ResultSet rs = stmt.executeQuery();
        if (!rs.isBeforeFirst()) {
            JOptionPane.showMessageDialog(null, "No matching *pending* request found.");
            loadPasswordResetRequests();
            return;
        }

        while (rs.next()) {
            model.addRow(new Object[]{
                    rs.getString("id"),
                    rs.getString("student_id"),
                    rs.getString("full_name"),          
                    rs.getString("new_password"),
                    rs.getString("reason"),
                    rs.getString("status")
            });
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Search error: " + ex.getMessage());
    }
}


    private void searchPendingAccounts() {
        String keyword = txtSearch1.getText().trim();
        DefaultTableModel model = (DefaultTableModel) tblPendingAccounts.getModel();
        model.setRowCount(0);

        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "")) {
            String sql = "SELECT * FROM users WHERE status = 'pending'";
            if (!keyword.isEmpty()) {
                sql += " AND (CAST(id AS CHAR) LIKE ? OR student_id LIKE ? OR full_name LIKE ? OR course LIKE ?)";
            }

            PreparedStatement ps = con.prepareStatement(sql);
            if (!keyword.isEmpty()) {
                for (int i = 1; i <= 4; i++) ps.setString(i, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            if (!rs.isBeforeFirst()) {
                JOptionPane.showMessageDialog(null, "No pending accounts matched your search.");
                loadPendingAccounts();
                return;
            }

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("student_id"),
                        rs.getString("full_name"),
                        rs.getString("course")
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Search error: " + ex.getMessage());
        }
    }

    private void loadPendingAccounts() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/event_system", "root", "")) {
            String sql = "SELECT id, student_id, full_name, course FROM users WHERE status = 'pending'";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            DefaultTableModel model = (DefaultTableModel) tblPendingAccounts.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("student_id"),
                        rs.getString("full_name"),
                        rs.getString("course")
                });
            }

            tblPendingAccounts.addMouseListener(new RowSelector(tblPendingAccounts));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

   public void loadPasswordResetRequests() {
    try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "")) {
        String query = "SELECT pr.id, pr.student_id, u.full_name, pr.new_password, pr.reason, pr.status " +
                       "FROM password_resets pr " +
                       "JOIN users u ON pr.student_id = u.student_id " +
                       "WHERE pr.status = 'pending'";
        PreparedStatement pst = con.prepareStatement(query);
        ResultSet rs = pst.executeQuery();

        DefaultTableModel model = (DefaultTableModel) tblPasswordResetRequests.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_id"),
                    rs.getString("full_name"),
                    rs.getString("new_password"),
                    rs.getString("reason"),
                    rs.getString("status")
            });
        }

        tblPasswordResetRequests.addMouseListener(new RowSelector(tblPasswordResetRequests));
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error loading reset requests: " + e.getMessage());
    }
}


    private void approveStudent(String id) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/event_system", "root", "")) {
            String sql = "UPDATE users SET status = 'approved' WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Student Approved Successfully!");
            loadPendingAccounts();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void rejectStudent(String studentId) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/event_system", "root", "")) {
            String sql = "UPDATE users SET status = 'rejected', rejection_note = 'Your account was rejected. Please contact the admin for further details.' WHERE id = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, studentId);
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Student has been successfully rejected.");
                loadPendingAccounts();
            } else {
                JOptionPane.showMessageDialog(null, "Error updating student status.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
        }
    }

    private void styleTableHeader() {
        applyTableStyle(tblPendingAccounts);
    }

    private void stylePasswordResetTableHeader() {
        applyTableStyle(tblPasswordResetRequests);
    }

    private void applyTableStyle(JTable table) {
        table.getTableHeader().setDefaultRenderer(new HeaderColor());
        table.setRowHeight(45);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 50));
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(32, 136, 203));
        header.setForeground(Color.WHITE);
        TableCellRenderer leftRenderer = new CellRendererLeftAlign();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
        }
    }

    class HeaderColor extends DefaultTableCellRenderer {
        public HeaderColor() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBackground(new Color(255,51,51));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            return this;
        }
    }

    class CellRendererLeftAlign extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.LEFT);
            return c;
        }
    }

    class RowSelector extends MouseAdapter {
        private final JTable table;
        private int lastClickedRow = -1;

        public RowSelector(JTable table) {
            this.table = table;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int clickedRow = table.rowAtPoint(e.getPoint());
            if (clickedRow == -1) return;

            if (clickedRow == lastClickedRow) {
                table.clearSelection();
                lastClickedRow = -1;
            } else {
                table.setRowSelectionInterval(clickedRow, clickedRow);
                lastClickedRow = clickedRow;
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        SidePanel = new javax.swing.JPanel();
        AccountApproval = new javax.swing.JPanel();
        account = new javax.swing.JLabel();
        PasswordReset = new javax.swing.JPanel();
        password = new javax.swing.JLabel();
        MainDesktop = new javax.swing.JPanel();
        AccountApprovalPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPendingAccounts = new javax.swing.JTable();
        btnApprove = new javax.swing.JButton();
        btnReject = new javax.swing.JButton();
        btnSubmitRequest3 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        txtSearch1 = new javax.swing.JTextField();
        account1 = new javax.swing.JLabel();
        btnSearch1 = new javax.swing.JButton();
        PasswordResetPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblPasswordResetRequests = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSubmitRequest7 = new javax.swing.JButton();
        btnApproveReset = new javax.swing.JButton();
        btnRejectReset = new javax.swing.JButton();
        account2 = new javax.swing.JLabel();
        btnSearch = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1499, 854));
        setUndecorated(true);
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(105, 105, 255));

        jLabel4.setBackground(new java.awt.Color(105, 105, 255));
        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 25)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("X");
        jLabel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel4MouseClicked(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\menu(2).png")); // NOI18N

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 102, 51));
        jLabel3.setText("MANAGEMENT SYSTEM");

        jLabel13.setBackground(new java.awt.Color(255, 255, 255));
        jLabel13.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("SCHOOL EVENT");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel2)
                .addGap(32, 32, 32)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 996, Short.MAX_VALUE)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(11, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel13))
                        .addGap(26, 26, 26))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18))))
        );

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1520, 70));

        SidePanel.setBackground(new java.awt.Color(51, 51, 51));
        SidePanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        AccountApproval.setBackground(new java.awt.Color(51, 51, 51));
        AccountApproval.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                AccountApprovalMouseClicked(evt);
            }
        });
        AccountApproval.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        account.setBackground(new java.awt.Color(0, 0, 0));
        account.setFont(new java.awt.Font("Segoe UI Emoji", 1, 15)); // NOI18N
        account.setForeground(new java.awt.Color(255, 102, 51));
        account.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\add.png")); // NOI18N
        account.setText(" Account Approval");
        AccountApproval.add(account, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 200, 70));

        SidePanel.add(AccountApproval, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 30, 270, 71));

        PasswordReset.setBackground(new java.awt.Color(51, 51, 51));
        PasswordReset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                PasswordResetMouseClicked(evt);
            }
        });
        PasswordReset.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        password.setBackground(new java.awt.Color(0, 0, 0));
        password.setFont(new java.awt.Font("Segoe UI Emoji", 1, 15)); // NOI18N
        password.setForeground(new java.awt.Color(255, 102, 51));
        password.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\rotation-lock(1).png")); // NOI18N
        password.setText("  Password Reset");
        PasswordReset.add(password, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 200, 70));

        SidePanel.add(PasswordReset, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 100, 270, 70));

        getContentPane().add(SidePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 270, 790));

        MainDesktop.setLayout(new java.awt.CardLayout());

        AccountApprovalPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblPendingAccounts.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Student ID", "Full Name", "Course"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPendingAccounts.setFocusable(false);
        tblPendingAccounts.setRowHeight(25);
        tblPendingAccounts.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblPendingAccounts.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblPendingAccounts.setShowGrid(false);
        tblPendingAccounts.setShowHorizontalLines(true);
        tblPendingAccounts.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblPendingAccounts);

        AccountApprovalPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 150, 1178, 540));

        btnApprove.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnApprove.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\check-mark.png")); // NOI18N
        btnApprove.setText("Approve");
        btnApprove.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnApproveMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnApproveMouseExited(evt);
            }
        });
        btnApprove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApproveActionPerformed(evt);
            }
        });
        AccountApprovalPanel.add(btnApprove, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 710, 140, 40));

        btnReject.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnReject.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\close(1).png")); // NOI18N
        btnReject.setText(" Reject");
        btnReject.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnRejectMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnRejectMouseExited(evt);
            }
        });
        btnReject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRejectActionPerformed(evt);
            }
        });
        AccountApprovalPanel.add(btnReject, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 710, 140, 40));

        btnSubmitRequest3.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSubmitRequest3.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\undo(1).png")); // NOI18N
        btnSubmitRequest3.setText("Back");
        btnSubmitRequest3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSubmitRequest3MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSubmitRequest3MouseExited(evt);
            }
        });
        btnSubmitRequest3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitRequest3ActionPerformed(evt);
            }
        });
        AccountApprovalPanel.add(btnSubmitRequest3, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 90, 110, 40));

        jLabel6.setBackground(new java.awt.Color(0, 0, 0));
        jLabel6.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel6.setText("Search :");
        AccountApprovalPanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 80, 90, 60));

        txtSearch1.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearch1ActionPerformed(evt);
            }
        });
        AccountApprovalPanel.add(txtSearch1, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 90, 220, 40));

        account1.setBackground(new java.awt.Color(255, 255, 255));
        account1.setFont(new java.awt.Font("Segoe UI Emoji", 1, 24)); // NOI18N
        account1.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(1).png")); // NOI18N
        account1.setText(" Account Approval Page");
        AccountApprovalPanel.add(account1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 320, 50));

        btnSearch1.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSearch1.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\search(1).png")); // NOI18N
        btnSearch1.setText("Search");
        btnSearch1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSearch1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSearch1MouseExited(evt);
            }
        });
        btnSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearch1ActionPerformed(evt);
            }
        });
        AccountApprovalPanel.add(btnSearch1, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 90, -1, 40));

        MainDesktop.add(AccountApprovalPanel, "card2");

        PasswordResetPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblPasswordResetRequests.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Request ID", "Student ID", "Student Name", "New Password", "Reason", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPasswordResetRequests.setFocusable(false);
        tblPasswordResetRequests.setRowHeight(25);
        tblPasswordResetRequests.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblPasswordResetRequests.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblPasswordResetRequests.setShowGrid(false);
        tblPasswordResetRequests.setShowHorizontalLines(true);
        tblPasswordResetRequests.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tblPasswordResetRequests);

        PasswordResetPanel.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 150, 1178, 540));

        jLabel5.setBackground(new java.awt.Color(0, 0, 0));
        jLabel5.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel5.setText("Search :");
        PasswordResetPanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 80, 90, 60));

        txtSearch.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        PasswordResetPanel.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 90, 220, 40));

        btnSubmitRequest7.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSubmitRequest7.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\undo(1).png")); // NOI18N
        btnSubmitRequest7.setText("Back");
        btnSubmitRequest7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSubmitRequest7MouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSubmitRequest7MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSubmitRequest7MouseExited(evt);
            }
        });
        btnSubmitRequest7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitRequest7ActionPerformed(evt);
            }
        });
        PasswordResetPanel.add(btnSubmitRequest7, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 90, 110, 40));

        btnApproveReset.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnApproveReset.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\check-mark.png")); // NOI18N
        btnApproveReset.setText("Approve");
        btnApproveReset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnApproveResetMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnApproveResetMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnApproveResetMouseExited(evt);
            }
        });
        btnApproveReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApproveResetActionPerformed(evt);
            }
        });
        PasswordResetPanel.add(btnApproveReset, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 710, 140, 40));

        btnRejectReset.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnRejectReset.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\close(1).png")); // NOI18N
        btnRejectReset.setText(" Reject");
        btnRejectReset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnRejectResetMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnRejectResetMouseExited(evt);
            }
        });
        btnRejectReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRejectResetActionPerformed(evt);
            }
        });
        PasswordResetPanel.add(btnRejectReset, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 710, 140, 40));

        account2.setBackground(new java.awt.Color(255, 255, 255));
        account2.setFont(new java.awt.Font("Segoe UI Emoji", 1, 24)); // NOI18N
        account2.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(1).png")); // NOI18N
        account2.setText(" Password Reset Page");
        PasswordResetPanel.add(account2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 320, 50));

        btnSearch.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSearch.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\search(1).png")); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnSearchMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSearchMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSearchMouseExited(evt);
            }
        });
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });
        PasswordResetPanel.add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 90, -1, 40));

        MainDesktop.add(PasswordResetPanel, "card3");

        getContentPane().add(MainDesktop, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 70, 1250, 790));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel4MouseClicked

    private void AccountApprovalMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_AccountApprovalMouseClicked
        // TODO add your handling code here:
          
    }//GEN-LAST:event_AccountApprovalMouseClicked

    private void PasswordResetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PasswordResetMouseClicked
        // TODO add your handling code here:
        
    }//GEN-LAST:event_PasswordResetMouseClicked

    private void btnApproveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApproveActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblPendingAccounts.getSelectedRow();
        if (selectedRow != -1) {
            String selectedId = tblPendingAccounts.getValueAt(selectedRow, 0).toString();
            String studentName = tblPendingAccounts.getValueAt(selectedRow, 2).toString();

            int confirm = JOptionPane.showConfirmDialog(null, 
                "Are you sure you want to APPROVE student: " + studentName + "?", 
                "Confirm Approval", 
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                approveStudent(selectedId);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please select a student to approve.");
        
    }
    }//GEN-LAST:event_btnApproveActionPerformed

    private void btnRejectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRejectActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblPendingAccounts.getSelectedRow();
        if (selectedRow != -1) {
            String selectedId = tblPendingAccounts.getValueAt(selectedRow, 0).toString();
            String studentName = tblPendingAccounts.getValueAt(selectedRow, 2).toString();

            
            int confirm = JOptionPane.showConfirmDialog(null, 
                "Are you sure you want to REJECT student: " + studentName + "?", 
                "Confirm Rejection", 
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
    
                rejectStudent(selectedId);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please select a student to reject.");
        }
    
    }//GEN-LAST:event_btnRejectActionPerformed

    private void btnSubmitRequest3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitRequest3ActionPerformed
        // TODO add your handling code here:
          
        new AdminDashBoardForm().setVisible(true);
               this.dispose();

    }//GEN-LAST:event_btnSubmitRequest3ActionPerformed

    private void btnApproveMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveMouseEntered
        // TODO add your handling code here:
          btnApprove.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnApproveMouseEntered

    private void btnRejectMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectMouseEntered
        // TODO add your handling code here
          btnReject.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnRejectMouseEntered

    private void btnSubmitRequest3MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest3MouseEntered
        // TODO add your handling code here:
        btnSubmitRequest3.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnSubmitRequest3MouseEntered

    private void btnSubmitRequest3MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest3MouseExited
        // TODO add your handling code here:
                      btnSubmitRequest3.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnSubmitRequest3MouseExited

    private void btnApproveMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveMouseExited
        // TODO add your handling code here:
                      btnApprove.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnApproveMouseExited

    private void btnRejectMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectMouseExited
        // TODO add your handling code here:
                      btnReject.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnRejectMouseExited

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
   
    }//GEN-LAST:event_txtSearchActionPerformed

    private void btnSubmitRequest7MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest7MouseEntered
        // TODO add your handling code here:
         btnSubmitRequest7.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnSubmitRequest7MouseEntered

    private void btnSubmitRequest7MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest7MouseExited
        // TODO add your handling code here:
           btnSubmitRequest7.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnSubmitRequest7MouseExited

    private void btnSubmitRequest7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitRequest7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSubmitRequest7ActionPerformed

    private void btnApproveResetMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveResetMouseEntered
        // TODO add your handling code here:
           btnApproveReset.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnApproveResetMouseEntered

    private void btnApproveResetMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveResetMouseExited
        // TODO add your handling code here:
        btnApproveReset.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnApproveResetMouseExited

    private void btnApproveResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnApproveResetActionPerformed
        // TODO add your handling code here:
           int selectedRow = tblPasswordResetRequests.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a request to approve.");
            return;
        }

        int requestId = Integer.parseInt(tblPasswordResetRequests.getValueAt(selectedRow, 0).toString());
        String studentId = tblPasswordResetRequests.getValueAt(selectedRow, 1).toString();
        String newPassword = tblPasswordResetRequests.getValueAt(selectedRow, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(null,
            "Approve password reset for Student ID: " + studentId + "?",
            "Confirm Approval",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");

        
               PreparedStatement updatePwd = con.prepareStatement("UPDATE users SET password = ? WHERE student_id = ?");
                updatePwd.setString(1, newPassword);
                updatePwd.setString(2, studentId);
                updatePwd.executeUpdate();

     
                PreparedStatement updateStatus = con.prepareStatement("UPDATE password_resets SET status = 'approved' WHERE id = ?");
                updateStatus.setInt(1, requestId);
                updateStatus.executeUpdate();

                JOptionPane.showMessageDialog(null, "Password reset approved.");
                loadPasswordResetRequests();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
            }
        }
    

        
    }//GEN-LAST:event_btnApproveResetActionPerformed

    private void btnRejectResetMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectResetMouseEntered
        // TODO add your handling code here:
         btnRejectReset.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnRejectResetMouseEntered

    private void btnRejectResetMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectResetMouseExited
        // TODO add your handling code here:
           btnRejectReset.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnRejectResetMouseExited

    private void btnRejectResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRejectResetActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblPasswordResetRequests.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a request to reject.");
            return;
        }

        int requestId = Integer.parseInt(tblPasswordResetRequests.getValueAt(selectedRow, 0).toString());
        String studentId = tblPasswordResetRequests.getValueAt(selectedRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(null,
            "Reject password reset for Student ID: " + studentId + "?",
            "Confirm Rejection",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");

                PreparedStatement updateStatus = con.prepareStatement("UPDATE password_resets SET status = 'rejected' WHERE id = ?");
                updateStatus.setInt(1, requestId);
                updateStatus.executeUpdate();

                JOptionPane.showMessageDialog(null, "Password reset rejected.");
                loadPasswordResetRequests();

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage());
            }
        }
    

    }//GEN-LAST:event_btnRejectResetActionPerformed

    private void btnApproveResetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveResetMouseClicked
        // TODO add your handling code here:
        
    }//GEN-LAST:event_btnApproveResetMouseClicked

    private void btnSubmitRequest7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest7MouseClicked
        // TODO add your handling code here:
         new AdminDashBoardForm().setVisible(true);
               this.dispose();
    }//GEN-LAST:event_btnSubmitRequest7MouseClicked

    private void txtSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearch1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearch1ActionPerformed

    private void btnSearch1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearch1MouseEntered
        // TODO add your handling code here:
        btnSearch1.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSearch1MouseEntered

    private void btnSearch1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearch1MouseExited
        // TODO add your handling code here:
        btnSearch1.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnSearch1MouseExited

    private void btnSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearch1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearch1ActionPerformed

    private void btnSearchMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseEntered
        // TODO add your handling code here:
           btnSearch.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSearchMouseEntered

    private void btnSearchMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseExited
        // TODO add your handling code here:
          btnSearch.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnSearchMouseExited

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnSearchMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearchMouseClicked

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
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AdminDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AdminDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AdminDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AdminDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PendingApprovalForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AccountApproval;
    private javax.swing.JPanel AccountApprovalPanel;
    private javax.swing.JPanel MainDesktop;
    private javax.swing.JPanel PasswordReset;
    private javax.swing.JPanel PasswordResetPanel;
    private javax.swing.JPanel SidePanel;
    private javax.swing.JLabel account;
    private javax.swing.JLabel account1;
    private javax.swing.JLabel account2;
    private javax.swing.JButton btnApprove;
    private javax.swing.JButton btnApproveReset;
    private javax.swing.JButton btnReject;
    private javax.swing.JButton btnRejectReset;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSearch1;
    private javax.swing.JButton btnSubmitRequest3;
    private javax.swing.JButton btnSubmitRequest7;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel password;
    private javax.swing.JTable tblPasswordResetRequests;
    private javax.swing.JTable tblPendingAccounts;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextField txtSearch1;
    // End of variables declaration//GEN-END:variables


}
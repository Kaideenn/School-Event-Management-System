import db.SessionManager;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.*;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;




/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author bihas
 */
public class UserManagementForm extends javax.swing.JFrame {

    /**
     * Creates new form UserManagementForm
     */
    public UserManagementForm() {
        if (!SessionManager.isAdminLoggedIn) {
            JOptionPane.showMessageDialog(null, "Access denied. Please log in first.");
            new RoleSelectionForm().setVisible(true);
            dispose(); 
            return;
        }

    initComponents();
    loadAllStudents();
    this.setLocationRelativeTo(null);
    styleTableHeader12345();
    populateCourseFilter();
    
btnSearch.addActionListener(e -> searchStudents());

comboCourseFilter.addActionListener(e -> filterStudentsByCourse());

btnEditStudent.addActionListener(e -> {
    int row = tblStudents.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(null, "Select a student to edit.");
        return;
    }

    String studentId = tblStudents.getValueAt(row, 0).toString();
    String originalName = tblStudents.getValueAt(row, 1).toString();
    String originalCourse = tblStudents.getValueAt(row, 3).toString(); 

    JTextField txtName = new JTextField(originalName);
    JPasswordField txtPassword = new JPasswordField();
    JComboBox<String> comboCourses = new JComboBox<>();


    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT course_name FROM courses");
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            comboCourses.addItem(rs.getString("course_name"));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to load courses.");
        return;
    }

    comboCourses.setSelectedItem(originalCourse);

    JPanel panel = new JPanel(new GridLayout(0, 1));
    panel.add(new JLabel("Full Name:"));
    panel.add(txtName);
    panel.add(new JLabel("Course:"));
    panel.add(comboCourses);
    panel.add(new JLabel("New Password (leave blank to keep current):"));
    panel.add(txtPassword);

    int result = JOptionPane.showConfirmDialog(null, panel, "Edit Student Info",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (result != JOptionPane.OK_OPTION) {
        return;
    }

    String newName = txtName.getText().trim();
    String newCourse = comboCourses.getSelectedItem().toString().trim();
    String newPassword = new String(txtPassword.getPassword()).trim();

    if (newName.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Full name cannot be empty.");
        return;
    }

    boolean nameChanged = !newName.equals(originalName);
    boolean courseChanged = !newCourse.equals(originalCourse);
    boolean passwordChanged = !newPassword.isEmpty();

    if (!nameChanged && !courseChanged && !passwordChanged) {
        JOptionPane.showMessageDialog(null, "No changes were made.");
        return;
    }

    try (Connection con = getConnection()) {
        if (nameChanged || courseChanged) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET full_name = ?, course = ? WHERE student_id = ?");
            ps.setString(1, newName);
            ps.setString(2, newCourse);
            ps.setString(3, studentId);
            ps.executeUpdate();
            ps.close();
        }

        if (passwordChanged) {
            PreparedStatement psPass = con.prepareStatement(
                "UPDATE users SET password = ? WHERE student_id = ?");
            psPass.setString(1, newPassword);
            psPass.setString(2, studentId);
            psPass.executeUpdate();
            psPass.close();
        }

        JOptionPane.showMessageDialog(null, "Student info updated successfully.");


        comboCourseFilter.setSelectedIndex(0);
        txtSearch.setText("");
        populateCourseFilter();
        loadAllStudents();

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to update student.");
    }
});
        
    btnToggleEnabled.addActionListener(e -> {
    int selectedRow = tblStudents.getSelectedRow();

    if (selectedRow < 0) {
        JOptionPane.showMessageDialog(null, "Please select a student to toggle.");
        return;
    }

    String studentId = tblStudents.getValueAt(selectedRow, 0).toString();
    String currentStatus = tblStudents.getValueAt(selectedRow, 5).toString(); 

    boolean isCurrentlyEnabled = currentStatus.equalsIgnoreCase("Enabled");
    boolean newStatus = !isCurrentlyEnabled;

    int confirm = JOptionPane.showConfirmDialog(
        null,
        "Are you sure you want to " + (newStatus ? "enable" : "disable") + " this student?",
        "Confirm",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm == JOptionPane.YES_OPTION) {
        try (Connection con = getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE users SET is_enabled = ? WHERE student_id = ?"
            );
            ps.setBoolean(1, newStatus);
            ps.setString(2, studentId);

            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Student has been " + (newStatus ? "enabled." : "disabled."));
            comboCourseFilter.setSelectedIndex(0);
txtSearch.setText(""); 
 populateCourseFilter();
            loadAllStudents(); 
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "❌ Error updating student status:\n" + ex.getMessage());
        }
    }
});
        
        btnDeleteStudent.addActionListener(e -> {
    int row = tblStudents.getSelectedRow();
    if (row < 0) {
        JOptionPane.showMessageDialog(null, "Select a student to delete.");
        return;
    }

    String studentId = tblStudents.getValueAt(row, 0).toString();
    int confirm = JOptionPane.showConfirmDialog(null, "Are you sure to delete?", "Confirm", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE student_id = ?")) {
            ps.setString(1, studentId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Student deleted.");
           
            comboCourseFilter.setSelectedIndex(0); 
txtSearch.setText(""); 
 populateCourseFilter();
  loadAllStudents(); 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
});
       
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
        jLabel1 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        comboCourseFilter = new javax.swing.JComboBox<>();
        btnEditStudent = new javax.swing.JButton();
        btnToggleEnabled = new javax.swing.JButton();
        btnDeleteStudent = new javax.swing.JButton();
        btnSubmitRequest4 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblStudents = new javax.swing.JTable();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1520, 854));
        setMinimumSize(new java.awt.Dimension(1520, 854));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1520, 854));
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

        jLabel1.setFont(new java.awt.Font("STXihei", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 102, 255));
        jLabel1.setText("Management ");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 80, 270, 60));

        txtSearch.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        getContentPane().add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 180, 230, 40));

        btnSearch.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSearch.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\search(1).png")); // NOI18N
        btnSearch.setText("Search");
        btnSearch.addMouseListener(new java.awt.event.MouseAdapter() {
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
        getContentPane().add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 180, -1, 40));

        comboCourseFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboCourseFilterActionPerformed(evt);
            }
        });
        getContentPane().add(comboCourseFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 180, 210, 40));

        btnEditStudent.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnEditStudent.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\user-avatar.png")); // NOI18N
        btnEditStudent.setText("Edit");
        btnEditStudent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEditStudentMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEditStudentMouseExited(evt);
            }
        });
        getContentPane().add(btnEditStudent, new org.netbeans.lib.awtextra.AbsoluteConstraints(1000, 800, 130, 40));

        btnToggleEnabled.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnToggleEnabled.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\toggle.png")); // NOI18N
        btnToggleEnabled.setText("Toggle");
        btnToggleEnabled.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnToggleEnabledMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnToggleEnabledMouseExited(evt);
            }
        });
        btnToggleEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToggleEnabledActionPerformed(evt);
            }
        });
        getContentPane().add(btnToggleEnabled, new org.netbeans.lib.awtextra.AbsoluteConstraints(1170, 800, 130, 40));

        btnDeleteStudent.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnDeleteStudent.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\trash(1).png")); // NOI18N
        btnDeleteStudent.setText("Delete");
        btnDeleteStudent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnDeleteStudentMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnDeleteStudentMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnDeleteStudentMouseExited(evt);
            }
        });
        getContentPane().add(btnDeleteStudent, new org.netbeans.lib.awtextra.AbsoluteConstraints(1340, 800, 130, 40));

        btnSubmitRequest4.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSubmitRequest4.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\undo(1).png")); // NOI18N
        btnSubmitRequest4.setText("Back");
        btnSubmitRequest4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSubmitRequest4MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSubmitRequest4MouseExited(evt);
            }
        });
        btnSubmitRequest4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSubmitRequest4ActionPerformed(evt);
            }
        });
        getContentPane().add(btnSubmitRequest4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1370, 180, -1, -1));

        tblStudents.setModel(new javax.swing.table.DefaultTableModel(
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
                "Student ID", "Student Name", "Event Name", "Password", "Course", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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
        tblStudents.setFocusable(false);
        tblStudents.setRowHeight(25);
        tblStudents.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblStudents.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblStudents.setShowGrid(false);
        tblStudents.setShowHorizontalLines(true);
        tblStudents.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tblStudents);

        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 240, 1410, 540));

        jLabel8.setBackground(new java.awt.Color(0, 0, 0));
        jLabel8.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel8.setText("FIlter :");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 170, 60, 60));

        jLabel9.setBackground(new java.awt.Color(0, 0, 0));
        jLabel9.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel9.setText("Search :");
        getContentPane().add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 170, 90, 60));

        jLabel5.setFont(new java.awt.Font("STXihei", 1, 36)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 102, 102));
        jLabel5.setText("Student  ");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 80, 160, 60));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel4MouseClicked

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearchActionPerformed

    private void btnToggleEnabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToggleEnabledActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnToggleEnabledActionPerformed

    private void btnSubmitRequest4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitRequest4ActionPerformed
        // TODO add your handling code here:
        new AdminDashBoardForm().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSubmitRequest4ActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnSubmitRequest4MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest4MouseEntered
        // TODO add your handling code here:
          
                         btnSubmitRequest4.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnSubmitRequest4MouseEntered

    private void btnSubmitRequest4MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest4MouseExited
        // TODO add your handling code here:
         btnSubmitRequest4.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnSubmitRequest4MouseExited

    private void btnDeleteStudentMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteStudentMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteStudentMouseClicked

    private void btnDeleteStudentMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteStudentMouseEntered
        // TODO add your handling code here:
          btnDeleteStudent.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnDeleteStudentMouseEntered

    private void btnDeleteStudentMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteStudentMouseExited
        // TODO add your handling code here:
         btnDeleteStudent.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnDeleteStudentMouseExited

    private void btnToggleEnabledMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnToggleEnabledMouseEntered
        // TODO add your handling code here:
          btnToggleEnabled.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnToggleEnabledMouseEntered

    private void btnToggleEnabledMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnToggleEnabledMouseExited
        // TODO add your handling code here:
         btnToggleEnabled.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnToggleEnabledMouseExited

    private void btnEditStudentMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditStudentMouseEntered
        // TODO add your handling code here:
          btnEditStudent.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnEditStudentMouseEntered

    private void btnEditStudentMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditStudentMouseExited
        // TODO add your handling code here:
         btnEditStudent.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnEditStudentMouseExited

    private void btnSearchMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseEntered
        // TODO add your handling code here:
            btnSearch.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSearchMouseEntered

    private void btnSearchMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseExited
        // TODO add your handling code here:
         btnSearch.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnSearchMouseExited

    private void comboCourseFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboCourseFilterActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboCourseFilterActionPerformed

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
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(UserManagementForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new UserManagementForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDeleteStudent;
    private javax.swing.JButton btnEditStudent;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSubmitRequest4;
    private javax.swing.JButton btnToggleEnabled;
    private javax.swing.JComboBox<String> comboCourseFilter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable tblStudents;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

private void searchStudents() {
    String keyword = txtSearch.getText().trim();

    if (keyword.isEmpty()) {
        if (comboCourseFilter.getSelectedIndex() > 0) {
            filterStudentsByCourse();
        } else {
            loadAllStudents();
        }
        return;
    }

    String query = "SELECT student_id, full_name, password, course, status, is_enabled " +
                   "FROM users " +
                   "WHERE student_id LIKE ? OR full_name LIKE ? OR course LIKE ? OR status LIKE ? OR " +
                   "(? LIKE 'enabled' AND is_enabled = TRUE) OR (? LIKE 'disabled' AND is_enabled = FALSE)";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        for (int i = 1; i <= 4; i++) {
            ps.setString(i, "%" + keyword + "%");
        }
        ps.setString(5, keyword.toLowerCase()); 
        ps.setString(6, keyword.toLowerCase()); 

        ResultSet rs = ps.executeQuery();
        updateStudentTable(rs); 

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "❌ Search failed: " + ex.getMessage());
    }
}


private void filterStudentsByCourse() {
    Object selectedObject = comboCourseFilter.getSelectedItem();


    if (selectedObject == null) {
        return; 
    }

    String selectedCourse = selectedObject.toString().trim();

    if (selectedCourse.equalsIgnoreCase("All")) {

        if (!txtSearch.getText().trim().isEmpty()) {
            searchStudents();
        } else {
            loadAllStudents();
        }
        return;
    }

    String query = "SELECT student_id, full_name, password, course, status, is_enabled FROM users WHERE TRIM(LOWER(course)) = LOWER(?)";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        ps.setString(1, selectedCourse);
        ResultSet rs = ps.executeQuery();
        updateStudentTable(rs);

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to filter students by course.");
    }
}


private void updateStudentTable(ResultSet rs) throws Exception {
    DefaultTableModel model = new DefaultTableModel(
        new Object[]{"Student ID", "Full Name", "Password", "Course", "Status", "Account Control"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    tblStudents.setModel(model); 


    for (MouseListener ml : tblStudents.getMouseListeners()) {
        tblStudents.removeMouseListener(ml);
    }

    tblStudents.addMouseListener(new MouseAdapter() {
        private int lastClickedRow = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            int clickedRow = tblStudents.rowAtPoint(e.getPoint());
            if (clickedRow == -1) return;

            if (clickedRow == lastClickedRow) {
                tblStudents.clearSelection();
                lastClickedRow = -1;
            } else {
                tblStudents.setRowSelectionInterval(clickedRow, clickedRow);
                lastClickedRow = clickedRow;
            }
        }
    });


    tblStudents.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String accountStatus = table.getModel().getValueAt(row, 5).toString(); 

            if (accountStatus.equalsIgnoreCase("Disabled")) {
                c.setForeground(Color.DARK_GRAY);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                if (!isSelected) {
                    c.setBackground(new Color(255, 220, 220));
                }
            } else {
                c.setForeground(Color.BLACK);
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
            }

            return c;
        }
    });

    boolean hasRows = false;

    while (rs.next()) {
        hasRows = true;

        String studentId = rs.getString("student_id");
        String fullName = rs.getString("full_name");
        String password = rs.getString("password");
        String course = rs.getString("course");
        String status = rs.getString("status");
        boolean isEnabled = rs.getBoolean("is_enabled");

        String accountControl = isEnabled ? "Enabled" : "Disabled";


        model.addRow(new Object[]{studentId, fullName, password, course, status, accountControl});
    }

    if (!hasRows) {
        JOptionPane.showMessageDialog(null, "No matching students found.");
    }
}

private void loadAllStudents() {

    DefaultTableModel model = new DefaultTableModel(
        new Object[]{"Student ID", "Full Name", "Password", "Course", "Status", "Account Control"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; 
        }
    };
    tblStudents.setModel(model); 

 
    tblStudents.addMouseListener(new MouseAdapter() {
        private int lastClickedRow = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            int clickedRow = tblStudents.rowAtPoint(e.getPoint());
            if (clickedRow == -1) return; 

            if (clickedRow == lastClickedRow) {
                tblStudents.clearSelection();
                lastClickedRow = -1;
            } else {
                tblStudents.setRowSelectionInterval(clickedRow, clickedRow);
                lastClickedRow = clickedRow;
            }
        }
    });


    tblStudents.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String accountStatus = table.getModel().getValueAt(row, 5).toString();
            if (accountStatus.equalsIgnoreCase("Disabled")) {
                c.setForeground(Color.DARK_GRAY);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                if (!isSelected) {
                    c.setBackground(new Color(255, 220, 220)); 
                }
            } else {
                c.setForeground(Color.BLACK);
                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
            }

            return c;
        }
    });

    String query = "SELECT student_id, full_name, password, course, status, is_enabled FROM users";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query);
         ResultSet rs = ps.executeQuery()) {

        boolean hasData = false;

        while (rs.next()) {
            hasData = true;

            String id = rs.getString("student_id");
            String name = rs.getString("full_name");
            String password = rs.getString("password");
            String course = rs.getString("course");
            String status = rs.getString("status");
            boolean isEnabled = rs.getBoolean("is_enabled");

            String enabledStatus = isEnabled ? "Enabled" : "Disabled";


            model.addRow(new Object[]{id, name, password, course, status, enabledStatus});
        }

        if (!hasData) {
            JOptionPane.showMessageDialog(null, "⚠ No student records found.");
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "❌ Failed to load student data: " + e.getMessage());
    }
}



    private void styleTableHeader12345() {
        tblStudents.getTableHeader().setDefaultRenderer(new HeaderColor());
        tblStudents.setRowHeight(45);

        JTableHeader header = tblStudents.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 50));
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(0, 153, 153));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Highlighting logic for "Disabled"
        String accountStatus = table.getModel().getValueAt(row, 5).toString();
        if (accountStatus.equalsIgnoreCase("Disabled")) {
            c.setForeground(Color.DARK_GRAY);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            if (!isSelected) {
                c.setBackground(new Color(255, 220, 220));
            }
        } else {
            c.setForeground(Color.BLACK);
            if (!isSelected) {
                c.setBackground(Color.WHITE);
            }
        }

       
        setHorizontalAlignment(JLabel.LEFT);
        return c;
    }
};


for (int i = 0; i < tblStudents.getColumnCount(); i++) {
    tblStudents.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
}
    }
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/event_system", "root", "");
    }

    private static class HeaderColor extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setBackground(new Color(0, 153, 153));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setHorizontalAlignment(JLabel.LEFT);
            return this;
        }
    }

   private void populateCourseFilter() {
    comboCourseFilter.removeAllItems();
    comboCourseFilter.addItem("All");

    try (Connection con = getConnection();
         Statement st = con.createStatement();
         ResultSet rs = st.executeQuery("SELECT DISTINCT TRIM(course) as course FROM users")) {

        while (rs.next()) {
            comboCourseFilter.addItem(rs.getString("course"));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}



}

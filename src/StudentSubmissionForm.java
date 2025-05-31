
import static db.DBConnection.getConnection;
import db.SessionManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.List;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;





/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author bihas
 */


public class StudentSubmissionForm extends javax.swing.JFrame {

    public StudentSubmissionForm() {
        if (!SessionManager.isAdminLoggedIn) {
            JOptionPane.showMessageDialog(null, "Access denied. Please log in first.");
            new RoleSelectionForm().setVisible(true);
            dispose();
            return;
        }

        initComponents();
        this.setLocationRelativeTo(null);
        styleTableHeader0();
        styleTableHeader01();
       populateCourseFilterCombo();
loadSubmittedStudents("All", "");

        
        requirementsPanel.setLayout(new BoxLayout(requirementsPanel, BoxLayout.Y_AXIS));

  
        MainDesktop.setLayout(new CardLayout());
        MainDesktop.add(cardSubmittedStudentsPanel, "cardSubmittedStudentsPanel");
        MainDesktop.add(cardApprovedStudentsPanel, "cardApprovedStudentsPanel");
        MainDesktop.add(cardSubmissionStudentForm, "cardSubmissionStudentForm"); 

        
        CardLayout card = (CardLayout) MainDesktop.getLayout();
        card.show(MainDesktop, "cardSubmittedStudentsPanel");

        

        SubmittedStudents.setBackground(new Color(0, 0, 0));
        ApprovedStudents.setBackground(new Color(51, 51, 51));

        final JPanel[] lastClicked = {null};

        SubmittedStudents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SubmittedStudents.setBackground(new Color(0, 0, 0));
                if (lastClicked[0] != null && lastClicked[0] != SubmittedStudents) {
                    lastClicked[0].setBackground(new Color(51, 51, 51));
                }
                lastClicked[0] = SubmittedStudents;

                card.show(MainDesktop, "cardSubmittedStudentsPanel");
            }
        });

        ApprovedStudents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ApprovedStudents.setBackground(new Color(0, 0, 0));
                if (lastClicked[0] != null && lastClicked[0] != ApprovedStudents) {
                    lastClicked[0].setBackground(new Color(51, 51, 51));
                }
                lastClicked[0] = ApprovedStudents;

                card.show(MainDesktop, "cardApprovedStudentsPanel");
            }
        });

      DefaultTableModel modelSubmitted = new DefaultTableModel(new String[]{
    "Student ID", "Student Name", "Event ID", "Event Name", "Course", "Time of Submission", "Status"
}, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};

DefaultTableModel modelApproved = new DefaultTableModel(new String[]{
    "Student ID", "Student Name", "Event ID", "Event Name", "Course", "Time of Submission", "Status"
}, 0) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};

tblSubmittedStudents.setModel(modelSubmitted);
    populateCourseFilterCombo();
loadSubmittedStudents("All", "");

        
        
TableColumn eventIdColumn = tblSubmittedStudents.getColumnModel().getColumn(2);

eventIdColumn.setMinWidth(0);
eventIdColumn.setMaxWidth(0);
eventIdColumn.setPreferredWidth(0);
eventIdColumn.setWidth(0);

tblApprovedStudents.setModel(modelApproved);
loadApprovedStudents(""); 



TableColumn eventIdColumnApproved = tblApprovedStudents.getColumnModel().getColumn(2);
eventIdColumnApproved.setMinWidth(0);
eventIdColumnApproved.setMaxWidth(0);
eventIdColumnApproved.setPreferredWidth(0);
eventIdColumnApproved.setWidth(0);


      btnViewSubmission.addActionListener(e -> {
    int selectedRow = tblSubmittedStudents.getSelectedRow();
    if (selectedRow < 0) {
        JOptionPane.showMessageDialog(null, "Please select a student submission to review.");
        return;
    }

    String studentId = tblSubmittedStudents.getValueAt(selectedRow, 0).toString();
    int eventId = Integer.parseInt(tblSubmittedStudents.getValueAt(selectedRow, 2).toString());


    loadStudentSubmissionForReview(studentId, eventId);
    card.show(MainDesktop, "cardSubmissionStudentForm");


    String status = tblSubmittedStudents.getValueAt(selectedRow, 3).toString().toLowerCase(); 


    if (status.equals("approved")) {
        btnApproveSubmission.setEnabled(false);
        btnRejectSubmission.setEnabled(false);
    } else {
        btnApproveSubmission.setEnabled(true);
        btnRejectSubmission.setEnabled(true);
    }
});



   btnApproveSubmission.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(
        null,
        "Are you sure you want to approve this submission?",
        "Confirm Approval",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) {
        return;
    }

    String studentId = txtStudentID.getText().trim();
    int eventId;

    try {
        eventId = Integer.parseInt(txtEventID.getText().trim());
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(null, "Invalid event ID.");
        return;
    }

    try (Connection con = getConnection()) {
        for (String reqName : fileFields.keySet()) {
            try (PreparedStatement pst = con.prepareStatement(
                "UPDATE submissions SET status = 'approved', admin_notes = NULL " +
                "WHERE student_id = ? AND event_id = ? AND requirement_text = ?"
            )) {
                pst.setString(1, studentId);
                pst.setInt(2, eventId);
                pst.setString(3, reqName);
                pst.executeUpdate();
            }
        }

        JOptionPane.showMessageDialog(null, "Submission approved.");

        // Refresh the data and return to the student submission form
        populateCourseFilterCombo();
        loadSubmittedStudents("All", txtSearch1.getText().trim());
        loadApprovedStudents(""); 
        card.show(MainDesktop, "cardSubmittedStudentsPanel");

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to approve submission due to a database error.");
    }
});



  btnRejectSubmission.addActionListener(e -> {
    String note = JOptionPane.showInputDialog(null, "Enter rejection note for the student:");

    if (note == null || note.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "Rejection note is required.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        null,
        "Are you sure you want to reject this submission?",
        "Confirm Rejection",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) {
        return; // Cancel rejection
    }

    try (Connection con = getConnection()) {
        PreparedStatement ps = con.prepareStatement(
            "UPDATE submissions SET status = 'rejected', admin_notes = ? WHERE student_id = ? AND event_id = ?"
        );
        ps.setString(1, note);
        ps.setString(2, txtStudentID.getText()); 
        ps.setInt(3, Integer.parseInt(txtEventID.getText())); 

        ps.executeUpdate();

        JOptionPane.showMessageDialog(null, "Submission rejected and notes saved.");

        populateCourseFilterCombo();
        loadSubmittedStudents("All", txtSearch1.getText().trim());

        CardLayout cardLayout = (CardLayout) MainDesktop.getLayout();
        cardLayout.show(MainDesktop, "cardSubmittedStudentsPanel");

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error rejecting submission:\n" + ex.getMessage());
    }
});




        btnBack.addActionListener(e -> {
            card.show(MainDesktop, "cardSubmittedStudentsPanel");
              populateCourseFilterCombo();
loadSubmittedStudents("All", txtSearch1.getText().trim());
populateCourseFilterCombo();

        });
        
        
 btnViewApprovedRequirements.addActionListener(e -> {
    int selectedRow = tblApprovedStudents.getSelectedRow();
    if (selectedRow < 0) {
        JOptionPane.showMessageDialog(null, "Please select a student submission to review.");
        return;
    }

    String studentId = tblApprovedStudents.getValueAt(selectedRow, 0).toString();
    int eventId = Integer.parseInt(tblApprovedStudents.getValueAt(selectedRow, 2).toString());

    System.out.println("Loading review for Student: " + studentId + ", Event ID: " + eventId);
loadSubmittedStudents("All", txtSearch1.getText().trim());
  
     populateCourseFilterCombo();
    loadStudentSubmissionForReview(studentId, eventId);
    
    card.show(MainDesktop, "cardSubmissionStudentForm");


    btnApproveSubmission.setEnabled(false);
    btnRejectSubmission.setEnabled(false);
});


btnPrintAll.addActionListener(e -> {
    String studentId = txtStudentID.getText();
    int eventId = Integer.parseInt(txtEventID.getText());

    try (Connection con = getConnection()) {
        PreparedStatement stmt = con.prepareStatement(
            "SELECT file_path FROM submissions WHERE student_id = ? AND event_id = ?"
        );
        stmt.setString(1, studentId);
        stmt.setInt(2, eventId);

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String filePath = rs.getString("file_path");

            if (filePath != null && !filePath.trim().isEmpty()) {
                File file = new File(filePath);

                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();

          
                    if (desktop.isSupported(Desktop.Action.PRINT)) {
                        desktop.print(file);
                    }

           
                    Path targetDir = Paths.get("C:/PrintedSubmissions/" + studentId + "_" + eventId);
                    Files.createDirectories(targetDir);

                    Path targetPath = targetDir.resolve(file.getName());
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING); // Copy
                }
            }
        }

        JOptionPane.showMessageDialog(null, "Submitted files printed and copied to folder.");

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to print or copy: " + ex.getMessage());
    }
});

btnPrintApprovedList.addActionListener(e -> {
    String[] options = {"All", "By Course", "By Event", "By Student"};
    String choice = (String) JOptionPane.showInputDialog(
        null,
        "Print approved list by:",
        "Print Options",
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        options[0]
    );

    if (choice == null) return;

    switch (choice) {
        case "All":
            printApprovedList(null, null);
            break;

        case "By Course":
            List<String> courses = getCoursesWithApprovedStudents();
            if (courses.isEmpty()) {
                JOptionPane.showMessageDialog(null, "⚠ No courses with approved students found.");
                return;
            }

            String selectedCourse = (String) JOptionPane.showInputDialog(
                null,
                "Select a course:",
                "Available Courses",
                JOptionPane.QUESTION_MESSAGE,
                null,
                courses.toArray(),
                courses.get(0)
            );

            if (selectedCourse != null) {
                printApprovedList("e.course", selectedCourse);
            }
            break;

        case "By Event":
            List<String> events = getApprovedEventNames();
            if (events.isEmpty()) {
                JOptionPane.showMessageDialog(null, "⚠ No events with approved submissions found.");
                return;
            }

            String selectedEvent = (String) JOptionPane.showInputDialog(
                null,
                "Select an event:",
                "Available Events",
                JOptionPane.QUESTION_MESSAGE,
                null,
                events.toArray(),
                events.get(0)
            );

            if (selectedEvent != null) {
                printApprovedList("e.event_name", selectedEvent);
            }
            break;

        case "By Student":
            List<String> studentIds = getApprovedStudentIds();
            if (studentIds.isEmpty()) {
                JOptionPane.showMessageDialog(null, "⚠ No students with approved submissions found.");
                return;
            }

            String selectedStudentId = (String) JOptionPane.showInputDialog(
                null,
                "Select a student ID:",
                "Approved Students",
                JOptionPane.QUESTION_MESSAGE,
                null,
                studentIds.toArray(),
                studentIds.get(0)
            );

            if (selectedStudentId != null) {
                printApprovedList("s.student_id", selectedStudentId);
            }
            break;
    }
});


btnSearch1.addActionListener(e -> {
    String course = (String) comboCourseFilter.getSelectedItem();
    String searchText = txtSearch1.getText();
    loadSubmittedStudents(course, searchText);
});

comboCourseFilter.addActionListener(e -> {
    String course = (String) comboCourseFilter.getSelectedItem();
    String searchText = txtSearch1.getText();
    loadSubmittedStudents(course, searchText);
});
btnSearch.addActionListener(e -> {
    String searchText = txtSearch.getText().trim();
    loadApprovedStudents(searchText);
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
        SidePanel = new javax.swing.JPanel();
        SubmittedStudents = new javax.swing.JPanel();
        account = new javax.swing.JLabel();
        ApprovedStudents = new javax.swing.JPanel();
        password = new javax.swing.JLabel();
        MainDesktop = new javax.swing.JPanel();
        cardSubmittedStudentsPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblSubmittedStudents = new javax.swing.JTable();
        btnSubmitRequest3 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        txtSearch1 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        btnSearch1 = new javax.swing.JButton();
        btnViewSubmission = new javax.swing.JButton();
        comboCourseFilter = new javax.swing.JComboBox<>();
        account1 = new javax.swing.JLabel();
        cardApprovedStudentsPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblApprovedStudents = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnSubmitRequest7 = new javax.swing.JButton();
        btnViewApprovedRequirements = new javax.swing.JButton();
        btnPrintApprovedList = new javax.swing.JButton();
        account2 = new javax.swing.JLabel();
        cardSubmissionStudentForm = new javax.swing.JPanel();
        txtStudentID = new javax.swing.JTextField();
        txtEventID = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        requirementsPanel = new javax.swing.JPanel();
        btnBack = new javax.swing.JButton();
        btnRejectSubmission = new javax.swing.JButton();
        btnApproveSubmission = new javax.swing.JButton();
        btnPrintAll = new javax.swing.JButton();
        account3 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1520, 854));
        setUndecorated(true);
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

        SubmittedStudents.setBackground(new java.awt.Color(51, 51, 51));
        SubmittedStudents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SubmittedStudentsMouseClicked(evt);
            }
        });
        SubmittedStudents.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        account.setBackground(new java.awt.Color(0, 0, 0));
        account.setFont(new java.awt.Font("Segoe UI Emoji", 1, 15)); // NOI18N
        account.setForeground(new java.awt.Color(255, 102, 51));
        account.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\submission(3).png")); // NOI18N
        account.setText("Students Submissions");
        SubmittedStudents.add(account, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 200, 70));

        SidePanel.add(SubmittedStudents, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 30, 270, 71));

        ApprovedStudents.setBackground(new java.awt.Color(51, 51, 51));
        ApprovedStudents.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ApprovedStudentsMouseClicked(evt);
            }
        });
        ApprovedStudents.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        password.setBackground(new java.awt.Color(0, 0, 0));
        password.setFont(new java.awt.Font("Segoe UI Emoji", 1, 15)); // NOI18N
        password.setForeground(new java.awt.Color(255, 102, 51));
        password.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\approve.png")); // NOI18N
        password.setText("Approved Students");
        ApprovedStudents.add(password, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 200, 70));

        SidePanel.add(ApprovedStudents, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 100, 270, 70));

        getContentPane().add(SidePanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 270, 790));

        MainDesktop.setLayout(new java.awt.CardLayout());

        cardSubmittedStudentsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblSubmittedStudents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Student ID", "Student Name", "Event Name", "Course", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblSubmittedStudents.setFocusable(false);
        tblSubmittedStudents.setRowHeight(25);
        tblSubmittedStudents.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblSubmittedStudents.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblSubmittedStudents.setShowGrid(false);
        tblSubmittedStudents.setShowHorizontalLines(true);
        tblSubmittedStudents.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblSubmittedStudents);

        cardSubmittedStudentsPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 154, 1178, 560));

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
        cardSubmittedStudentsPanel.add(btnSubmitRequest3, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 100, 110, 40));

        jLabel7.setBackground(new java.awt.Color(0, 0, 0));
        jLabel7.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel7.setText("Filter by Course :");
        cardSubmittedStudentsPanel.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 100, 140, 40));

        txtSearch1.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearch1ActionPerformed(evt);
            }
        });
        cardSubmittedStudentsPanel.add(txtSearch1, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 100, 220, 40));

        jLabel6.setBackground(new java.awt.Color(0, 0, 0));
        jLabel6.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel6.setText("Search :");
        cardSubmittedStudentsPanel.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 90, 90, 60));

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
        cardSubmittedStudentsPanel.add(btnSearch1, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 100, -1, 40));

        btnViewSubmission.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnViewSubmission.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\view.png")); // NOI18N
        btnViewSubmission.setText("View Submission");
        btnViewSubmission.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnViewSubmissionMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnViewSubmissionMouseExited(evt);
            }
        });
        btnViewSubmission.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewSubmissionActionPerformed(evt);
            }
        });
        cardSubmittedStudentsPanel.add(btnViewSubmission, new org.netbeans.lib.awtextra.AbsoluteConstraints(1020, 730, 190, 40));

        cardSubmittedStudentsPanel.add(comboCourseFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 100, 230, 40));

        account1.setBackground(new java.awt.Color(255, 255, 255));
        account1.setFont(new java.awt.Font("Segoe UI Emoji", 1, 24)); // NOI18N
        account1.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(1).png")); // NOI18N
        account1.setText("Students Submissions Page");
        cardSubmittedStudentsPanel.add(account1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 360, 70));

        MainDesktop.add(cardSubmittedStudentsPanel, "card2");

        cardApprovedStudentsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblApprovedStudents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Student ID", "Student Name", "Event ID", "Event Name", "Course", "Time of Submission", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblApprovedStudents.setFocusable(false);
        tblApprovedStudents.setRowHeight(25);
        tblApprovedStudents.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblApprovedStudents.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblApprovedStudents.setShowGrid(false);
        tblApprovedStudents.setShowHorizontalLines(true);
        tblApprovedStudents.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tblApprovedStudents);

        cardApprovedStudentsPanel.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 154, 1178, 560));

        jLabel5.setBackground(new java.awt.Color(0, 0, 0));
        jLabel5.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel5.setText("Search :");
        cardApprovedStudentsPanel.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 90, 90, 60));

        txtSearch.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        cardApprovedStudentsPanel.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 100, 220, 40));

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
        cardApprovedStudentsPanel.add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 100, -1, 40));

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
        cardApprovedStudentsPanel.add(btnSubmitRequest7, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 100, 110, 40));

        btnViewApprovedRequirements.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnViewApprovedRequirements.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\search-data.png")); // NOI18N
        btnViewApprovedRequirements.setText("Review");
        btnViewApprovedRequirements.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnViewApprovedRequirementsMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnViewApprovedRequirementsMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnViewApprovedRequirementsMouseExited(evt);
            }
        });
        btnViewApprovedRequirements.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewApprovedRequirementsActionPerformed(evt);
            }
        });
        cardApprovedStudentsPanel.add(btnViewApprovedRequirements, new org.netbeans.lib.awtextra.AbsoluteConstraints(1060, 730, 140, 40));

        btnPrintApprovedList.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnPrintApprovedList.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\printing.png")); // NOI18N
        btnPrintApprovedList.setText("Print");
        btnPrintApprovedList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnPrintApprovedListMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnPrintApprovedListMouseExited(evt);
            }
        });
        btnPrintApprovedList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrintApprovedListActionPerformed(evt);
            }
        });
        cardApprovedStudentsPanel.add(btnPrintApprovedList, new org.netbeans.lib.awtextra.AbsoluteConstraints(880, 730, 140, 40));

        account2.setBackground(new java.awt.Color(255, 255, 255));
        account2.setFont(new java.awt.Font("Segoe UI Emoji", 1, 24)); // NOI18N
        account2.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(1).png")); // NOI18N
        account2.setText("Approved Students Page");
        cardApprovedStudentsPanel.add(account2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, 360, 50));

        MainDesktop.add(cardApprovedStudentsPanel, "card3");

        cardSubmissionStudentForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtStudentID.setEditable(false);
        txtStudentID.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        cardSubmissionStudentForm.add(txtStudentID, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 100, 130, 40));

        txtEventID.setEditable(false);
        txtEventID.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        txtEventID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEventIDActionPerformed(evt);
            }
        });
        cardSubmissionStudentForm.add(txtEventID, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 100, 140, 40));

        jLabel1.setFont(new java.awt.Font("STXihei", 1, 22)); // NOI18N
        jLabel1.setText("Requirements :");
        cardSubmissionStudentForm.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 180, 180, -1));

        jLabel9.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel9.setText("Event ID :");
        cardSubmissionStudentForm.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 100, -1, 40));

        javax.swing.GroupLayout requirementsPanelLayout = new javax.swing.GroupLayout(requirementsPanel);
        requirementsPanel.setLayout(requirementsPanelLayout);
        requirementsPanelLayout.setHorizontalGroup(
            requirementsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1118, Short.MAX_VALUE)
        );
        requirementsPanelLayout.setVerticalGroup(
            requirementsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 478, Short.MAX_VALUE)
        );

        jScrollPane3.setViewportView(requirementsPanel);

        cardSubmissionStudentForm.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 220, 1120, 480));

        btnBack.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnBack.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\undo(1).png")); // NOI18N
        btnBack.setText("Back");
        btnBack.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnBackMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnBackMouseExited(evt);
            }
        });
        cardSubmissionStudentForm.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(1070, 160, 110, 40));

        btnRejectSubmission.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnRejectSubmission.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\prohibition.png")); // NOI18N
        btnRejectSubmission.setText("Reject");
        btnRejectSubmission.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnRejectSubmissionMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnRejectSubmissionMouseExited(evt);
            }
        });
        cardSubmissionStudentForm.add(btnRejectSubmission, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 720, 130, 40));

        btnApproveSubmission.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnApproveSubmission.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\mark.png")); // NOI18N
        btnApproveSubmission.setText("Approve");
        btnApproveSubmission.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnApproveSubmissionMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnApproveSubmissionMouseExited(evt);
            }
        });
        cardSubmissionStudentForm.add(btnApproveSubmission, new org.netbeans.lib.awtextra.AbsoluteConstraints(1050, 720, 130, 40));

        btnPrintAll.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnPrintAll.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\printing.png")); // NOI18N
        btnPrintAll.setText("print all");
        btnPrintAll.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnPrintAllMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnPrintAllMouseExited(evt);
            }
        });
        cardSubmissionStudentForm.add(btnPrintAll, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 720, 130, 40));

        account3.setBackground(new java.awt.Color(255, 255, 255));
        account3.setFont(new java.awt.Font("Segoe UI Emoji", 1, 24)); // NOI18N
        account3.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(1).png")); // NOI18N
        account3.setText("Student Submission Review");
        cardSubmissionStudentForm.add(account3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 370, 50));

        jLabel10.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel10.setText("Student id :");
        cardSubmissionStudentForm.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 100, -1, 40));

        MainDesktop.add(cardSubmissionStudentForm, "card4");

        getContentPane().add(MainDesktop, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 70, 1250, 790));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel4MouseClicked

    private void SubmittedStudentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SubmittedStudentsMouseClicked
        // TODO add your handling code here:

    }//GEN-LAST:event_SubmittedStudentsMouseClicked

    private void ApprovedStudentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ApprovedStudentsMouseClicked
        // TODO add your handling code here:

    }//GEN-LAST:event_ApprovedStudentsMouseClicked

    private void btnSubmitRequest3MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest3MouseEntered
        // TODO add your handling code here:
        btnSubmitRequest3.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnSubmitRequest3MouseEntered

    private void btnSubmitRequest3MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest3MouseExited
        // TODO add your handling code here:
        btnSubmitRequest3.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnSubmitRequest3MouseExited

    private void btnSubmitRequest3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitRequest3ActionPerformed
        // TODO add your handling code here:

        new AdminDashBoardForm().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSubmitRequest3ActionPerformed

    private void txtSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearch1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearch1ActionPerformed

    private void btnSearch1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearch1MouseEntered
        // TODO add your handling code here:
       btnSearch1.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSearch1MouseEntered

    private void btnSearch1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearch1MouseExited
        // TODO add your handling code here:
        btnSearch1.setBackground(new Color(255, 255, 255));
        
    }//GEN-LAST:event_btnSearch1MouseExited

    private void btnSearch1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearch1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearch1ActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_txtSearchActionPerformed

    private void btnSearchMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseEntered
        // TODO add your handling code here:
        btnSearch.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSearchMouseEntered

    private void btnSearchMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSearchMouseExited
        // TODO add your handling code here:
        btnSearch.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnSearchMouseExited

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnSubmitRequest7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest7MouseClicked
        // TODO add your handling code here:
        new AdminDashBoardForm().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSubmitRequest7MouseClicked

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

    private void btnViewApprovedRequirementsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnViewApprovedRequirementsMouseClicked
        // TODO add your handling code here:

    }//GEN-LAST:event_btnViewApprovedRequirementsMouseClicked

    private void btnViewApprovedRequirementsMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnViewApprovedRequirementsMouseEntered
        // TODO add your handling code here:
        btnViewApprovedRequirements.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnViewApprovedRequirementsMouseEntered

    private void btnViewApprovedRequirementsMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnViewApprovedRequirementsMouseExited
        // TODO add your handling code here:
        btnViewApprovedRequirements.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnViewApprovedRequirementsMouseExited

    private void btnViewApprovedRequirementsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewApprovedRequirementsActionPerformed
        // TODO add your handling code here:
       

    }//GEN-LAST:event_btnViewApprovedRequirementsActionPerformed

    private void btnPrintApprovedListMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnPrintApprovedListMouseEntered
        // TODO add your handling code here:
        btnPrintApprovedList.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnPrintApprovedListMouseEntered

    private void btnPrintApprovedListMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnPrintApprovedListMouseExited
        // TODO add your handling code here:
        btnPrintApprovedList.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnPrintApprovedListMouseExited

    private void btnPrintApprovedListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrintApprovedListActionPerformed
        

    }//GEN-LAST:event_btnPrintApprovedListActionPerformed

    private void btnViewSubmissionMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnViewSubmissionMouseEntered
        // TODO add your handling code here:
         btnViewSubmission.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnViewSubmissionMouseEntered

    private void btnViewSubmissionMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnViewSubmissionMouseExited
        // TODO add your handling code here:
             btnViewSubmission.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnViewSubmissionMouseExited

    private void btnViewSubmissionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewSubmissionActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnViewSubmissionActionPerformed

    private void txtEventIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEventIDActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventIDActionPerformed

    private void btnApproveSubmissionMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveSubmissionMouseEntered
        // TODO add your handling code here:
         btnApproveSubmission.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnApproveSubmissionMouseEntered

    private void btnPrintAllMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnPrintAllMouseEntered
        // TODO add your handling code here:
         btnPrintAll.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnPrintAllMouseEntered

    private void btnBackMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBackMouseEntered
        // TODO add your handling code here:
          btnBack.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnBackMouseEntered

    private void btnBackMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBackMouseExited
        // TODO add your handling code here:
         btnBack.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnBackMouseExited

    private void btnPrintAllMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnPrintAllMouseExited
        // TODO add your handling code here:
        btnPrintAll.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnPrintAllMouseExited

    private void btnRejectSubmissionMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectSubmissionMouseExited
        // TODO add your handling code here:
         btnRejectSubmission.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnRejectSubmissionMouseExited

    private void btnApproveSubmissionMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnApproveSubmissionMouseExited
        // TODO add your handling code here:
         btnApproveSubmission.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnApproveSubmissionMouseExited

    private void btnRejectSubmissionMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnRejectSubmissionMouseEntered
        // TODO add your handling code here:
         btnRejectSubmission.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnRejectSubmissionMouseEntered

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
            java.util.logging.Logger.getLogger(StudentSubmissionForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StudentSubmissionForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StudentSubmissionForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StudentSubmissionForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new StudentSubmissionForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ApprovedStudents;
    private javax.swing.JPanel MainDesktop;
    private javax.swing.JPanel SidePanel;
    private javax.swing.JPanel SubmittedStudents;
    private javax.swing.JLabel account;
    private javax.swing.JLabel account1;
    private javax.swing.JLabel account2;
    private javax.swing.JLabel account3;
    private javax.swing.JButton btnApproveSubmission;
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnPrintAll;
    private javax.swing.JButton btnPrintApprovedList;
    private javax.swing.JButton btnRejectSubmission;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSearch1;
    private javax.swing.JButton btnSubmitRequest3;
    private javax.swing.JButton btnSubmitRequest7;
    private javax.swing.JButton btnViewApprovedRequirements;
    private javax.swing.JButton btnViewSubmission;
    private javax.swing.JPanel cardApprovedStudentsPanel;
    private javax.swing.JPanel cardSubmissionStudentForm;
    private javax.swing.JPanel cardSubmittedStudentsPanel;
    private javax.swing.JComboBox<String> comboCourseFilter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel password;
    private javax.swing.JPanel requirementsPanel;
    private javax.swing.JTable tblApprovedStudents;
    private javax.swing.JTable tblSubmittedStudents;
    private javax.swing.JTextField txtEventID;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextField txtSearch1;
    private javax.swing.JTextField txtStudentID;
    // End of variables declaration//GEN-END:variables


private List<String> getCoursesWithApprovedStudents() {
    List<String> courses = new ArrayList<>();
    try (Connection con = getConnection();
         PreparedStatement pst = con.prepareStatement(
             "SELECT DISTINCT e.course FROM submissions s JOIN events e ON s.event_id = e.id WHERE s.status = 'approved'"
         );
         ResultSet rs = pst.executeQuery()) {

        while (rs.next()) {
            courses.add(rs.getString("course"));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return courses;
}

private List<String> getApprovedEventNames() {
    List<String> events = new ArrayList<>();
    try (Connection con = getConnection();
         PreparedStatement pst = con.prepareStatement(
             "SELECT DISTINCT e.event_name FROM submissions s JOIN events e ON s.event_id = e.id WHERE s.status = 'approved'"
         );
         ResultSet rs = pst.executeQuery()) {

        while (rs.next()) {
            events.add(rs.getString("event_name"));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return events;
}


private List<String> getApprovedStudentIds() {
    List<String> studentIds = new ArrayList<>();
    try (Connection con = getConnection();
         PreparedStatement pst = con.prepareStatement(
             "SELECT DISTINCT s.student_id FROM submissions s WHERE s.status = 'approved'"
         );
         ResultSet rs = pst.executeQuery()) {

        while (rs.next()) {
            studentIds.add(rs.getString("student_id"));
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return studentIds;
}

private void styleTableHeader0() {
    tblSubmittedStudents.getTableHeader().setDefaultRenderer(new StudentSubmissionForm.HeaderColor());
    tblSubmittedStudents.setRowHeight(45);

    JTableHeader header = tblSubmittedStudents.getTableHeader();
    header.setPreferredSize(new Dimension(header.getWidth(), 50));
    header.setFont(new Font("Segoe UI", Font.BOLD, 14));
    header.setBackground(new Color(32, 136, 203));
    header.setForeground(Color.WHITE);

    applyLeftAlignToCells(tblSubmittedStudents); }

private void styleTableHeader01() {
    tblApprovedStudents.getTableHeader().setDefaultRenderer(new StudentSubmissionForm.HeaderColor());
    tblApprovedStudents.setRowHeight(45);

    JTableHeader header = tblApprovedStudents.getTableHeader();
    header.setPreferredSize(new Dimension(header.getWidth(), 50));
    header.setFont(new Font("Segoe UI", Font.BOLD, 14));
    header.setBackground(new Color(32, 136, 203));
    header.setForeground(Color.WHITE);

    applyLeftAlignToCells(tblApprovedStudents); 
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
class HeaderColor extends DefaultTableCellRenderer {
    private final Color backgroundColor = new Color(32, 136, 203);  

    public HeaderColor() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setBackground(backgroundColor);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 14));

        return this;
    }
}


private void applyLeftAlignToCells(JTable table) {
    TableCellRenderer leftRenderer = new CellRendererLeftAlign();
    for (int i = 0; i < table.getColumnCount(); i++) {
        table.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
    }
}


private void loadSubmittedStudents(String selectedCourse, String searchText) {
    DefaultTableModel model = (DefaultTableModel) tblSubmittedStudents.getModel();
    model.setRowCount(0);


    for (MouseListener ml : tblSubmittedStudents.getMouseListeners()) {
        tblSubmittedStudents.removeMouseListener(ml);
    }

    tblSubmittedStudents.addMouseListener(new MouseAdapter() {
        private int lastClickedRow = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            int clickedRow = tblSubmittedStudents.rowAtPoint(e.getPoint());
            if (clickedRow == -1) return;

            if (clickedRow == lastClickedRow) {
                tblSubmittedStudents.clearSelection();
                lastClickedRow = -1;
            } else {
                tblSubmittedStudents.setRowSelectionInterval(clickedRow, clickedRow);
                lastClickedRow = clickedRow;
            }
        }
    });

    StringBuilder query = new StringBuilder(
        "SELECT " +
        "s.student_id, " +
        "u.full_name AS student_name, " +
        "e.id AS event_id, " +
        "e.event_name, " +
        "e.course, " +
        "MAX(s.timestamp) AS submission_time, " +
        "s.status " +
        "FROM submissions s " +
        "JOIN users u ON s.student_id = u.student_id " +
        "JOIN events e ON s.event_id = e.id " +
        "WHERE s.status != 'approved' "
    );

    List<String> conditions = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    if (selectedCourse != null && !selectedCourse.equals("All")) {
        conditions.add("e.course = ?");
        params.add(selectedCourse);
    }

    if (searchText != null && !searchText.trim().isEmpty()) {
        String likeClause = "%" + searchText.trim() + "%";
        conditions.add("(" +
            "s.student_id LIKE ? OR " +
            "u.full_name LIKE ? OR " +
            "e.event_name LIKE ? OR " +
            "e.course LIKE ? OR " +
            "s.status LIKE ?" +
        ")");
        for (int i = 0; i < 5; i++) {
            params.add(likeClause);
        }
    }

    if (!conditions.isEmpty()) {
        query.append(" AND ").append(String.join(" AND ", conditions));
    }

    query.append(" GROUP BY s.student_id, s.event_id, u.full_name, e.id, e.event_name, e.course, s.status ");
    query.append(" ORDER BY submission_time DESC");

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query.toString())) {

        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("student_id"),
                    rs.getString("student_name"),
                    rs.getInt("event_id"),
                    rs.getString("event_name"),
                    rs.getString("course"),
                    rs.getString("submission_time"),
                    rs.getString("status")
                });
            }
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load submitted students: " + ex.getMessage());
    }
}


private void loadApprovedStudents(String searchText) {
    DefaultTableModel model = (DefaultTableModel) tblApprovedStudents.getModel();
    model.setRowCount(0);// Clear the table

    tblApprovedStudents.addMouseListener(new MouseAdapter() {
        private int lastClickedRow = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            int clickedRow = tblApprovedStudents.rowAtPoint(e.getPoint());
            if (clickedRow == -1) return;
            if (clickedRow == lastClickedRow) {
                tblApprovedStudents.clearSelection();
                lastClickedRow = -1;
            } else {
                tblApprovedStudents.setRowSelectionInterval(clickedRow, clickedRow);
                lastClickedRow = clickedRow;
            }
        }
    });

    String query = "SELECT " +
                   "s.student_id, " +
                   "u.full_name AS student_name, " +
                   "e.id AS event_id, " +
                   "e.event_name, " +
                   "e.course, " +
                   "MAX(s.timestamp) AS submission_time, " +
                   "MAX(s.status) AS status " +
                   "FROM submissions s " +
                   "JOIN users u ON s.student_id = u.student_id " +
                   "JOIN events e ON s.event_id = e.id " +
                   "WHERE s.status = 'approved' ";

    if (searchText != null && !searchText.trim().isEmpty()) {
        query += "AND (s.student_id LIKE ? OR u.full_name LIKE ? OR e.event_name LIKE ? OR e.course LIKE ?) ";
    }

    query += "GROUP BY s.student_id, s.event_id, u.full_name, e.id, e.event_name, e.course " +
             "ORDER BY submission_time DESC";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        if (searchText != null && !searchText.trim().isEmpty()) {
            String keyword = "%" + searchText.trim() + "%";
            ps.setString(1, keyword);
            ps.setString(2, keyword);
            ps.setString(3, keyword);
            ps.setString(4, keyword);
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String studentId = rs.getString("student_id");
            String studentName = rs.getString("student_name");
            int eventId = rs.getInt("event_id");
            String eventName = rs.getString("event_name");
            String course = rs.getString("course");
            String submissionTime = rs.getString("submission_time");
            String status = rs.getString("status");

            model.addRow(new Object[]{
                studentId, studentName, eventId, eventName, course, submissionTime, status
            });
        }


        TableColumn eventIdColumn = tblApprovedStudents.getColumnModel().getColumn(2);
        eventIdColumn.setMinWidth(0);
        eventIdColumn.setMaxWidth(0);
        eventIdColumn.setPreferredWidth(0);
        eventIdColumn.setWidth(0);

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load approved students: " + ex.getMessage());
    }
}


private Map<String, JTextField> fileFields = new HashMap<>();
private Map<String, JTextArea> notesFields = new HashMap<>();

private void loadStudentSubmissionForReview(String studentId, int eventId) {
    txtStudentID.setText(studentId);
    txtEventID.setText(String.valueOf(eventId));

    requirementsPanel.removeAll(); 
    fileFields.clear();
    notesFields.clear();

    try (Connection con = getConnection()) {
        PreparedStatement reqStmt = con.prepareStatement(
            "SELECT requirement_name FROM event_requirements WHERE event_id = ?"
        );
        reqStmt.setInt(1, eventId);
        ResultSet reqRs = reqStmt.executeQuery();

        while (reqRs.next()) {
            String reqName = reqRs.getString("requirement_name");

            PreparedStatement subStmt = con.prepareStatement(
                "SELECT file_path, admin_notes FROM submissions WHERE student_id = ? AND event_id = ? AND requirement_text = ?"
            );
            subStmt.setString(1, studentId);
            subStmt.setInt(2, eventId);
            subStmt.setString(3, reqName);
            ResultSet subRs = subStmt.executeQuery();

            String filePath = "";
            String adminNotes = "";
            if (subRs.next()) {
                filePath = subRs.getString("file_path");
                adminNotes = subRs.getString("admin_notes") != null ? subRs.getString("admin_notes") : "";
            }


            JPanel reqPanel = new JPanel(new BorderLayout(5, 5));
            reqPanel.setBorder(BorderFactory.createTitledBorder(reqName));

            JTextField filePathField = new JTextField(filePath);
            filePathField.setEditable(false);

           JButton viewFileBtn = new JButton("View");
JButton printBtn = new JButton("Print");

String finalFilePath = filePath;
String finalReqName = reqName;

viewFileBtn.addActionListener(e -> {
    if (finalFilePath == null || finalFilePath.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "No file submitted for this requirement.");
        return;
    }

    File file = new File(finalFilePath);
    if (!file.exists()) {
        JOptionPane.showMessageDialog(null, "File does not exist: " + finalFilePath);
        return;
    }

    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
        } else {
            JOptionPane.showMessageDialog(null, "Desktop not supported. Cannot open file.");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Unable to open file:\n" + ex.getMessage());
    }
});

printBtn.addActionListener(e -> {
    if (finalFilePath == null || finalFilePath.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "No file submitted for this requirement.");
        return;
    }

    File file = new File(finalFilePath);
    if (!file.exists()) {
        JOptionPane.showMessageDialog(null, "File does not exist: " + finalFilePath);
        return;
    }

    try {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.PRINT)) {
                desktop.print(file);
            } else {
                JOptionPane.showMessageDialog(null, "Print not supported on this system.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Desktop not supported.");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to print file: " + ex.getMessage());
    }
});



            JTextArea notesArea = new JTextArea(adminNotes, 3, 40);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);

            fileFields.put(reqName, filePathField);
            notesFields.put(reqName, notesArea);

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
buttonsPanel.add(viewFileBtn);
buttonsPanel.add(printBtn); 

JPanel topPanel = new JPanel(new BorderLayout(5, 0));
topPanel.add(filePathField, BorderLayout.CENTER);
topPanel.add(buttonsPanel, BorderLayout.EAST);

reqPanel.add(topPanel, BorderLayout.NORTH);
            reqPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);

            requirementsPanel.add(reqPanel);
        }

        requirementsPanel.revalidate();
        requirementsPanel.repaint();

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to load submission details:\n" + e.getMessage());
    }
}

private void rejectSubmissionWithNotes() {
    String studentId = txtStudentID.getText().trim();
    int eventId = Integer.parseInt(txtEventID.getText().trim());

    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to reject this submission and send notes?",
        "Confirm Rejection", JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) return;

    try (Connection con = getConnection()) {
        for (String reqName : notesFields.keySet()) {
            JTextArea notesArea = notesFields.get(reqName);
            String note = notesArea.getText().trim();

            PreparedStatement ps = con.prepareStatement(
                "UPDATE submissions SET status = 'rejected', admin_notes = ? " +
                "WHERE student_id = ? AND event_id = ? AND requirement_text = ?"
            );
            ps.setString(1, note);
            ps.setString(2, studentId);
            ps.setInt(3, eventId);
            ps.setString(4, reqName);
            ps.executeUpdate();
        }

        JOptionPane.showMessageDialog(this, "Submission rejected and notes saved.");
       populateCourseFilterCombo();
loadSubmittedStudents("All", txtSearch1.getText().trim());


        CardLayout cardLayout = (CardLayout) MainDesktop.getLayout();
cardLayout.show(MainDesktop, "cardSubmittedStudentsPanel");


    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to reject submission:\n" + e.getMessage());
    }
}

private void printRequirement(String studentId, int eventId, String requirementName) {
    try (Connection con = getConnection()) {
        PreparedStatement ps = con.prepareStatement(
            "SELECT file_path, admin_notes, status FROM submissions " +
            "WHERE student_id = ? AND event_id = ? AND requirement_text = ?"
        );
        ps.setString(1, studentId);
        ps.setInt(2, eventId);
        ps.setString(3, requirementName);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String filePath = rs.getString("file_path");
            String notes = rs.getString("admin_notes") != null ? rs.getString("admin_notes") : "";
            String status = rs.getString("status");

            StringBuilder content = new StringBuilder();
            content.append("Requirement: ").append(requirementName).append("\n");
            content.append("File: ").append(filePath).append("\n");
            content.append("Status: ").append(status).append("\n");
            content.append("Admin Notes:\n").append(notes).append("\n");

            JTextArea textArea = new JTextArea(content.toString());
            boolean done = textArea.print();

            if (done) {
                JOptionPane.showMessageDialog(null, "Print successful.");
            } else {
                JOptionPane.showMessageDialog(null, "Print canceled.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "No submission data found for this requirement.");
        }
    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error printing requirement: " + e.getMessage());
    }
}

private void printApprovedList(String filterColumn, String filterValue) {
    StringBuilder content = new StringBuilder();
    content.append("Approved Students List\n");
    content.append("=======================\n\n");

    String query = "SELECT s.student_id, u.full_name, e.event_name, e.course, MAX(s.timestamp) AS timestamp " +
                   "FROM submissions s " +
                   "JOIN users u ON s.student_id = u.student_id " +
                   "JOIN events e ON s.event_id = e.id " +
                   "WHERE s.status = 'approved'";

    if (filterColumn != null) {
        query += " AND " + filterColumn + " = ?";
    }

    query += " GROUP BY s.student_id, e.event_name, e.course, u.full_name ORDER BY e.event_name";

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        if (filterColumn != null) {
            ps.setString(1, filterValue);
        }

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String studentId = rs.getString("student_id");
            String name = rs.getString("full_name");
            String event = rs.getString("event_name");
            String course = rs.getString("course");
            String time = rs.getString("timestamp");

            content.append("Event: ").append(event).append("\n");
            content.append("Course: ").append(course).append("\n");
            content.append("Student ID: ").append(studentId).append("\n");
            content.append("Name: ").append(name).append("\n");
            content.append("Approved on: ").append(time).append("\n");
            content.append("-------------------------------\n\n");
        }

        // Print
        File tempFile = File.createTempFile("approved_list", ".txt");
        Files.write(tempFile.toPath(), content.toString().getBytes());

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().print(tempFile);
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to print approved list: " + ex.getMessage());
    }
}


private JComboBox<String> courseComboBox;
private JButton btnOk;

private void initializeUI() {
    courseComboBox = new JComboBox<>();
    btnOk = new JButton("OK");

    loadCourses();
}

private void loadCourses() {
    List<String> courses = getCoursesWithApprovedStudents();

    if (courses == null || courses.isEmpty()) {
        JOptionPane.showMessageDialog(null, "⚠ No approved students found for any course.");
        courseComboBox.setModel(new DefaultComboBoxModel<>());
        btnOk.setEnabled(false);
    } else {
        courseComboBox.setModel(new DefaultComboBoxModel<>(courses.toArray(new String[0])));
        btnOk.setEnabled(true);
    }
}


public class Student {
    private String studentId;
    private String fullName;

    public Student(String studentId, String fullName) {
        this.studentId = studentId;
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return fullName + " (" + studentId + ")";
    }
}


private List<Student> getApprovedStudentsByCourse(String course) {
    List<Student> students = new ArrayList<>();
    String query = """
        SELECT DISTINCT u.student_id, u.full_name
        FROM users u
        JOIN submissions s ON u.student_id = s.student_id
        JOIN events e ON s.event_id = e.id
        WHERE s.status = 'approved' AND e.course = ?
        ORDER BY u.full_name
    """;

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {

        ps.setString(1, course);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String fullName = rs.getString("full_name");
                students.add(new Student(studentId, fullName));
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "❌ Failed to load students: " + e.getMessage());
    }

    return students;
}

    private boolean eventExists(String eventName) {
    String query = "SELECT 1 FROM events WHERE event_name = ?";
    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {
        ps.setString(1, eventName);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    private boolean approvedStudentExists(String studentId) {
    String query = "SELECT 1 FROM submissions WHERE student_id = ? AND status = 'approved'";
    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(query)) {
        ps.setString(1, studentId);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

    private void populateCourseFilterCombo() {
    comboCourseFilter.removeAllItems();
    comboCourseFilter.addItem("All");

    try (Connection con = getConnection();
         PreparedStatement ps = con.prepareStatement(
             "SELECT DISTINCT e.course FROM submissions s JOIN events e ON s.event_id = e.id WHERE s.status != 'approved'"
         );
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            comboCourseFilter.addItem(rs.getString("course"));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

}

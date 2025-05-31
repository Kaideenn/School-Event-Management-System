

import db.SessionManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.*;
import java.sql.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.filechooser.FileNameExtensionFilter;

public class StudentDashBoardForm extends javax.swing.JFrame {

    private int selectedEventId = -1;
    private CardLayout cardLayout;
    private String currentCourse;
    private String studentID;
    
    private Map<String, String> uploadedFiles = new HashMap<>();
    private Map<String, String> adminNotesMap = new HashMap<>();


    public StudentDashBoardForm() {
        if (!SessionManager.isStudentLoggedIn || SessionManager.loggedInStudentId == null) {
            JOptionPane.showMessageDialog(null, "Access denied. Please log in as a student.");
            new RoleSelectionForm().setVisible(true); 
            dispose();
            return;
        }

        initComponents();
        this.setLocationRelativeTo(null);
        styleTableHeader123();

        requirementListPanel.setLayout(new GridLayout(0, 2, 10, 10)); 

      
        tblStudentDashboard.getColumnModel().getColumn(0).setMinWidth(0);
        tblStudentDashboard.getColumnModel().getColumn(0).setMaxWidth(0);
        tblStudentDashboard.getColumnModel().getColumn(0).setWidth(0);

        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        mainPanel.add(cardStudentDashBoardForm, "cardStudentDashBoardForm");
        mainPanel.add(cardSubmitRequirementForm, "cardSubmitRequirementForm");
        cardLayout.show(mainPanel, "cardStudentDashBoardForm");

        btnBackToDashboard.addActionListener(e -> {
    comboFilter.setSelectedIndex(0); 
    txtSearch.setText("");          

   
    loadStudentEvents(currentCourse, "All", "");

    cardLayout.show(mainPanel, "cardStudentDashBoardForm");
});


        btnGoToSubmit.addActionListener(e -> {
    int selectedRow = tblStudentDashboard.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Please select an event to submit requirements.");
        return;
    }

    lblEventTitle.setText(tblStudentDashboard.getValueAt(selectedRow, 1).toString());
    selectedEventId = Integer.parseInt(tblStudentDashboard.getValueAt(selectedRow, 0).toString());
   
    uploadedFiles.clear(); 
    
    loadRequirementsForStudent(selectedEventId); 

    loadExistingSubmission();  

    cardLayout.show(mainPanel, "cardSubmitRequirementForm");
    
      
});

    }

    public void setStudentDetails(String course, String studentID) {
        this.currentCourse = course;
        this.studentID = studentID;

      SwingUtilities.invokeLater(() -> loadStudentEvents(currentCourse, "All", ""));


       btnSubmitRequirement.addActionListener(e -> {
   
    if (uploadedFiles.size() < requirementListPanel.getComponentCount() / 2) {
        JOptionPane.showMessageDialog(null, "Please upload a file for each requirement.");
        return;
    }

    try (Connection con = getConnection()) {
       
        PreparedStatement deadlineStmt = con.prepareStatement("SELECT deadline FROM events WHERE id = ?");
        deadlineStmt.setInt(1, selectedEventId);
        ResultSet deadlineRs = deadlineStmt.executeQuery();

        if (deadlineRs.next()) {
            String deadlineStr = deadlineRs.getString("deadline");
            Date deadlineDate;
            try {
                deadlineDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(deadlineStr);
            } catch (java.text.ParseException e1) {
                deadlineDate = new SimpleDateFormat("yyyy-MM-dd").parse(deadlineStr);
            }

            if (new Date().after(deadlineDate)) {
                JOptionPane.showMessageDialog(null, "You cannot submit. The deadline has passed.");
                return;
            }
        }

      
        PreparedStatement checkStmt = con.prepareStatement("SELECT * FROM submissions WHERE student_id = ? AND event_id = ?");
        checkStmt.setString(1, studentID);
        checkStmt.setInt(2, selectedEventId);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            String status = rs.getString("status");
            if ("pending".equalsIgnoreCase(status) || "approved".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(null, "You cannot resubmit unless your previous submission was rejected.");
                return;
            }

            
            PreparedStatement deleteStmt = con.prepareStatement("DELETE FROM submissions WHERE student_id = ? AND event_id = ?");
            deleteStmt.setString(1, studentID);
            deleteStmt.setInt(2, selectedEventId);
            deleteStmt.executeUpdate();

            for (Map.Entry<String, String> entry : uploadedFiles.entrySet()) {
                PreparedStatement insertStmt = con.prepareStatement(
                    "INSERT INTO submissions (student_id, event_id, requirement_text, file_path, status) VALUES (?, ?, ?, ?, 'pending')");
                insertStmt.setString(1, studentID);
                insertStmt.setInt(2, selectedEventId);
                insertStmt.setString(3, entry.getKey());  
                insertStmt.setString(4, entry.getValue());
                insertStmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(null, "Requirements resubmitted successfully.");

        } else {
            
            for (Map.Entry<String, String> entry : uploadedFiles.entrySet()) {
                PreparedStatement insertStmt = con.prepareStatement(
                    "INSERT INTO submissions (student_id, event_id, requirement_text, file_path, status) VALUES (?, ?, ?, ?, 'pending')");
                insertStmt.setString(1, studentID);
                insertStmt.setInt(2, selectedEventId);
                insertStmt.setString(3, entry.getKey());
                insertStmt.setString(4, entry.getValue());
                insertStmt.executeUpdate();
            }
            JOptionPane.showMessageDialog(null, "Requirements submitted successfully.");
        }
   comboFilter.setSelectedIndex(0); 
           txtSearch.setText("");
         
      
        uploadedFiles.clear();
        loadExistingSubmission();
          loadStudentEvents(currentCourse, "All", "");
        

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
    }
});
       btnLogout.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to log out?",
        "Logout Confirmation",
        JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
      
        SessionManager.isStudentLoggedIn = false;
        SessionManager.loggedInStudentId = null;

        
        new RoleSelectionForm().setVisible(true);
        dispose(); 
    }
});

comboFilter.addActionListener(e -> {
    String selectedFilter = (String) comboFilter.getSelectedItem();
    loadStudentEvents(currentCourse, selectedFilter, txtSearch.getText());
});

btnSearch.addActionListener(e -> {
    String selectedFilter = (String) comboFilter.getSelectedItem();
    String searchText = txtSearch.getText();
    loadStudentEvents(currentCourse, selectedFilter, searchText);
});


    }
    private void loadRequirementsForStudent(int eventId) {
        requirementListPanel.removeAll();
        uploadedFiles.clear();
        requirementListPanel.setLayout(new GridLayout(0, 2, 10, 10));

        try (Connection con = getConnection()) {
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM event_requirements WHERE event_id = ?");
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String requirementName = rs.getString("requirement_name");

                JLabel reqLabel = new JLabel("• " + requirementName);
reqLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
reqLabel.setForeground(new Color(60, 60, 60));


                JPanel filePanel = new JPanel(new BorderLayout(5, 0));
                JTextField filePathField = new JTextField();
                filePathField.setEditable(false);

                JButton uploadBtn = new JButton("Upload File");
                uploadBtn.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Documents and Images", "pdf", "docx", "jpg", "png"));

                    int result = fileChooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        String filePath = selectedFile.getAbsolutePath();
                        filePathField.setText(filePath);
                        uploadedFiles.put(requirementName, filePath); 
                    }
                });

                filePanel.add(filePathField, BorderLayout.CENTER);
                filePanel.add(uploadBtn, BorderLayout.EAST);

                requirementListPanel.add(reqLabel);
                requirementListPanel.add(filePanel);
            }

            requirementListPanel.revalidate();
            requirementListPanel.repaint();
            
            
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  private void loadExistingSubmission() {
    try (Connection con = getConnection()) {
        PreparedStatement pst = con.prepareStatement(
            "SELECT * FROM submissions WHERE student_id = ? AND event_id = ?");
        pst.setString(1, studentID);
        pst.setInt(2, selectedEventId);
        ResultSet rs = pst.executeQuery();

        Map<String, String> existingFiles = new HashMap<>();

       String submissionStatus = null;
String adminNote = "";


while (rs.next()) {
    existingFiles.put(rs.getString("requirement_text"), rs.getString("file_path"));
    adminNote = rs.getString("admin_notes"); 
    submissionStatus = rs.getString("status");
}


boolean isRejected = "rejected".equalsIgnoreCase(submissionStatus);
boolean isApproved = "approved".equalsIgnoreCase(submissionStatus);
boolean isPending = "pending".equalsIgnoreCase(submissionStatus);


if (submissionStatus != null) {
    lblStatus.setText("Status: " + submissionStatus);

    if (isApproved) {
        lblStatus.setForeground(new java.awt.Color(102,255,102)); 
    } else if (isRejected) {
        lblStatus.setForeground(java.awt.Color.RED); 
    } else {
        lblStatus.setForeground(java.awt.Color.WHITE); 
    }
} else {
    lblStatus.setText("Status: No submission yet");
    lblStatus.setForeground(java.awt.Color.WHITE); 
}


requirementListPanel.removeAll();



        PreparedStatement reqStmt = con.prepareStatement(
            "SELECT requirement_name FROM event_requirements WHERE event_id = ?");
        reqStmt.setInt(1, selectedEventId);
        ResultSet reqRs = reqStmt.executeQuery();

        while (reqRs.next()) {
            String reqName = reqRs.getString("requirement_name");

            JLabel reqLabel = new JLabel("• " + reqName);
            reqLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            reqLabel.setForeground(new Color(60, 60, 60));

            JPanel filePanel = new JPanel(new BorderLayout(5, 0));
            JTextField filePathField = new JTextField();
            filePathField.setEditable(false);

            String existingFilePath = existingFiles.getOrDefault(reqName, "");
            filePathField.setText(existingFilePath);

            if (!existingFilePath.isEmpty()) {
                uploadedFiles.put(reqName, existingFilePath);
            }

            JButton uploadBtn = new JButton("Upload File");
            uploadBtn.setEnabled(isRejected || submissionStatus == null); 

            uploadBtn.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Documents and Images", "pdf", "docx", "jpg", "png"));
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();
                    filePathField.setText(filePath);
                    uploadedFiles.put(reqName, filePath);
                }
            });

            filePanel.add(filePathField, BorderLayout.CENTER);
            filePanel.add(uploadBtn, BorderLayout.EAST);

            requirementListPanel.add(reqLabel);
            requirementListPanel.add(filePanel);
        }

  
        if (isRejected && adminNote != null && !adminNote.trim().isEmpty()) {
    lblAdminNote.setText("<html><b>Admin Note:</b> " + adminNote + "</html>");
    lblAdminNote.setForeground(new Color(255, 255, 255));
    lblAdminNote.setFont(new Font("Segoe UI", Font.ITALIC, 16));
    lblAdminNote.setVisible(true);
} else {

    lblAdminNote.setText("");
    lblAdminNote.setVisible(false);
}


        requirementListPanel.revalidate();
        requirementListPanel.repaint();


        btnSubmitRequirement.setEnabled(submissionStatus == null || isRejected);


        if (isPending) {
            JOptionPane.showMessageDialog(null, "Your submission is pending approval. You cannot change your files now.");
        } else if (isApproved) {
            JOptionPane.showMessageDialog(null, "Your submission has been approved. Congrats!!.");
        } else if (!isRejected && submissionStatus != null) {
            JOptionPane.showMessageDialog(null, "You cannot edit this submission unless it was rejected.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    

}






   private void loadStudentEvents(String course, String filter, String searchText) {
    DefaultTableModel model = (DefaultTableModel) tblStudentDashboard.getModel();
    model.setRowCount(0);

    String baseQuery = "SELECT * FROM events WHERE course = ?";
    String orderBy = "";
    String searchClause = "";
    
     tblStudentDashboard.addMouseListener(new MouseAdapter() {
    private int lastClickedRow = -1;

    @Override
    public void mousePressed(MouseEvent e) {
        int clickedRow = tblStudentDashboard.rowAtPoint(e.getPoint());
        if (clickedRow == -1) return; 

        if (clickedRow == lastClickedRow) {
 
            tblStudentDashboard.clearSelection();
            lastClickedRow = -1;
        } else {

            tblStudentDashboard.setRowSelectionInterval(clickedRow, clickedRow);
            lastClickedRow = clickedRow;
        }
    }
});


    
    if (filter != null) {
        switch (filter) {
            case "A-Z":
                orderBy = " ORDER BY event_name ASC";
                break;
            case "Z-A":
                orderBy = " ORDER BY event_name DESC";
                break;
            case "Date":
                orderBy = " ORDER BY date ASC";
                break;
            default:
                orderBy = ""; // no sorting or default sorting
        }
    }

   
    if (searchText != null && !searchText.trim().isEmpty()) {
        searchClause = " AND (event_name LIKE ? OR description LIKE ? OR date LIKE ?  OR deadline LIKE ?)";
    }

    try (Connection con = getConnection()) {
        PreparedStatement stmt = con.prepareStatement(baseQuery + searchClause + orderBy);
        stmt.setString(1, course);

        if (!searchClause.isEmpty()) {
            
            for (int i = 2; i <= 5; i++) {
                stmt.setString(i, "%" + searchText + "%");
            }
        }

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("event_name"),
                rs.getString("date"),
                rs.getString("description"),
                rs.getString("deadline"),
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error loading events: " + e.getMessage());
    }
}


    private void styleTableHeader123() {
        tblStudentDashboard.getTableHeader().setDefaultRenderer(new HeaderColor());
        tblStudentDashboard.setRowHeight(45);

        JTableHeader header = tblStudentDashboard.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 50));
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(0, 153, 153));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tblStudentDashboard.getColumnCount(); i++) {
            tblStudentDashboard.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
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
            setHorizontalAlignment(JLabel.CENTER);
            return this;
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
        mainPanel = new javax.swing.JPanel();
        cardStudentDashBoardForm = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblStudentDashboard = new javax.swing.JTable();
        btnGoToSubmit = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        btnLogout = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        comboFilter = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        cardSubmitRequirementForm = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        lblEventTitle = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        btnSubmitRequirement = new javax.swing.JButton();
        btnBackToDashboard = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        requirementListPanel = new javax.swing.JPanel();
        lblAdminNote = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(1499, 854));
        setMinimumSize(new java.awt.Dimension(1499, 854));
        setUndecorated(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(105, 105, 255));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel4.setBackground(new java.awt.Color(105, 105, 255));
        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 25)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("X");
        jLabel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel4MouseClicked(evt);
            }
        });
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1448, 23, 20, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\menu(2).png")); // NOI18N
        jPanel2.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(21, 13, -1, 53));

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 102, 51));
        jLabel3.setText("MANAGEMENT SYSTEM");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(232, 20, 240, -1));

        jLabel13.setBackground(new java.awt.Color(255, 255, 255));
        jLabel13.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("SCHOOL EVENT");
        jPanel2.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(85, 20, -1, -1));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1500, 70));

        mainPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainPanel.setMaximumSize(new java.awt.Dimension(1499, 854));
        mainPanel.setMinimumSize(new java.awt.Dimension(1499, 854));
        mainPanel.setPreferredSize(new java.awt.Dimension(1499, 854));
        mainPanel.setLayout(new java.awt.CardLayout());

        cardStudentDashBoardForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel6.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        cardStudentDashBoardForm.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(1494, 50, -1, -1));

        tblStudentDashboard.setModel(new javax.swing.table.DefaultTableModel(
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
                "ID", "Event Name", "Date", "Description", "Deadline"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblStudentDashboard.setFocusable(false);
        tblStudentDashboard.setRowHeight(25);
        tblStudentDashboard.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblStudentDashboard.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblStudentDashboard.setShowGrid(false);
        tblStudentDashboard.setShowHorizontalLines(true);
        tblStudentDashboard.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblStudentDashboard);

        cardStudentDashBoardForm.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 170, 1370, 550));

        btnGoToSubmit.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnGoToSubmit.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\arrow.png")); // NOI18N
        btnGoToSubmit.setText("Submit requirements");
        btnGoToSubmit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnGoToSubmitMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnGoToSubmitMouseExited(evt);
            }
        });
        btnGoToSubmit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGoToSubmitActionPerformed(evt);
            }
        });
        cardStudentDashBoardForm.add(btnGoToSubmit, new org.netbeans.lib.awtextra.AbsoluteConstraints(1230, 730, -1, 40));

        jLabel8.setBackground(new java.awt.Color(102, 102, 255));
        jLabel8.setFont(new java.awt.Font("STZhongsong", 1, 40)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 102, 51));
        jLabel8.setText("STUDENT");
        cardStudentDashBoardForm.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 20, 220, 50));

        jLabel15.setBackground(new java.awt.Color(255, 255, 255));
        jLabel15.setFont(new java.awt.Font("STZhongsong", 1, 40)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(102, 102, 255));
        jLabel15.setText("DASHBOARD");
        cardStudentDashBoardForm.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 20, 290, 50));

        btnLogout.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnLogout.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\logout.png")); // NOI18N
        btnLogout.setText("Logout");
        btnLogout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnLogoutMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnLogoutMouseExited(evt);
            }
        });
        cardStudentDashBoardForm.add(btnLogout, new org.netbeans.lib.awtextra.AbsoluteConstraints(1300, 110, 120, 40));

        jLabel9.setBackground(new java.awt.Color(0, 0, 0));
        jLabel9.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel9.setText("FIlter :");
        cardStudentDashBoardForm.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 110, 60, 40));

        comboFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "A-Z", "Z-A", "Date" }));
        cardStudentDashBoardForm.add(comboFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 110, 210, 40));

        jLabel10.setBackground(new java.awt.Color(0, 0, 0));
        jLabel10.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel10.setText("Search :");
        cardStudentDashBoardForm.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 110, 90, 40));

        txtSearch.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        cardStudentDashBoardForm.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 110, 230, 40));

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
        cardStudentDashBoardForm.add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 110, -1, 40));

        mainPanel.add(cardStudentDashBoardForm, "card2");

        cardSubmitRequirementForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel16.setBackground(new java.awt.Color(255, 255, 255));
        jLabel16.setFont(new java.awt.Font("STZhongsong", 1, 36)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(102, 102, 255));
        jLabel16.setText("Submit Request Form");
        cardSubmitRequirementForm.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 30, 420, 50));

        jPanel7.setBackground(new java.awt.Color(102, 102, 255));
        jPanel7.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblEventTitle.setFont(new java.awt.Font("STXihei", 1, 24)); // NOI18N
        lblEventTitle.setForeground(new java.awt.Color(255, 255, 255));
        jPanel7.add(lblEventTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 20, 770, 40));

        jLabel1.setFont(new java.awt.Font("Segoe UI Emoji", 1, 21)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Requirements description:");
        jPanel7.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 170, -1, -1));

        jLabel5.setFont(new java.awt.Font("STXihei", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Event Title: ");
        jPanel7.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 20, 140, 40));

        lblStatus.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        lblStatus.setForeground(new java.awt.Color(255, 255, 255));
        jPanel7.add(lblStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 20, 240, 50));

        btnSubmitRequirement.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSubmitRequirement.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\arrow.png")); // NOI18N
        btnSubmitRequirement.setText("Submit");
        btnSubmitRequirement.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSubmitRequirementMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSubmitRequirementMouseExited(evt);
            }
        });
        jPanel7.add(btnSubmitRequirement, new org.netbeans.lib.awtextra.AbsoluteConstraints(1060, 600, 120, 40));

        btnBackToDashboard.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnBackToDashboard.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\back-arrow.png")); // NOI18N
        btnBackToDashboard.setText("Back");
        btnBackToDashboard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnBackToDashboardMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnBackToDashboardMouseExited(evt);
            }
        });
        jPanel7.add(btnBackToDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 600, 110, 40));

        requirementListPanel.setLayout(new javax.swing.BoxLayout(requirementListPanel, javax.swing.BoxLayout.LINE_AXIS));
        jScrollPane3.setViewportView(requirementListPanel);

        jPanel7.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 210, 1100, 350));

        lblAdminNote.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        lblAdminNote.setForeground(new java.awt.Color(255, 255, 255));
        jPanel7.add(lblAdminNote, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 160, 500, 50));

        cardSubmitRequirementForm.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 100, 1270, 670));

        mainPanel.add(cardSubmitRequirementForm, "card3");

        getContentPane().add(mainPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 1500, 790));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel4MouseClicked

    private void btnGoToSubmitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGoToSubmitActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnGoToSubmitActionPerformed

    private void btnGoToSubmitMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnGoToSubmitMouseExited
        // TODO add your handling code here:
        btnGoToSubmit.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnGoToSubmitMouseExited

    private void btnGoToSubmitMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnGoToSubmitMouseEntered
        // TODO add your handling code here:
        btnGoToSubmit.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnGoToSubmitMouseEntered

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearchActionPerformed

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

    private void btnLogoutMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLogoutMouseEntered
        // TODO add your handling code here:
             btnLogout.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnLogoutMouseEntered

    private void btnLogoutMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnLogoutMouseExited
        // TODO add your handling code here:
         btnLogout.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnLogoutMouseExited

    private void btnBackToDashboardMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBackToDashboardMouseEntered
        // TODO add your handling code here:
        btnBackToDashboard.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnBackToDashboardMouseEntered

    private void btnBackToDashboardMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnBackToDashboardMouseExited
        // TODO add your handling code here:
        btnBackToDashboard.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnBackToDashboardMouseExited

    private void btnSubmitRequirementMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequirementMouseEntered
        // TODO add your handling code here:
        btnSubmitRequirement.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSubmitRequirementMouseEntered

    private void btnSubmitRequirementMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequirementMouseExited
        // TODO add your handling code here:
        btnSubmitRequirement.setBackground(new Color(255, 255, 255));
    }//GEN-LAST:event_btnSubmitRequirementMouseExited

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
            java.util.logging.Logger.getLogger(StudentDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StudentDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StudentDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StudentDashBoardForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
         java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new StudentDashBoardForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBackToDashboard;
    private javax.swing.JButton btnGoToSubmit;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSubmitRequirement;
    private javax.swing.JPanel cardStudentDashBoardForm;
    private javax.swing.JPanel cardSubmitRequirementForm;
    private javax.swing.JComboBox<String> comboFilter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblAdminNote;
    private javax.swing.JLabel lblEventTitle;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel requirementListPanel;
    private javax.swing.JTable tblStudentDashboard;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

}

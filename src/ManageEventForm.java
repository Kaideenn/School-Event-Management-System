

import db.SessionManager;
import java.awt.CardLayout;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;


public class ManageEventForm extends javax.swing.JFrame {
    CardLayout cardLayout;
   
  private String originalEventName;
private String originalDate;
private String originalDeadline;
private String originalDescription;
private String originalCourse;
private ArrayList<String> originalRequirements;
private int currentEditingEventId = -1;



    public ManageEventForm() {
        if (!SessionManager.isAdminLoggedIn) {
            JOptionPane.showMessageDialog(null, "Access denied. Please log in first.");
            new RoleSelectionForm().setVisible(true);
            dispose();
            return;
        }

        initComponents();
        loadCoursesFromDatabase();
        populateCourseFilter();

        this.setLocationRelativeTo(null);
        loadEvents("All", "");

        styleTableHeader1234();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        mainPanel.add(cardManageEvents, "cardManageEvents");
        mainPanel.add(cardAddEventForm, "cardAddEventForm");
        cardLayout.show(mainPanel, "cardManageEvents");

        requirementListPanel = new JPanel();
        requirementListPanel.setLayout(new BoxLayout(requirementListPanel, BoxLayout.Y_AXIS));
        requirementListPanel.setBackground(Color.WHITE);
        jScrollPane1.setViewportView(requirementListPanel);
        jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        btnSearch.addActionListener(e -> {
            String filter = comboFilter.getSelectedItem().toString();
            String keyword = txtSearch.getText();
            loadEvents(filter, keyword);
        });

        comboCourseFilter.addActionListener(e -> {
            String filter = comboFilter.getSelectedItem().toString();
            String keyword = txtSearch.getText();
            loadEvents(filter, keyword);
        });

        comboFilter.addActionListener(e -> {
            String filter = comboFilter.getSelectedItem().toString();
            String keyword = txtSearch.getText();
            loadEvents(filter, keyword);
        });

        btnAddEvent.addActionListener(e -> {
            clearAddForm();
            cardLayout.show(mainPanel, "cardAddEventForm");
        });

        btnCancel.addActionListener(e -> cardLayout.show(mainPanel, "cardManageEvents"));

        btnSaveEvent.addActionListener(e -> saveOrUpdateEvent());
        btnAddRequirement.addActionListener(e -> addRequirementField());

        txtDate.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                validateDateInput(e);
            }
        });
        txtDeadline.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                validateDateInput(e);
            }
        });

        btnEdit.addActionListener(e -> editSelectedEvent());
        btnDelete.addActionListener(e -> deleteSelectedEvent());
        
        
    }

    private void validateDateInput(KeyEvent e) {
        char c = e.getKeyChar();
        if (!Character.isDigit(c) && c != '-' && !Character.isISOControl(c)) {
            e.consume();
            JOptionPane.showMessageDialog(null, "Only numbers and '-' are allowed (YYYY-MM-DD)");
        }
    }

  private void saveOrUpdateEvent() {
    String eventName = txtEventName.getText().trim();
    String date = txtDate.getText().trim();
    String course = cmbCourse.getSelectedItem().toString();
    String description = txtDescription.getText().trim();
    String deadline = txtDeadline.getText().trim();

    if (eventName.isEmpty() || date.isEmpty() || deadline.isEmpty() || description.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Please complete all required fields.");
        return;
    }

    String datePattern = "\\d{4}-\\d{2}-\\d{2}";
    if (!date.matches(datePattern) || !deadline.matches(datePattern)) {
        JOptionPane.showMessageDialog(null, "Date and Deadline must be in YYYY-MM-DD format.");
        return;
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    sdf.setLenient(false);

    try {
        Date eventDateObj = sdf.parse(date);
        Date deadlineDateObj = sdf.parse(deadline);

        Date today = new Date();
        if (eventDateObj.before(today) || deadlineDateObj.before(today)) {
            JOptionPane.showMessageDialog(null, "Date or deadline cannot be in the past.");
            return;
        }

      if (!deadlineDateObj.before(eventDateObj)) {
    JOptionPane.showMessageDialog(null, "Deadline must be before the event date. Same-day deadlines are not allowed.");
    return;
}


        boolean hasRequirement = false;
        List<JPanel> toRemove = new ArrayList<>();
        for (Component comp : requirementListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                JTextField reqField = (JTextField) panel.getComponent(0);
                String requirement = reqField.getText().trim();
                if (requirement.isEmpty()) {
                    toRemove.add(panel);
                } else {
                    hasRequirement = true;
                }
            }
        }

        for (JPanel panel : toRemove) {
            requirementListPanel.remove(panel);
        }
        requirementListPanel.revalidate();
        requirementListPanel.repaint();

        if (!hasRequirement) {
            JOptionPane.showMessageDialog(null, "Please add at least one requirement for this event.");
            return;
        }

        HashSet<String> requirementSet = new HashSet<>();
        for (Component comp : requirementListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JTextField reqField = (JTextField) ((JPanel) comp).getComponent(0);
                String requirement = reqField.getText().trim().toLowerCase();
                if (!requirement.isEmpty()) {
                    if (requirementSet.contains(requirement)) {
                        JOptionPane.showMessageDialog(null, "Duplicate requirements are not allowed.");
                        return;
                    }
                    requirementSet.add(requirement);
                }
            }
        }

        if (btnSaveEvent.getText().equals("Update")) {
            ArrayList<String> currentRequirements = getRequirementTexts();

            boolean noChanges = eventName.equals(originalEventName)
                    && date.equals(originalDate)
                    && deadline.equals(originalDeadline)
                    && description.equals(originalDescription)
                    && course.equals(originalCourse)
                    && currentRequirements.equals(originalRequirements);

            if (noChanges) {
                JOptionPane.showMessageDialog(null, "No changes made.");
                return;
            }

            int confirmUpdate = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to update this event?",
                    "Confirm Update",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmUpdate != JOptionPane.YES_OPTION) {
                return;
            }
        } else {
            int confirmAdd = JOptionPane.showConfirmDialog(
                    null,
                    "Are you sure you want to add this event?",
                    "Confirm Add",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirmAdd != JOptionPane.YES_OPTION) {
                return;
            }
        }

        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
        String query;
        if (btnSaveEvent.getText().equals("Update")) {
            query = "SELECT COUNT(*) FROM events WHERE event_name = ? AND course = ? AND date = ? AND deadline = ? AND id != ?";
        } else {
            query = "SELECT COUNT(*) FROM events WHERE event_name = ? AND course = ? AND date = ? AND deadline = ?";
        }

        PreparedStatement checkStmt = con.prepareStatement(query);
        checkStmt.setString(1, eventName);
        checkStmt.setString(2, course);
        checkStmt.setString(3, date);
        checkStmt.setString(4, deadline);
        if (btnSaveEvent.getText().equals("Update")) {
            checkStmt.setInt(5, currentEditingEventId);
        }

        ResultSet rsCheck = checkStmt.executeQuery();
        if (rsCheck.next() && rsCheck.getInt(1) > 0) {
            JOptionPane.showMessageDialog(null, "Duplicate event exists.");
            return;
        }

        if (btnSaveEvent.getText().equals("Update")) {
            PreparedStatement updateStmt = con.prepareStatement(
                    "UPDATE events SET event_name=?, date=?, course=?, description=?, deadline=? WHERE id=?"
            );
            updateStmt.setString(1, eventName);
            updateStmt.setString(2, date);
            updateStmt.setString(3, course);
            updateStmt.setString(4, description);
            updateStmt.setString(5, deadline);
            updateStmt.setInt(6, currentEditingEventId);
            updateStmt.executeUpdate();

            PreparedStatement delReqStmt = con.prepareStatement("DELETE FROM event_requirements WHERE event_id = ?");
            delReqStmt.setInt(1, currentEditingEventId);
            delReqStmt.executeUpdate();

            insertRequirements(con, currentEditingEventId);
            JOptionPane.showMessageDialog(null, "Event updated.");
        } else {
            PreparedStatement insertStmt = con.prepareStatement(
                    "INSERT INTO events (event_name, date, course, description, deadline) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            insertStmt.setString(1, eventName);
            insertStmt.setString(2, date);
            insertStmt.setString(3, course);
            insertStmt.setString(4, description);
            insertStmt.setString(5, deadline);
            insertStmt.executeUpdate();
            ResultSet rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                insertRequirements(con, rs.getInt(1));
            }
            JOptionPane.showMessageDialog(null, "Event added.");
        }

        btnSaveEvent.setText("Save");
        currentEditingEventId = -1;
        cardLayout.show(mainPanel, "cardManageEvents");
        loadEvents("All", "");
        populateCourseFilter();
        comboFilter.setSelectedIndex(0);
        txtSearch.setText("");
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
    }
}

private ArrayList<String> getRequirementTexts() {
    ArrayList<String> reqs = new ArrayList<>();
    for (Component comp : requirementListPanel.getComponents()) {
        if (comp instanceof JPanel) {
            JTextField txt = (JTextField) ((JPanel) comp).getComponent(0);
            String text = txt.getText().trim();
            if (!text.isEmpty()) {
                reqs.add(text);
            }
        }
    }
    return reqs;
}


    private void insertRequirements(Connection con, int eventId) throws SQLException {
        for (Component comp : requirementListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JTextField reqField = (JTextField) ((JPanel) comp).getComponent(0);
                String requirement = reqField.getText().trim();
                if (!requirement.isEmpty()) {
                    PreparedStatement reqStmt = con.prepareStatement("INSERT INTO event_requirements (event_id, requirement_name) VALUES (?, ?)");
                    reqStmt.setInt(1, eventId);
                    reqStmt.setString(2, requirement);
                    reqStmt.executeUpdate();
                }
            }
        }
    }



   private void loadEvents(String sortFilter, String keyword) {
    try {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM events");
        ArrayList<String> conditions = new ArrayList<>();
        ArrayList<String> parameters = new ArrayList<>();

        keyword = keyword.trim();
       Object selectedCourseObj = comboCourseFilter.getSelectedItem();
String selectedCourse = selectedCourseObj != null ? selectedCourseObj.toString() : "";

 tblManageEvents.addMouseListener(new MouseAdapter() {
    private int lastClickedRow = -1;

    @Override
    public void mousePressed(MouseEvent e) {
        int clickedRow = tblManageEvents.rowAtPoint(e.getPoint());
        if (clickedRow == -1) return; 

        if (clickedRow == lastClickedRow) {
       
            tblManageEvents.clearSelection();
            lastClickedRow = -1;
        } else {
          
            tblManageEvents.setRowSelectionInterval(clickedRow, clickedRow);
            lastClickedRow = clickedRow;
        }
    }
});


        if (!keyword.isEmpty()) {
            conditions.add("(event_name LIKE ? OR course LIKE ? OR description LIKE ? OR date LIKE ? OR deadline LIKE ?)");
            for (int i = 0; i < 5; i++) {
                parameters.add("%" + keyword + "%");
            }
        }

        if (!selectedCourse.equals("All")) {
            conditions.add("course = ?");
            parameters.add(selectedCourse);
        }

        if (!conditions.isEmpty()) {
            queryBuilder.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        switch (sortFilter) {
            case "A-Z":
                queryBuilder.append(" ORDER BY event_name ASC");
                break;
            case "Z-A":
                queryBuilder.append(" ORDER BY event_name DESC");
                break;
            case "Date":
                queryBuilder.append(" ORDER BY date ASC");
                break;
            case "Course":
                queryBuilder.append(" ORDER BY course ASC");
                break;
        }

        PreparedStatement stmt = con.prepareStatement(queryBuilder.toString());

        for (int i = 0; i < parameters.size(); i++) {
            stmt.setString(i + 1, parameters.get(i));
        }

        ResultSet rs = stmt.executeQuery();
        DefaultTableModel model = (DefaultTableModel) tblManageEvents.getModel();
        model.setRowCount(0);

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("event_name"),
                rs.getString("date"),
                rs.getString("course"),
                rs.getString("description"),
                rs.getString("deadline")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Error loading events.");
    }
}


 private void loadRequirementsForEvent(int eventId) {
    requirementListPanel.removeAll();
    originalRequirements = new ArrayList<>();

    try {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
        PreparedStatement stmt = con.prepareStatement("SELECT * FROM event_requirements WHERE event_id = ?");
        stmt.setInt(1, eventId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String requirementName = rs.getString("requirement_name").trim();
            originalRequirements.add(requirementName);

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JTextField txtRequirement = new JTextField(requirementName, 20);
            JButton btnRemove = new JButton("Remove");

            btnRemove.addActionListener(e -> {
                requirementListPanel.remove(panel);
                requirementListPanel.revalidate();
                requirementListPanel.repaint();
            });

            panel.add(txtRequirement);
            panel.add(btnRemove);
            requirementListPanel.add(panel);
        }

        requirementListPanel.revalidate();
        requirementListPanel.repaint();
    } catch (Exception e) {
        e.printStackTrace();
    }
}


 private void addRequirementField() {
   
    List<Component> toRemove = new ArrayList<>();
    for (Component comp : requirementListPanel.getComponents()) {
        if (comp instanceof JPanel panel) {
            JTextField reqField = (JTextField) panel.getComponent(0);
            if (reqField.getText().trim().isEmpty()) {
                toRemove.add(panel);
            }
        }
    }

    if (!toRemove.isEmpty()) {
        for (Component comp : toRemove) {
            requirementListPanel.remove(comp);
        }
        requirementListPanel.revalidate();
        requirementListPanel.repaint();
        JOptionPane.showMessageDialog(null, "Please fill in the current requirement before adding another.");
        return;
    }

   
    for (Component comp : requirementListPanel.getComponents()) {
        if (comp instanceof JPanel panel) {
            JTextField reqField = (JTextField) panel.getComponent(0);
            if (reqField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please fill in the current requirement before adding another.");
                return;
            }
        }
    }

    
    Set<String> existingRequirements = new HashSet<>();
    for (Component comp : requirementListPanel.getComponents()) {
        if (comp instanceof JPanel panel) {
            JTextField reqField = (JTextField) panel.getComponent(0);
            String text = reqField.getText().trim().toLowerCase();
            if (!text.isEmpty()) {
                if (!existingRequirements.add(text)) {
                    JOptionPane.showMessageDialog(null, "Duplicate requirements are not allowed.");
                    return;
                }
            }
        }
    }

    
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JTextField txtRequirement = new JTextField(20);
    JButton btnRemove = new JButton("Remove");

    
    btnRemove.addActionListener(e -> {
        requirementListPanel.remove(panel);
        requirementListPanel.revalidate();
        requirementListPanel.repaint();
    });

    panel.add(txtRequirement);
    panel.add(btnRemove);

    requirementListPanel.add(panel);
    requirementListPanel.revalidate();
    requirementListPanel.repaint();

    txtRequirement.requestFocus();
}





   private void editSelectedEvent() {
    int selectedRow = tblManageEvents.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(null, "Select an event to edit.");
        return;
    }

    
    currentEditingEventId = Integer.parseInt(tblManageEvents.getValueAt(selectedRow, 0).toString());
    String eventName = tblManageEvents.getValueAt(selectedRow, 1).toString();
    String date = tblManageEvents.getValueAt(selectedRow, 2).toString();
    String course = tblManageEvents.getValueAt(selectedRow, 3).toString();
    String description = tblManageEvents.getValueAt(selectedRow, 4).toString();
    String deadline = tblManageEvents.getValueAt(selectedRow, 5).toString();

    
    txtEventName.setText(eventName);
    txtDate.setText(date);
    cmbCourse.setSelectedItem(course);
    txtDescription.setText(description);
    txtDeadline.setText(deadline);

    
    originalEventName = eventName;
    originalDate = date;
    originalCourse = course;
    originalDescription = description;
    originalDeadline = deadline;

    
    originalRequirements = new ArrayList<>();
    loadRequirementsForEvent(currentEditingEventId); 

    // Switch to the update form
    btnSaveEvent.setText("Update");
    cardLayout.show(mainPanel, "cardAddEventForm");
}


    private void deleteSelectedEvent() {
        int selectedRow = tblManageEvents.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Select an event to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int eventId = Integer.parseInt(tblManageEvents.getValueAt(selectedRow, 0).toString());
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
                PreparedStatement delReqStmt = con.prepareStatement("DELETE FROM event_requirements WHERE event_id = ?");
                delReqStmt.setInt(1, eventId);
                delReqStmt.executeUpdate();

                PreparedStatement delEventStmt = con.prepareStatement("DELETE FROM events WHERE id = ?");
                delEventStmt.setInt(1, eventId);
                delEventStmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "Event deleted.");
              
                loadEvents("All", "");
          populateCourseFilter();
          comboFilter.setSelectedIndex(0); 
    txtSearch.setText("");    
  loadEvents("All", "");  
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error deleting event: " + ex.getMessage());
            }
        }
    }

    private void clearAddForm() {
    txtEventName.setText("");
    txtDate.setText("");
    cmbCourse.setSelectedIndex(0);
    txtDescription.setText("");
    txtDeadline.setText("");

    requirementListPanel.removeAll();
    requirementListPanel.revalidate();
    requirementListPanel.repaint();

    btnSaveEvent.setText("Save");
    currentEditingEventId = -1;
}


   private void populateCourseFilter() {
    comboCourseFilter.removeAllItems();
    comboCourseFilter.addItem("All");
    try {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
        PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT course FROM events");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            comboCourseFilter.addItem(rs.getString("course"));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

private void loadCoursesFromDatabase() {
    try {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
        String query = "SELECT course_name FROM courses";
        PreparedStatement stmt = con.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        cmbCourse.removeAllItems(); 

        while (rs.next()) {
            String courseName = rs.getString("course_name");
            cmbCourse.addItem(courseName);
        }

        con.close();
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to load courses: " + ex.getMessage());
    }
}

private void loadEventForEditing(int eventId) {
    try {
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/event_system", "root", "");
        PreparedStatement pst = con.prepareStatement("SELECT * FROM events WHERE id = ?");
        pst.setInt(1, eventId);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            txtEventName.setText(rs.getString("event_name"));
            txtDate.setText(rs.getString("date"));
            txtDeadline.setText(rs.getString("deadline"));
            txtDescription.setText(rs.getString("description"));
            cmbCourse.setSelectedItem(rs.getString("course"));

            
            originalEventName = rs.getString("event_name");
            originalDate = rs.getString("date");
            originalDeadline = rs.getString("deadline");
            originalDescription = rs.getString("description");
            originalCourse = rs.getString("course");
        }

      
        originalRequirements = new ArrayList<>();
        PreparedStatement reqStmt = con.prepareStatement("SELECT requirement FROM event_requirements WHERE event_id = ?");
        reqStmt.setInt(1, eventId);
        ResultSet reqRs = reqStmt.executeQuery();

        requirementListPanel.removeAll(); // clear UI
        while (reqRs.next()) {
            String req = reqRs.getString("requirement");
            originalRequirements.add(req);

            JPanel reqPanel = new JPanel();
            JTextField txt = new JTextField(req, 20);
            reqPanel.add(txt);
            requirementListPanel.add(reqPanel);
        }

        requirementListPanel.revalidate();
        requirementListPanel.repaint();

        btnSaveEvent.setText("Update");
        currentEditingEventId = eventId;
        cardLayout.show(mainPanel, "cardAddEditEvent");

    } catch (Exception e) {
        e.printStackTrace();
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
        cardManageEvents = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblManageEvents = new javax.swing.JTable();
        btnDelete = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnAddEvent = new javax.swing.JButton();
        btnSubmitRequest4 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        comboFilter = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        comboCourseFilter = new javax.swing.JComboBox<>();
        cardAddEventForm = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        txtEventName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtDate = new javax.swing.JTextField();
        cmbCourse = new javax.swing.JComboBox<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtDescription = new javax.swing.JTextArea();
        txtDeadline = new javax.swing.JTextField();
        btnSaveEvent = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        btnAddRequirement = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        requirementListPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1499, 854));
        setUndecorated(true);
        setResizable(false);
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
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1470, 21, 20, -1));

        jLabel2.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\menu(2).png")); // NOI18N
        jPanel2.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(21, 11, -1, 53));

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 102, 51));
        jLabel3.setText("MANAGEMENT SYSTEM");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 16, 240, 30));

        jLabel13.setBackground(new java.awt.Color(255, 255, 255));
        jLabel13.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("SCHOOL EVENT");
        jPanel2.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(85, 18, -1, -1));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1530, 70));

        mainPanel.setLayout(new java.awt.CardLayout());

        cardManageEvents.setForeground(new java.awt.Color(255, 255, 255));
        cardManageEvents.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        tblManageEvents.setModel(new javax.swing.table.DefaultTableModel(
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
                "ID", "Event Name", "Date", "Course", "Description", "Deadline"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class
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
        tblManageEvents.setFocusable(false);
        tblManageEvents.setRowHeight(25);
        tblManageEvents.setSelectionBackground(new java.awt.Color(255, 102, 51));
        tblManageEvents.setSelectionForeground(new java.awt.Color(255, 255, 255));
        tblManageEvents.setShowGrid(false);
        tblManageEvents.setShowHorizontalLines(true);
        tblManageEvents.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(tblManageEvents);

        cardManageEvents.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 90, 1420, 540));

        btnDelete.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnDelete.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\trash.png")); // NOI18N
        btnDelete.setText("Delete");
        btnDelete.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnDeleteMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnDeleteMouseExited(evt);
            }
        });
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        cardManageEvents.add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(1330, 650, 130, 40));

        btnEdit.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnEdit.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\refresh.png")); // NOI18N
        btnEdit.setText("Update");
        btnEdit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnEditMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnEditMouseExited(evt);
            }
        });
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });
        cardManageEvents.add(btnEdit, new org.netbeans.lib.awtextra.AbsoluteConstraints(1160, 650, 130, 40));

        btnAddEvent.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnAddEvent.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\plus.png")); // NOI18N
        btnAddEvent.setText("Add Event");
        btnAddEvent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAddEventMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAddEventMouseExited(evt);
            }
        });
        btnAddEvent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddEventActionPerformed(evt);
            }
        });
        cardManageEvents.add(btnAddEvent, new org.netbeans.lib.awtextra.AbsoluteConstraints(980, 650, -1, -1));

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
        cardManageEvents.add(btnSubmitRequest4, new org.netbeans.lib.awtextra.AbsoluteConstraints(1360, 30, -1, 40));

        jLabel8.setBackground(new java.awt.Color(0, 0, 0));
        jLabel8.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel8.setText("Course :");
        cardManageEvents.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 20, 90, 60));

        comboFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "A-Z", "Z-A", "Date" }));
        comboFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboFilterActionPerformed(evt);
            }
        });
        cardManageEvents.add(comboFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 30, 210, 40));

        jLabel9.setBackground(new java.awt.Color(0, 0, 0));
        jLabel9.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel9.setText("Search :");
        cardManageEvents.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 20, 90, 60));

        txtSearch.setFont(new java.awt.Font("STXihei", 0, 14)); // NOI18N
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        cardManageEvents.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(830, 30, 230, 40));

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
        cardManageEvents.add(btnSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(1080, 30, -1, 40));

        jLabel11.setBackground(new java.awt.Color(0, 0, 0));
        jLabel11.setFont(new java.awt.Font("STXihei", 1, 16)); // NOI18N
        jLabel11.setText("FIlter :");
        cardManageEvents.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 20, 60, 60));

        comboCourseFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboCourseFilterActionPerformed(evt);
            }
        });
        cardManageEvents.add(comboCourseFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 30, 210, 40));

        mainPanel.add(cardManageEvents, "card3");

        cardAddEventForm.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(153, 153, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtEventName.setFont(new java.awt.Font("STXihei", 0, 18)); // NOI18N
        txtEventName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtEventNameFocusLost(evt);
            }
        });
        txtEventName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEventNameActionPerformed(evt);
            }
        });
        txtEventName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtEventNameKeyTyped(evt);
            }
        });
        jPanel1.add(txtEventName, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 90, 400, 40));

        jLabel5.setBackground(new java.awt.Color(255, 255, 255));
        jLabel5.setFont(new java.awt.Font("STXinwei", 1, 44)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Add Event Form ");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 20, 350, 50));

        txtDate.setFont(new java.awt.Font("STXihei", 0, 18)); // NOI18N
        txtDate.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtDateFocusLost(evt);
            }
        });
        txtDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDateActionPerformed(evt);
            }
        });
        txtDate.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtDateKeyTyped(evt);
            }
        });
        jPanel1.add(txtDate, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 160, 400, 40));

        cmbCourse.setFont(new java.awt.Font("STXihei", 0, 18)); // NOI18N
        cmbCourse.setForeground(new java.awt.Color(51, 51, 51));
        cmbCourse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbCourseActionPerformed(evt);
            }
        });
        jPanel1.add(cmbCourse, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 240, 400, 40));

        txtDescription.setColumns(20);
        txtDescription.setFont(new java.awt.Font("STXihei", 0, 18)); // NOI18N
        txtDescription.setRows(5);
        jScrollPane3.setViewportView(txtDescription);

        jPanel1.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 320, 400, 60));

        txtDeadline.setFont(new java.awt.Font("STXihei", 0, 18)); // NOI18N
        txtDeadline.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtDeadlineFocusLost(evt);
            }
        });
        txtDeadline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDeadlineActionPerformed(evt);
            }
        });
        txtDeadline.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtDeadlineKeyTyped(evt);
            }
        });
        jPanel1.add(txtDeadline, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 420, 400, 40));

        btnSaveEvent.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnSaveEvent.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\right-arrow(2).png")); // NOI18N
        btnSaveEvent.setText("Save");
        btnSaveEvent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnSaveEventMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnSaveEventMouseExited(evt);
            }
        });
        btnSaveEvent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveEventActionPerformed(evt);
            }
        });
        jPanel1.add(btnSaveEvent, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 640, 140, 40));

        btnCancel.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnCancel.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\prohibition.png")); // NOI18N
        btnCancel.setText("Cancel");
        btnCancel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnCancelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnCancelMouseExited(evt);
            }
        });
        jPanel1.add(btnCancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 640, 140, 40));

        jLabel17.setBackground(new java.awt.Color(255, 255, 255));
        jLabel17.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("Event Name :");
        jPanel1.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 90, -1, 40));

        jLabel10.setBackground(new java.awt.Color(255, 255, 255));
        jLabel10.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Event Date :");
        jPanel1.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 160, -1, 40));

        jLabel12.setBackground(new java.awt.Color(255, 255, 255));
        jLabel12.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Course :");
        jPanel1.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 240, -1, 40));

        jLabel14.setBackground(new java.awt.Color(255, 255, 255));
        jLabel14.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("Description :");
        jPanel1.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 320, -1, 60));

        jLabel16.setBackground(new java.awt.Color(255, 255, 255));
        jLabel16.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Deadline :");
        jPanel1.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 420, -1, 40));

        jLabel15.setBackground(new java.awt.Color(255, 255, 255));
        jLabel15.setFont(new java.awt.Font("STXihei", 1, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("Requirements : ");
        jPanel1.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(232, 490, 150, 120));

        btnAddRequirement.setFont(new java.awt.Font("STXihei", 1, 14)); // NOI18N
        btnAddRequirement.setIcon(new javax.swing.ImageIcon("C:\\Users\\bihas\\Downloads\\add(1).png")); // NOI18N
        btnAddRequirement.setText("Add");
        btnAddRequirement.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btnAddRequirementMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btnAddRequirementMouseExited(evt);
            }
        });
        btnAddRequirement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddRequirementActionPerformed(evt);
            }
        });
        jPanel1.add(btnAddRequirement, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 530, 120, 40));

        requirementListPanel.setLayout(new javax.swing.BoxLayout(requirementListPanel, javax.swing.BoxLayout.LINE_AXIS));
        jScrollPane1.setViewportView(requirementListPanel);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 490, 400, 120));

        cardAddEventForm.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 0, 1150, 700));

        mainPanel.add(cardAddEventForm, "card3");

        getContentPane().add(mainPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 150, 1500, 730));

        jLabel1.setFont(new java.awt.Font("STXihei", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 102, 255));
        jLabel1.setText("Events");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(770, 80, 270, 60));

        jLabel6.setFont(new java.awt.Font("STXihei", 1, 36)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 102, 102));
        jLabel6.setText("Manage");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 80, 170, 60));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jLabel4MouseClicked

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnAddEventActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddEventActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnAddEventActionPerformed

    private void btnSubmitRequest4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitRequest4ActionPerformed
        // TODO add your handling code here:
        new AdminDashBoardForm().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnSubmitRequest4ActionPerformed

    private void btnAddRequirementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddRequirementActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnAddRequirementActionPerformed

    private void btnSaveEventActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveEventActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnSaveEventActionPerformed

    private void txtDeadlineKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDeadlineKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDeadlineKeyTyped

    private void txtDeadlineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDeadlineActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDeadlineActionPerformed

    private void txtDeadlineFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtDeadlineFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDeadlineFocusLost

    private void cmbCourseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbCourseActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbCourseActionPerformed

    private void txtDateKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDateKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDateKeyTyped

    private void txtDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDateActionPerformed

    private void txtDateFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtDateFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDateFocusLost

    private void txtEventNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtEventNameKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventNameKeyTyped

    private void txtEventNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEventNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventNameActionPerformed

    private void txtEventNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtEventNameFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEventNameFocusLost

    private void comboFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboFilterActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboFilterActionPerformed

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

    private void btnSubmitRequest4MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest4MouseEntered
        // TODO add your handling code here:
           btnSubmitRequest4.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnSubmitRequest4MouseEntered

    private void btnDeleteMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteMouseEntered
        // TODO add your handling code here:
           btnDelete.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnDeleteMouseEntered

    private void btnAddEventMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAddEventMouseEntered
        // TODO add your handling code here:
         btnAddEvent.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnAddEventMouseEntered

    private void btnEditMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditMouseEntered
        // TODO add your handling code here:
         btnEdit.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnEditMouseEntered

    private void btnSubmitRequest4MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSubmitRequest4MouseExited
        // TODO add your handling code here:
           btnSubmitRequest4.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnSubmitRequest4MouseExited

    private void btnAddEventMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAddEventMouseExited
        // TODO add your handling code here:
           btnAddEvent.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnAddEventMouseExited

    private void btnEditMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnEditMouseExited
        // TODO add your handling code here:
           btnEdit.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnEditMouseExited

    private void btnDeleteMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnDeleteMouseExited
        // TODO add your handling code here:
           btnDelete.setBackground(new Color(242, 242, 242));
    }//GEN-LAST:event_btnDeleteMouseExited

    private void comboCourseFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboCourseFilterActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboCourseFilterActionPerformed

    private void btnAddRequirementMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAddRequirementMouseEntered
        // TODO add your handling code here:
           btnAddRequirement.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnAddRequirementMouseEntered

    private void btnSaveEventMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSaveEventMouseEntered
        // TODO add your handling code here:
           btnSaveEvent.setBackground(new Color(102,255,102));
    }//GEN-LAST:event_btnSaveEventMouseEntered

    private void btnCancelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCancelMouseEntered
        // TODO add your handling code here:
          btnCancel.setBackground(new Color(255,102,51));
    }//GEN-LAST:event_btnCancelMouseEntered

    private void btnAddRequirementMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnAddRequirementMouseExited
        // TODO add your handling code here:
          btnAddRequirement.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnAddRequirementMouseExited

    private void btnCancelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnCancelMouseExited
        // TODO add your handling code here:
          btnCancel.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnCancelMouseExited

    private void btnSaveEventMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnSaveEventMouseExited
        // TODO add your handling code here:
          btnSaveEvent.setBackground(new Color(255, 255, 255));

    }//GEN-LAST:event_btnSaveEventMouseExited

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
            java.util.logging.Logger.getLogger(ManageEventForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ManageEventForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ManageEventForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ManageEventForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ManageEventForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddEvent;
    private javax.swing.JButton btnAddRequirement;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnSaveEvent;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSubmitRequest4;
    private javax.swing.JPanel cardAddEventForm;
    private javax.swing.JPanel cardManageEvents;
    private javax.swing.JComboBox<String> cmbCourse;
    private javax.swing.JComboBox<String> comboCourseFilter;
    private javax.swing.JComboBox<String> comboFilter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel requirementListPanel;
    private javax.swing.JTable tblManageEvents;
    private javax.swing.JTextField txtDate;
    private javax.swing.JTextField txtDeadline;
    private javax.swing.JTextArea txtDescription;
    private javax.swing.JTextField txtEventName;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

      
private void styleTableHeader1234() {
    tblManageEvents.getTableHeader().setDefaultRenderer(new ManageEventForm.HeaderColor());
    tblManageEvents.setRowHeight(45);

    JTableHeader header = tblManageEvents.getTableHeader();
    header.setPreferredSize(new Dimension(header.getWidth(), 50));
    header.setFont(new Font("Segoe UI", Font.BOLD, 14));
    header.setBackground(new Color(32, 136, 203));
    header.setForeground(Color.WHITE);
    
    applyLeftAlignToCells(); 
}
    class HeaderColor extends DefaultTableCellRenderer {
    public HeaderColor() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setBackground(new Color(102,153,255));
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


private void applyLeftAlignToCells() {
    TableCellRenderer leftRenderer = new CellRendererLeftAlign();
    for (int i = 0; i < tblManageEvents.getColumnCount(); i++) {
        tblManageEvents.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
    }
}

}
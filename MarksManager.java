import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

public class MarksManager extends Frame {
    Choice studentChoice, subjectChoice, assessmentChoice;
    TextField nameField, rollField, marksField;
    TextArea reportArea;
    Button addStudentBtn, updateStudentBtn, deleteStudentBtn;
    Button addSubjectBtn, updateSubjectBtn, deleteSubjectBtn;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            boolean authenticated = showLoginDialog();
            if (authenticated) {
                new MarksManager();
            } else {
                System.exit(0);
            }
        });
    }

    static boolean showLoginDialog() {
        JDialog dialog = new JDialog((Frame) null, "Login", true);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(null);

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        JButton cancelBtn = new JButton("Cancel");

        dialog.add(new JLabel("Username:"));
        dialog.add(userField);
        dialog.add(new JLabel("Password:"));
        dialog.add(passField);
        dialog.add(loginBtn);
        dialog.add(cancelBtn);

        final boolean[] authenticated = {false};

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());

            try (Connection conn = DBUtil.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
                );
                ps.setString(1, user);
                ps.setString(2, pass);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    authenticated[0] = true;
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Invalid credentials.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
        return authenticated[0];
    }

    public MarksManager() {
        setTitle("Student Marks Management");
        setSize(700, 600);
        setLayout(new FlowLayout());

        add(new Label("Student Name:"));
        nameField = new TextField(15); add(nameField);

        add(new Label("Roll No:"));
        rollField = new TextField(10); add(rollField);

        addStudentBtn = new Button("Add Student");
        updateStudentBtn = new Button("Update Student");
        deleteStudentBtn = new Button("Delete Student");
        add(addStudentBtn); add(updateStudentBtn); add(deleteStudentBtn);

        addStudentBtn.addActionListener(e -> addStudent());
        updateStudentBtn.addActionListener(e -> updateStudent());
        deleteStudentBtn.addActionListener(e -> deleteStudent());

        add(new Label("Subject:"));
        TextField subjectField = new TextField(10);
        add(subjectField);

        addSubjectBtn = new Button("Add Subject");
        updateSubjectBtn = new Button("Update Subject");
        deleteSubjectBtn = new Button("Delete Subject");
        add(addSubjectBtn); add(updateSubjectBtn); add(deleteSubjectBtn);
        
        

        addSubjectBtn.addActionListener(e -> {
            String subject = subjectField.getText();
            try (Connection conn = DBUtil.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO subjects(name) VALUES(?)");
                ps.setString(1, subject);
                ps.executeUpdate();
                loadChoices();
                JOptionPane.showMessageDialog(this, "Subject added.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        updateSubjectBtn.addActionListener(e -> {
            try (Connection conn = DBUtil.getConnection()) {
                int subjectId = Integer.parseInt(subjectChoice.getSelectedItem().split("-")[0]);
                String subject = subjectField.getText();
                PreparedStatement ps = conn.prepareStatement("UPDATE subjects SET name=? WHERE id=?");
                ps.setString(1, subject);
                ps.setInt(2, subjectId);
                ps.executeUpdate();
                loadChoices();
                JOptionPane.showMessageDialog(this, "Subject updated.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

deleteSubjectBtn.addActionListener(e -> {
    try (Connection conn = DBUtil.getConnection()) {
        int subjectId = Integer.parseInt(subjectChoice.getSelectedItem().split("-")[0]);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this subject and all related marks?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM marks WHERE subject_id=?");
            ps1.setInt(1, subjectId);
            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM subjects WHERE id=?");
            ps2.setInt(1, subjectId);
            ps2.executeUpdate();

            loadChoices();
            JOptionPane.showMessageDialog(this, "Subject and related marks deleted.");
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
});


        studentChoice = new Choice();
        subjectChoice = new Choice();
        assessmentChoice = new Choice();
        assessmentChoice.add("Internal");
        assessmentChoice.add("Midterm");
        assessmentChoice.add("Final");

        add(new Label("Student:")); add(studentChoice);
        add(new Label("Subject:")); add(subjectChoice);
        add(new Label("Assessment:")); add(assessmentChoice);

        add(new Label("Marks:"));
        marksField = new TextField(5); add(marksField);

        Button addMarksBtn = new Button("Add Marks");
        add(addMarksBtn);

        addMarksBtn.addActionListener(e -> addMarks());

        Button studentReportBtn = new Button("Student Report");
        Button subjectAvgBtn = new Button("Subject Averages");
        add(studentReportBtn); add(subjectAvgBtn);

        reportArea = new TextArea(15, 60);
        add(reportArea);

        studentReportBtn.addActionListener(e -> generateStudentReport());
        subjectAvgBtn.addActionListener(e -> generateSubjectAverages());
        
        Button allStudentsReportBtn = new Button("All Students Report");
        add(allStudentsReportBtn);

        allStudentsReportBtn.addActionListener(e -> generateAllStudentsReport());

        Button exitBtn = new Button("Exit");
        add(exitBtn);
        exitBtn.addActionListener(e -> System.exit(0));

        loadChoices();
        toggleAdminControls(true);
        setVisible(true);
    }
    
    void generateAllStudentsReport() {
    try (Connection conn = DBUtil.getConnection()) {
        PreparedStatement ps = conn.prepareStatement(
            "SELECT s.name, s.roll_no, sub.name AS subject, m.assessment, m.marks " +
            "FROM marks m " +
            "JOIN students s ON s.id = m.student_id " +
            "JOIN subjects sub ON sub.id = m.subject_id " +
            "ORDER BY s.name, sub.name, m.assessment");

        ResultSet rs = ps.executeQuery();

        StringBuilder report = new StringBuilder("All Students Report:\n\n");
        String lastStudent = "";
        while (rs.next()) {
            String currentStudent = rs.getString("name") + " (" + rs.getString("roll_no") + ")";
            if (!currentStudent.equals(lastStudent)) {
                report.append("\n").append(currentStudent).append("\n");
                lastStudent = currentStudent;
            }
            report.append("   ")
                  .append(rs.getString("subject")).append(" - ")
                  .append(rs.getString("assessment")).append(": ")
                  .append(rs.getDouble("marks")).append("\n");
        }

        reportArea.setText(report.toString());
    } catch (Exception e) {
        reportArea.setText("Error generating all students report: " + e.getMessage());
    }
}
    
    void toggleAdminControls(boolean show) {
        addStudentBtn.setEnabled(show);
        updateStudentBtn.setEnabled(show);
        deleteStudentBtn.setEnabled(show);
        addSubjectBtn.setEnabled(show);
        updateSubjectBtn.setEnabled(show);
        deleteSubjectBtn.setEnabled(show);
    }

    void addStudent() {
        String name = nameField.getText();
        String roll = rollField.getText();
        if (name.isEmpty() || roll.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Roll No required.");
            return;
        }
        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO students (name, roll_no) VALUES (?, ?)");
            ps.setString(1, name);
            ps.setString(2, roll);
            ps.executeUpdate();
            loadChoices();
            JOptionPane.showMessageDialog(this, "Student added.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    void updateStudent() {
        try (Connection conn = DBUtil.getConnection()) {
            int studentId = Integer.parseInt(studentChoice.getSelectedItem().split("-")[0]);
            String name = nameField.getText();
            String roll = rollField.getText();
            PreparedStatement ps = conn.prepareStatement("UPDATE students SET name=?, roll_no=? WHERE id=?");
            ps.setString(1, name);
            ps.setString(2, roll);
            ps.setInt(3, studentId);
            ps.executeUpdate();
            loadChoices();
            JOptionPane.showMessageDialog(this, "Student updated.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    void deleteStudent() {
        try (Connection conn = DBUtil.getConnection()) {
            int studentId = Integer.parseInt(studentChoice.getSelectedItem().split("-")[0]);
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this student and all related marks?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                PreparedStatement ps1 = conn.prepareStatement("DELETE FROM marks WHERE student_id=?");
                ps1.setInt(1, studentId);
                ps1.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM students WHERE id=?");
                ps2.setInt(1, studentId);
                ps2.executeUpdate();
                loadChoices();
                JOptionPane.showMessageDialog(this, "Student and related marks deleted.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    void addMarks() {
        try (Connection conn = DBUtil.getConnection()) {
            int studentId = Integer.parseInt(studentChoice.getSelectedItem().split("-")[0]);
            int subjectId = Integer.parseInt(subjectChoice.getSelectedItem().split("-")[0]);
            String assessment = assessmentChoice.getSelectedItem();
            double marks = Double.parseDouble(marksField.getText());
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO marks (student_id, subject_id, assessment, marks) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, studentId);
            ps.setInt(2, subjectId);
            ps.setString(3, assessment);
            ps.setDouble(4, marks);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Marks added.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    void updateMarks() {
        // To be implemented based on marks ID or unique fields
        JOptionPane.showMessageDialog(this, "Update Marks feature not implemented.");
    }

    void deleteMarks() {
        // To be implemented based on marks ID or unique fields
        JOptionPane.showMessageDialog(this, "Delete Marks feature not implemented.");
    }

    void generateStudentReport() {
        try (Connection conn = DBUtil.getConnection()) {
            int studentId = Integer.parseInt(studentChoice.getSelectedItem().split("-")[0]);
            PreparedStatement ps = conn.prepareStatement(
                "SELECT s.name, sub.name AS subject, m.assessment, m.marks " +
                "FROM marks m JOIN students s ON s.id = m.student_id " +
                "JOIN subjects sub ON sub.id = m.subject_id " +
                "WHERE m.student_id = ?"
            );
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            StringBuilder report = new StringBuilder("Student Report:\n");
            while (rs.next()) {
                report.append(rs.getString("name")).append(" - ")
                      .append(rs.getString("subject")).append(" - ")
                      .append(rs.getString("assessment")).append(": ")
                      .append(rs.getDouble("marks")).append("\n");
            }
            reportArea.setText(report.toString());
        } catch (Exception e) {
            reportArea.setText("Error generating report.");
        }
    }

    void generateSubjectAverages() {
        try (Connection conn = DBUtil.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT sub.name AS subject, AVG(marks) as avg_mark " +
                "FROM marks m JOIN subjects sub ON sub.id = m.subject_id " +
                "GROUP BY subject_id"
            );
            StringBuilder report = new StringBuilder("Subject Averages:\n");
            while (rs.next()) {
                report.append(rs.getString("subject")).append(": ")
                      .append(String.format("%.2f", rs.getDouble("avg_mark"))).append("\n");
            }
            reportArea.setText(report.toString());
        } catch (Exception e) {
            reportArea.setText("Error generating averages.");
        }
    }

    void loadChoices() {
        studentChoice.removeAll();
        subjectChoice.removeAll();
        try (Connection conn = DBUtil.getConnection()) {
            ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM students");
            while (rs1.next()) {
                studentChoice.add(rs1.getInt("id") + "-" + rs1.getString("name"));
            }
            ResultSet rs2 = conn.createStatement().executeQuery("SELECT * FROM subjects");
            while (rs2.next()) {
                subjectChoice.add(rs2.getInt("id") + "-" + rs2.getString("name"));
            }
        } catch (Exception e) {
            System.out.println("Error loading choices: " + e.getMessage());
        }
    }
}

class DBUtil {
    public static Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/student_db", "root", "root@3112");
    }
}
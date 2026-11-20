package com.support;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketApp extends JFrame {
    private static final String[] PRIORITIES = {"Low", "Medium", "High", "Critical"};
    private static final String[] STATUSES = {"Open", "In Progress", "Resolved"};
    private static final String[] COLS = {"ID", "Customer", "Subject", "Priority", "Status", "Assigned To", "Created"};

    // New ticket form
    private final JTextField nameF = new JTextField(20), emailF = new JTextField(20), phoneF = new JTextField(20),
            subjectF = new JTextField(20), assignF = new JTextField(20);
    private final JTextArea descA = new JTextArea(5, 20);
    private final JComboBox<String> prioC = new JComboBox<>(PRIORITIES);

    // Ticket list
    private final DefaultTableModel listModel = new DefaultTableModel(COLS, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(listModel);
    private final JTextField searchId = new JTextField(6);
    private final JComboBox<String> searchStatus = new JComboBox<>(prepend("All", STATUSES));
    private final JComboBox<String> searchPrio = new JComboBox<>(prepend("All", PRIORITIES));

    // Pending report
    private final DefaultTableModel pendModel = new DefaultTableModel(
            new String[]{"ID", "Customer", "Subject", "Priority", "Status", "Assigned To", "Age (days)"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel pendSummary = new JLabel(" ");

    public TicketApp() {
        super("Customer Support Ticket System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("New Ticket", buildForm());
        tabs.addTab("Tickets", buildList());
        tabs.addTab("Pending Report", buildReport());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) search();
            if (tabs.getSelectedIndex() == 2) loadPending();
        });
        add(tabs);
        setSize(900, 520);
        setLocationRelativeTo(null);
    }

    private static String[] prepend(String first, String[] rest) {
        String[] r = new String[rest.length + 1];
        r[0] = first;
        System.arraycopy(rest, 0, r, 1, rest.length);
        return r;
    }

    // ---------- UI builders ----------
    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Customer Name*", "Email*", "Phone", "Subject*", "Priority", "Assign To", "Description"};
        Component[] fields = {nameF, emailF, phoneF, subjectF, prioC, assignF, new JScrollPane(descA)};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i;
            p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
            p.add(fields[i], g);
            g.fill = GridBagConstraints.NONE; g.weightx = 0;
        }
        JButton create = new JButton("Create Ticket");
        create.addActionListener(e -> createTicket());
        g.gridx = 1; g.gridy = labels.length;
        p.add(create, g);
        return p;
    }

    private JPanel buildList() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Ticket ID:")); top.add(searchId);
        top.add(new JLabel("Status:")); top.add(searchStatus);
        top.add(new JLabel("Priority:")); top.add(searchPrio);
        JButton find = new JButton("Search");
        find.addActionListener(e -> search());
        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            searchId.setText(""); searchStatus.setSelectedIndex(0); searchPrio.setSelectedIndex(0); search();
        });
        top.add(find); top.add(reset);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton assign = new JButton("Assign");
        assign.addActionListener(e -> assignSelected());
        JButton prio = new JButton("Change Priority");
        prio.addActionListener(e -> changePriority());
        JButton progress = new JButton("Mark In Progress");
        progress.addActionListener(e -> setStatus("In Progress"));
        JButton close = new JButton("Resolve / Close");
        close.addActionListener(e -> setStatus("Resolved"));
        JButton details = new JButton("View Details");
        details.addActionListener(e -> showDetails());
        for (JButton b : new JButton[]{assign, prio, progress, close, details}) bottom.add(b);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildReport() {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        JTable t = new JTable(pendModel);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadPending());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refresh); top.add(pendSummary);
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    // ---------- Actions ----------
    private void createTicket() {
        String name = nameF.getText().trim(), email = emailF.getText().trim(), subject = subjectF.getText().trim();
        if (name.isEmpty() || email.isEmpty() || subject.isEmpty()) {
            msg("Name, Email and Subject are required."); return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) { msg("Enter a valid email."); return; }
        try (Connection c = DB.get()) {
            int custId = findOrCreateCustomer(c, name, email, phoneF.getText().trim());
            String assignee = assignF.getText().trim();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO tickets (customer_id, subject, description, priority, status, assigned_to) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, custId);
                ps.setString(2, subject);
                ps.setString(3, descA.getText().trim());
                ps.setString(4, (String) prioC.getSelectedItem());
                ps.setString(5, "Open");
                ps.setString(6, assignee.isEmpty() ? null : assignee);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    msg("Ticket #" + k.getInt(1) + " created.");
                }
            }
            nameF.setText(""); emailF.setText(""); phoneF.setText(""); subjectF.setText("");
            assignF.setText(""); descA.setText(""); prioC.setSelectedIndex(1);
        } catch (SQLException ex) { error(ex); }
    }

    private int findOrCreateCustomer(Connection c, String name, String email, String phone) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM customers WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO customers (name, email, phone) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, email); ps.setString(3, phone);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    private void search() {
        StringBuilder sql = new StringBuilder(
                "SELECT t.id, c.name, t.subject, t.priority, t.status, COALESCE(t.assigned_to,'-'), t.created_at " +
                "FROM tickets t JOIN customers c ON c.id=t.customer_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        String id = searchId.getText().trim();
        if (!id.isEmpty()) {
            try { params.add(Integer.parseInt(id)); sql.append(" AND t.id=?"); }
            catch (NumberFormatException e) { msg("Ticket ID must be a number."); return; }
        }
        if (searchStatus.getSelectedIndex() > 0) { sql.append(" AND t.status=?"); params.add(searchStatus.getSelectedItem()); }
        if (searchPrio.getSelectedIndex() > 0) { sql.append(" AND t.priority=?"); params.add(searchPrio.getSelectedItem()); }
        sql.append(" ORDER BY t.id DESC");
        fill(listModel, sql.toString(), params, 7);
    }

    private void loadPending() {
        fill(pendModel,
                "SELECT t.id, c.name, t.subject, t.priority, t.status, COALESCE(t.assigned_to,'Unassigned'), " +
                "DATEDIFF(NOW(), t.created_at) FROM tickets t JOIN customers c ON c.id=t.customer_id " +
                "WHERE t.status <> 'Resolved' " +
                "ORDER BY FIELD(t.priority,'Critical','High','Medium','Low'), t.created_at",
                new ArrayList<>(), 7);
        int unassigned = 0, critical = 0;
        for (int i = 0; i < pendModel.getRowCount(); i++) {
            if ("Unassigned".equals(pendModel.getValueAt(i, 5))) unassigned++;
            if ("Critical".equals(pendModel.getValueAt(i, 3))) critical++;
        }
        pendSummary.setText("  Pending: " + pendModel.getRowCount() + "   |   Critical: " + critical
                + "   |   Unassigned: " + unassigned);
    }

    private void fill(DefaultTableModel model, String sql, List<Object> params, int cols) {
        model.setRowCount(0);
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                    model.addRow(row);
                }
            }
        } catch (SQLException ex) { error(ex); }
    }

    private Integer selectedId() {
        int r = table.getSelectedRow();
        if (r < 0) { msg("Select a ticket first."); return null; }
        return (Integer) listModel.getValueAt(table.convertRowIndexToModel(r), 0);
    }

    private void update(String sql, Object... args) {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        } catch (SQLException ex) { error(ex); }
        search();
    }

    private void assignSelected() {
        Integer id = selectedId(); if (id == null) return;
        String who = JOptionPane.showInputDialog(this, "Assign ticket #" + id + " to:");
        if (who == null || who.trim().isEmpty()) return;
        update("UPDATE tickets SET assigned_to=?, status=IF(status='Open','In Progress',status) WHERE id=?", who.trim(), id);
    }

    private void changePriority() {
        Integer id = selectedId(); if (id == null) return;
        Object p = JOptionPane.showInputDialog(this, "New priority for #" + id, "Priority",
                JOptionPane.PLAIN_MESSAGE, null, PRIORITIES, PRIORITIES[1]);
        if (p != null) update("UPDATE tickets SET priority=? WHERE id=?", p, id);
    }

    private void setStatus(String status) {
        Integer id = selectedId(); if (id == null) return;
        update("UPDATE tickets SET status=? WHERE id=?", status, id);
    }

    private void showDetails() {
        Integer id = selectedId(); if (id == null) return;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "SELECT t.subject, t.description, c.name, c.email, c.phone FROM tickets t " +
                "JOIN customers c ON c.id=t.customer_id WHERE t.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) msg("Ticket #" + id + "\nSubject: " + rs.getString(1) + "\nCustomer: " + rs.getString(3)
                        + " (" + rs.getString(4) + ", " + rs.getString(5) + ")\n\n" + rs.getString(2));
            }
        } catch (SQLException ex) { error(ex); }
    }

    private void msg(String m) { JOptionPane.showMessageDialog(this, m); }
    private void error(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicketApp().setVisible(true));
    }
}

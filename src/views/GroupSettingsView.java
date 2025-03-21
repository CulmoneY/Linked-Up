package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import interface_adapter.AddGroupEvent.AddGroupEventController;
import interface_adapter.AddGroupEvent.AddGroupEventState;
import interface_adapter.AddGroupEvent.AddGroupEventViewModel;
import interface_adapter.AddRecommendedEvent.AddRecommendedEventController;
import interface_adapter.AddRecommendedEvent.AddRecommendedEventState;
import interface_adapter.AddRecommendedEvent.AddRecommendedEventViewModel;
import interface_adapter.DeleteGroupEvent.DeleteGroupEventController;
import interface_adapter.DeleteGroupEvent.DeleteGroupEventState;
import interface_adapter.DeleteGroupEvent.DeleteGroupEventViewModel;
import interface_adapter.ModifyGroupName.ModifyGroupNameController;
import interface_adapter.ModifyGroupName.ModifyGroupNameViewModel;
import interface_adapter.RemoveGroupMember.RemoveGroupMemberController;
import interface_adapter.RemoveGroupMember.RemoveGroupMemberState;
import interface_adapter.RemoveGroupMember.RemoveGroupMemberViewModel;
import interface_adapter.TimeslotSelection.TimeslotSelectionController;
import entity.Event;
import interface_adapter.TimeslotSelection.TimeslotSelectionState;
import interface_adapter.TimeslotSelection.TimeslotSelectionViewModel;
import interface_adapter.AddGroupMember.*;
import interface_adapter.ExportCalendar.ExportCalendarState;
import interface_adapter.ExportCalendar.ExportCalendarViewModel;
import interface_adapter.ExportCalendar.ExportCalendarController;

public class GroupSettingsView extends JPanel implements ActionListener, PropertyChangeListener {

    private final ViewManager viewManager;
    private final JPanel eventsPanel;
    private final JPanel membersPanel;
    private final JPanel addMembersPanel;
    private final JTextField eventNameField;
    private final JTextField eventStartField;
    private final JTextField eventEndField;
    private final JLabel recommendedEventLabel;
    private final JLabel groupNameLabel;
    private final String viewName = "groupSettingsView";
    private Event reccomendedEvent;

    private final TimeslotSelectionViewModel timeslotSelectionViewModel;
    private TimeslotSelectionController timeslotSelectionController;
    
    private final AddGroupMemberViewModel addGroupMemberViewModel;
    private  AddGroupMemberController addGroupMemberController;

    private final RemoveGroupMemberViewModel removeGroupMemberViewModel;
    private RemoveGroupMemberController removeGroupMemberController;


    private final AddRecommendedEventViewModel addRecommendedEventViewModel;
    private AddRecommendedEventController addRecommendedEventController;

    private final AddGroupEventViewModel addGroupEventViewModel;
    private AddGroupEventController addGroupEventController;

    private final DeleteGroupEventViewModel deleteGroupEventViewModel;
    private DeleteGroupEventController deleteGroupEventController;

    private final ExportCalendarViewModel exportCalendarViewModel;
    private ExportCalendarController exportCalendarController;

    private final ModifyGroupNameViewModel modifyGroupNameViewModel;
    private ModifyGroupNameController modifyGroupNameController;

    private String currentGroup; // Instance variable to store the current group name

    public GroupSettingsView(ViewManager viewManager, TimeslotSelectionViewModel timeslotSelectionViewModel, AddGroupMemberViewModel addGroupMemberViewModel,
                             RemoveGroupMemberViewModel removeGroupMemberViewModel, AddRecommendedEventViewModel addRecommendedEventViewModel,
                             AddGroupEventViewModel addGroupEventViewModel, DeleteGroupEventViewModel deleteGroupEventViewModel,
                             ExportCalendarViewModel exportCalendarViewModel, ModifyGroupNameViewModel modifyGroupNameViewModel) {
        this.addGroupMemberViewModel = addGroupMemberViewModel;
        addGroupMemberViewModel.addPropertyChangeListener(this);

        this.modifyGroupNameViewModel = modifyGroupNameViewModel;
        modifyGroupNameViewModel.addPropertyChangeListener(this);

        this.removeGroupMemberViewModel = removeGroupMemberViewModel;
        removeGroupMemberViewModel.addPropertyChangeListener(this);

        this.viewManager = viewManager;
        this.timeslotSelectionViewModel = timeslotSelectionViewModel;
        this.timeslotSelectionViewModel.addPropertyChangeListener(this);

        this.addRecommendedEventViewModel = addRecommendedEventViewModel;
        addRecommendedEventViewModel.addPropertyChangeListener(this);

        this.addGroupEventViewModel = addGroupEventViewModel;
        addGroupEventViewModel.addPropertyChangeListener(this);

        this.deleteGroupEventViewModel = deleteGroupEventViewModel;
        deleteGroupEventViewModel.addPropertyChangeListener(this);

        this.exportCalendarViewModel = exportCalendarViewModel;
        exportCalendarViewModel.addPropertyChangeListener(this);

        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(1280, 720));

        // Top Panel: Group Name and Settings
        JPanel topPanel = new JPanel(new BorderLayout());

// Centered Group Name Label
        groupNameLabel = new JLabel("Group's Settings", SwingConstants.CENTER);
        groupNameLabel.setFont(new Font("Arial", Font.BOLD, 24)); // Larger and bold font
        topPanel.add(groupNameLabel, BorderLayout.NORTH);

// Add a panel for modifying the group name
        JPanel modifyGroupNamePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcModify = new GridBagConstraints();
        gbcModify.insets = new Insets(10, 10, 10, 10);
        gbcModify.fill = GridBagConstraints.HORIZONTAL;

// Label for modify group name
        gbcModify.gridx = 0;
        gbcModify.gridy = 0;
        modifyGroupNamePanel.add(new JLabel("New Group Name:"), gbcModify);

// Text field for entering the new group name
        JTextField modifyGroupNameField = new JTextField();
        modifyGroupNameField.setPreferredSize(new Dimension(300, 30)); // Larger text field
        gbcModify.gridx = 1;
        modifyGroupNamePanel.add(modifyGroupNameField, gbcModify);

// Button to trigger the modify group name action
        gbcModify.gridx = 2;
        JButton modifyGroupNameButton = new JButton("Modify Group Name");
        modifyGroupNameButton.addActionListener(e -> {
            String newGroupName = modifyGroupNameField.getText().trim();
            if (newGroupName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Group name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                modifyGroupNameController.execute(currentGroup, newGroupName);
            }
        });
        modifyGroupNamePanel.add(modifyGroupNameButton, gbcModify);

// Add the Modify Group Name Panel below the title
        topPanel.add(modifyGroupNamePanel, BorderLayout.CENTER);

// Back Button on the Right
        JButton backButton = new JButton("BACK");
        backButton.addActionListener(e -> viewManager.switchToView("groupChatView"));
        topPanel.add(backButton, BorderLayout.EAST);

// Add the top panel to the layout
        this.add(topPanel, BorderLayout.NORTH);


        // Left Panel: Upcoming Events
        eventsPanel = new JPanel();
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
        JScrollPane eventsScrollPane = new JScrollPane(eventsPanel);
        eventsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        eventsScrollPane.setPreferredSize(new Dimension(600, 400));

        JPanel addEventPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Get Current Time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String currentTime = now.format(formatter);

        // Event Name Field
        gbc.gridx = 0;
        gbc.gridy = 0;
        addEventPanel.add(new JLabel("Name:"), gbc);
        eventNameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        addEventPanel.add(eventNameField, gbc);

        // Event Start Time
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        addEventPanel.add(new JLabel("Start:"), gbc);
        eventStartField = new JTextField(20);
        eventStartField.setToolTipText("Format: YYYY-MM-DD HH:MM");
        eventStartField.setText(currentTime); // Set predefined value to the current time
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        addEventPanel.add(eventStartField, gbc);

        // Event End Time
        gbc.gridx = 0;
        gbc.gridy = 2;
        addEventPanel.add(new JLabel("End:"), gbc);
        eventEndField = new JTextField(20);
        eventEndField.setToolTipText("Format: YYYY-MM-DD HH:MM");
        eventEndField.setText(currentTime); // Set predefined value to the current time
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        addEventPanel.add(eventEndField, gbc);

        // Add Event Button
        JButton addEventButton = new JButton("ADD EVENT");
        addEventButton.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        addEventPanel.add(addEventButton, gbc);

        // Export Calendar Button
        JButton exportCalendarButton = new JButton("EXPORT CALENDAR");
        exportCalendarButton.addActionListener(this);
        gbc.gridx = 2; // Place it next to the Add Event button
        gbc.gridwidth = 1;
        addEventPanel.add(exportCalendarButton, gbc);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Upcoming Events:"), BorderLayout.NORTH);
        leftPanel.add(eventsScrollPane, BorderLayout.CENTER);
        leftPanel.add(addEventPanel, BorderLayout.SOUTH);

        this.add(leftPanel, BorderLayout.WEST);


        // Right Panel: Recommended Event and Members
        JPanel rightPanel = new JPanel(new GridBagLayout());

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH; // Allow components to expand fully

        gbc.gridx = 0;
        gbc.gridy = 0;

//
//        // Add a panel for modifying the group name
//        JPanel modifyGroupNamePanel = new JPanel(new GridBagLayout());
//        GridBagConstraints gbcModify = new GridBagConstraints();
//        gbcModify.insets = new Insets(10, 10, 10, 10);
//        gbcModify.fill = GridBagConstraints.HORIZONTAL;
//
//        // Label for modify group name
//        gbcModify.gridx = 0;
//        gbcModify.gridy = 0;
//        modifyGroupNamePanel.add(new JLabel("New Group Name:"), gbcModify);
//
//        // Text field for entering the new group name
//        JTextField modifyGroupNameField = new JTextField();
//        modifyGroupNameField.setPreferredSize(new Dimension(300, 30)); // Set larger size
//        gbcModify.gridx = 1;
//        modifyGroupNamePanel.add(modifyGroupNameField, gbcModify);
//
//        // Button to trigger the modify group name action
//        JButton modifyGroupNameButton = new JButton("Modify Group Name");
//        modifyGroupNameButton.addActionListener(e -> {
//            String newGroupName = modifyGroupNameField.getText().trim();
//            if (newGroupName.isEmpty()) {
//                JOptionPane.showMessageDialog(this, "Group name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
//            } else {
//                modifyGroupNameController.execute(currentGroup, newGroupName);
//            }
//        });
//        gbcModify.gridx = 2;
//        modifyGroupNamePanel.add(modifyGroupNameButton, gbcModify);
//
//        // Add the panel to the top of the right panel
//        rightPanel.add(modifyGroupNamePanel, gbcModify);
//
//        // Update the layout and repaint
//        rightPanel.revalidate();
//        rightPanel.repaint();


// Prominent Recommendation Text
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel recommendationTextLabel = new JLabel("We think you should link up on!");
        recommendationTextLabel.setFont(new Font("Arial", Font.BOLD, 20)); // Bold and larger text
        rightPanel.add(recommendationTextLabel, gbc);

// Recommendation Event Label
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        recommendedEventLabel = new JLabel("No recommendation available.");
        rightPanel.add(recommendedEventLabel, gbc);

// Add Recommended Event Button
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        JButton addRecommendedEventButton = new JButton("Add Recommended Event");
        addRecommendedEventButton.addActionListener(this);
        rightPanel.add(addRecommendedEventButton, gbc);

// Current Members Label
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        rightPanel.add(new JLabel("Current Members:"), gbc);

// Current Members Scroll Pane
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weighty = 0.4; // Allocate 40% of vertical space
        membersPanel = new JPanel();
        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
        JScrollPane membersScrollPane = new JScrollPane(membersPanel);
        membersScrollPane.setPreferredSize(new Dimension(300, 200));
        rightPanel.add(membersScrollPane, gbc);

// Add Members Label
        gbc.gridy = 5;
        gbc.weighty = 0; // Reset weight for labels
        rightPanel.add(new JLabel("Add Members:"), gbc);

// Add Members Scroll Pane
        gbc.gridy = 6;
        gbc.weighty = 0.4; // Allocate 40% of vertical space
        addMembersPanel = new JPanel();
        addMembersPanel.setLayout(new BoxLayout(addMembersPanel, BoxLayout.Y_AXIS));
        JScrollPane addMembersScrollPane = new JScrollPane(addMembersPanel);
        addMembersScrollPane.setPreferredSize(new Dimension(300, 200));
        rightPanel.add(addMembersScrollPane, gbc);

// Add the right panel to the main layout
        this.add(rightPanel, BorderLayout.CENTER);

    }


    public void refreshGroupName() {
        // Retrieve current group from the GroupChatView
        currentGroup = ((GroupChatView) viewManager.getView("groupChatView")).getCurrentGroup();
        groupNameLabel.setText(currentGroup + "'s Settings");
    }

    public void refreshRecommendation() {
        System.out.println(currentGroup);
        timeslotSelectionController.execute(currentGroup, viewManager.getUser());
    }

    public void refreshEvents() {
        eventsPanel.removeAll();

        // Retrieve events for the current group
        List<List<String>> groupEvents = viewManager.getGroupEvents(currentGroup);
        // Set a fixed size for each event panel
        Dimension eventPanelSize = new Dimension(580, 100);

        // Create a container with vertical BoxLayout
        JPanel fixedSizeContainer = new JPanel();
        fixedSizeContainer.setLayout(new BoxLayout(fixedSizeContainer, BoxLayout.Y_AXIS));

        for (List<String> event : groupEvents) {
            String eventName = event.get(0);
            String startTime = event.get(1);
            String endTime = event.get(2);

            // Create event panel with a fixed size
            JPanel eventPanel = new JPanel(new BorderLayout());
            eventPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            eventPanel.setMaximumSize(eventPanelSize); // Set fixed maximum size
            eventPanel.setPreferredSize(eventPanelSize); // Set fixed preferred size

            // Event details label
            JLabel eventLabel = new JLabel("<html><b>" + eventName + "</b><br>Start: " + startTime + "<br>End: " + endTime + "</html>");
            eventPanel.add(eventLabel, BorderLayout.CENTER);

            // Remove button for each event
            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(e -> {
                deleteGroupEventController.execute(currentGroup, eventName, startTime, endTime);
            });
            eventPanel.add(removeButton, BorderLayout.EAST);

            // Add event panel to the container
            fixedSizeContainer.add(eventPanel);
        }

        // Ensure the container has the same layout as the main events panel
        eventsPanel.setLayout(new BorderLayout());
        eventsPanel.add(fixedSizeContainer, BorderLayout.NORTH);

        // Revalidate and repaint the panel
        eventsPanel.revalidate();
        eventsPanel.repaint();
    }


    public void refreshGroupMembers() {
        membersPanel.removeAll();
        List<List<String>> groupMembers = viewManager.getGroupMembers(currentGroup);

        for (List<String> member : groupMembers) {
            String memberName = member.get(0);
            String memberLanguage = member.get(1);

            JButton memberButton = new JButton(memberName + " (" + memberLanguage + ")");
            memberButton.addActionListener(e -> {
                removeGroupMemberController.execute(currentGroup, memberName);

            });
            membersPanel.add(memberButton);
        }
        membersPanel.revalidate();
        membersPanel.repaint();
    }

    public void refreshNewMembers() {
        addMembersPanel.removeAll();

        // Retrieve the current group's members
        List<List<String>> groupMembers = viewManager.getGroupMembers(currentGroup);

        // Retrieve the user's friends
        List<List<String>> userFriends = viewManager.getFriends();

        // Filter friends who are not already members of the group
        for (List<String> friend : userFriends) {
            String friendName = friend.get(0);
            String friendLanguage = friend.get(1);

            // Check if the friend is not in the group
            boolean isAlreadyMember = groupMembers.stream()
                    .anyMatch(member -> member.get(0).equals(friendName));

            if (!isAlreadyMember) {
                // Create a button for each friend
                JButton addFriendButton = new JButton(friendName + " (" + friendLanguage + ")");
                addFriendButton.addActionListener(e -> {
                    addGroupMemberController.execute(currentGroup, friendName);
                });
                addMembersPanel.add(addFriendButton);
            }
        }

        addMembersPanel.revalidate();
        addMembersPanel.repaint();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if ("ADD EVENT".equals(command)) {
            addGroupEventController.execute(currentGroup, eventNameField.getText(), eventStartField.getText(), eventEndField.getText());
        } else if ("Add Recommended Event".equals(command)) {
            addRecommendedEventController.excute(reccomendedEvent, currentGroup);
        } else if ("EXPORT CALENDAR".equals(command)) {
            exportCalendarController.exportCalendar(null, currentGroup);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("timeslotSuccess".equals(evt.getPropertyName())) {
            TimeslotSelectionState timeslotSelectionState = (TimeslotSelectionState) evt.getNewValue();
            Event event = timeslotSelectionState.getEvent();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String eventInfo = "<html><b>" + event.getEventName() + "</b><br>Start: " + event.getStartTime().format(formatter) +
                    "<br>End: " + event.getEndTime().format(formatter) + "</html>";
            recommendedEventLabel.setText(eventInfo);
            reccomendedEvent = event;
        } else if ("groupnameSuccess".equals(evt.getPropertyName())) {
            String newGroupName = (String) evt.getNewValue();
            currentGroup = newGroupName; // Update the current group name
            refreshRecommendation();
            refreshGroupMembers();
            refreshEvents();
            refreshNewMembers();
            refreshGroupName(); // Update the group name label
            JOptionPane.showMessageDialog(this, "Group name successfully updated to " + newGroupName + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if ("groupnameError".equals(evt.getPropertyName())) {
            String error = (String) evt.getNewValue();
            JOptionPane.showMessageDialog(this, "Failed to modify group name: " + error, "Error", JOptionPane.ERROR_MESSAGE);
        }
        else if ("addRecommendedSuccess".equals(evt.getPropertyName())) {
            AddRecommendedEventState addRecommendedEventState = (AddRecommendedEventState) evt.getNewValue();
            String eventName = addRecommendedEventState.getEvent();
            refreshRecommendation();
            refreshEvents();
            JOptionPane.showMessageDialog(this, "The LinkUp " + eventName + " Was Successfully Added", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if ("addGroupMemberSuccess".equals(evt.getPropertyName())) {
            AddGroupMemberState addGroupMemberState = (AddGroupMemberState) evt.getNewValue();
            String username = addGroupMemberState.getUsername();
            String groupname = addGroupMemberState.getGroupname();
            JOptionPane.showMessageDialog(this, "Friend " + username + " was successfully added to " + groupname + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshRecommendation();
            refreshEvents();
            refreshGroupMembers();
            refreshNewMembers();
        } else if ("removeGroupMemberSuccess".equals(evt.getPropertyName())) {
            RemoveGroupMemberState removeGroupMemberState = (RemoveGroupMemberState) evt.getNewValue();
            String username = removeGroupMemberState.getUsername();
            String groupname = removeGroupMemberState.getGroupname();
            JOptionPane.showMessageDialog(this, "Friend " + username + " was successfully removed from " + groupname + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshRecommendation();
            refreshEvents();
            refreshGroupMembers();
            refreshNewMembers();
        } else if ("addGroupEventSuccess".equals(evt.getPropertyName())) {
            AddGroupEventState addGroupEventState = (AddGroupEventState) evt.getNewValue();
            String eventName = addGroupEventState.getEventname();
            refreshRecommendation();
            refreshEvents();
            JOptionPane.showMessageDialog(this, "The event " + eventName + " was successfully added to " + currentGroup + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if ("addGroupEventError".equals(evt.getPropertyName())) {
            AddGroupEventState addGroupEventState = (AddGroupEventState) evt.getNewValue();
            String error = addGroupEventState.getError();
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        } else if ("deleteGroupEventSuccess".equals(evt.getPropertyName())) {
            DeleteGroupEventState deleteGroupEventState = (DeleteGroupEventState) evt.getNewValue();
            String eventName = deleteGroupEventState.getEventName();
            refreshRecommendation();
            refreshEvents();
            JOptionPane.showMessageDialog(this, "The event " + eventName + " was successfully removed from " + currentGroup + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else if ("ExportGroupCalendarSuccess".equals(evt.getPropertyName())) {
            JOptionPane.showMessageDialog(this, "Group calendar is successfully exported to the CalendarExports Directory of LinkUp", "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } else if ("exportCalendarFail".equals(evt.getPropertyName())) {
            ExportCalendarState exportCalendarState = (ExportCalendarState) evt.getNewValue();
            JOptionPane.showMessageDialog(this, exportCalendarState.getMessage(), "Export Fail", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getViewName() {
        return viewName;
    }

    public void setTimeslotSelectionController(TimeslotSelectionController timeslotSelectionController) {
        this.timeslotSelectionController = timeslotSelectionController;
    }

    public void setAddRecommendedEventController(AddRecommendedEventController addRecommendedEventController) {
        this.addRecommendedEventController = addRecommendedEventController;
    }
    
    public void setAddGroupMemberController(AddGroupMemberController addGroupMemberController) {
        this.addGroupMemberController = addGroupMemberController;
    }

    public void setRemoveGroupMemberController(RemoveGroupMemberController removeGroupMemberController) {
        this.removeGroupMemberController = removeGroupMemberController;
    }

    public void setAddGroupEventController(AddGroupEventController addGroupEventController) {
        this.addGroupEventController = addGroupEventController;
    }

    public void setDeleteGroupEventController(DeleteGroupEventController deleteGroupEventController) {
        this.deleteGroupEventController = deleteGroupEventController;
    }

    public void setExportCalendarController (ExportCalendarController exportCalendarController) {
        this.exportCalendarController = exportCalendarController;
    }


    // TODO Complete that method
    // setter
    // the groupname controller
    // its not a final instance.
    public void setModifyGroupNameController(ModifyGroupNameController modifyGroupNameController) {
        this.modifyGroupNameController = modifyGroupNameController;
    }
}

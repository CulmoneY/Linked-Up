package daos;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import entity.*;
import usecases.account_creation.AccountCreationUserDataAccessInterface;
import usecases.add_group_event.AddGroupEventDataAccessInterface;
import usecases.add_personal_event.AddPersonalEventDataAccessInterface;
import usecases.add_recommended_event.AddRecommendedEventDataAccessInterface;
import usecases.delete_group_event.DeleteGroupEventDataAccessInterface;
import usecases.delete_personal_event.DeletePersonalEventDataAccessInterface;
import usecases.login.LoginUserDataAccessInterface;
import usecases.add_friend.AddFriendDataAccessInterface;
import org.bson.Document;
import database.MongoDBConnection;
import usecases.create_group.CreateGroupDataAccessInterface;
import usecases.message.MessageDataAccessInterface;
import usecases.message_translation.MessageTranslationDataAccessInterface;
import usecases.change_language.ChangeLanguageDataAccessInterface;
import usecases.modify_group_name.ModifyGroupNameDataAccessInterface;
import usecases.remove_group_member.RemoveGroupMemberDataAccessInterface;
import usecases.timeslot_selection.TimeslotSelectionDataAccessInterface;
import usecases.remove_friend.RemoveFriendDataAccessInterface;
import usecases.add_group_member.AddGroupMemberDataAccessInterface;
import usecases.export_calendar.ExportCalendarDataAccessInterface;


import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class MongoDAO implements CreateGroupDataAccessInterface, AddPersonalEventDataAccessInterface,
        AccountCreationUserDataAccessInterface, LoginUserDataAccessInterface, MessageDataAccessInterface,
        MessageTranslationDataAccessInterface, AddFriendDataAccessInterface, ChangeLanguageDataAccessInterface,
        DeletePersonalEventDataAccessInterface, TimeslotSelectionDataAccessInterface, AddRecommendedEventDataAccessInterface,
        RemoveFriendDataAccessInterface, AddGroupMemberDataAccessInterface, RemoveGroupMemberDataAccessInterface,
        AddGroupEventDataAccessInterface, DeleteGroupEventDataAccessInterface, ExportCalendarDataAccessInterface, ModifyGroupNameDataAccessInterface {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> groupCollection;
    private final MongoCollection<Document> userCollection;
    private final MongoCollection<Document> translationsCollection;
    private final MessageFactory messageFactory;
    private final GroupFactory groupFactory;
    private final CalendarFactory calendarFactory;
    private final UserFactory userFactory;
    private final EventFactory eventFactory;

    public MongoDAO(GroupFactory groupFactory, MessageFactory messageFactory, CalendarFactory calendarFactory, UserFactory userFactory, EventFactory eventFactory) {
        this.mongoClient = MongoDBConnection.getMongoClient();
        this.database = mongoClient.getDatabase("LinkUp");
        this.groupCollection = database.getCollection("groups");
        this.userCollection = database.getCollection("users");
        this.translationsCollection = database.getCollection("translations");
        this.groupFactory = groupFactory;
        this.messageFactory = messageFactory;
        this.calendarFactory = calendarFactory;
        this.userFactory = userFactory;
        this.eventFactory = eventFactory;
    }

    @Override
    public boolean accountExists(String username) {
        Document query = new Document("username", username);
        return userCollection.find(query).first() != null;
    }

    public User getUser(String username) {
        Document query = new Document("username", username);
        Document userDoc = userCollection.find(query).first();
        if (userDoc == null) return null;

        String name = userDoc.getString("username");
        String password = userDoc.getString("password");
        String language = userDoc.getString("language");
        User user = userFactory.create(name, password, language);

        List<Document> friendDocs = (List<Document>) userDoc.get("friends");
        List<User> friends = deserializeFriends(friendDocs);
        user.setFriends(friends);

        Document calendarDoc = (Document) userDoc.get("calendar");
        Calendar calendar = deserializeCalendar(calendarDoc);
        user.setUserCalendar(calendar);

        List<Document> groupDocs = (List<Document>) userDoc.get("groups");
        List<Group> groups = deserializeGroups(groupDocs);
        for (Group group : groups)
            user.addGroup(group);
        return user;
    }


    public Group getGroup(String groupName) {

        Document query = new Document("groupname", groupName);
        Document groupDoc = groupCollection.find(query).first();
        if (groupDoc == null) return null;
        String groupname = groupDoc.getString("groupname");
        List<Document> userDocs = (List<Document>) groupDoc.get("users");
        List<User> users = deserializeUsers(userDocs);


        Document calendarDoc = (Document) groupDoc.get("calendar");
        Calendar calendar = deserializeCalendar(calendarDoc);

        List<Document> messageDoc = (List<Document>) groupDoc.get("messages");
        List<Message> messages = deserializeMessages(messageDoc);

        Group group = groupFactory.create(groupname, users);

        for (Message message : messages) {
            group.addMessage(message);
        }

        group.setGroupCalendar(calendar);

        return group;

    }

    // TODO :
    // CODE THIS METHOD
    // ASSUMES THE NEW GROUP NAME IS NOT TAKEN.
    @Override
    public void modifyGroupName(String oldGroupName, String newGroupName) {
        // Step 1: Query the database for the group document with the old group name
        Document query = new Document("groupname", oldGroupName);
        Document groupDoc = groupCollection.find(query).first();

        // Step 2: Validate the existence of the old group
        if (groupDoc == null) {
            // test for me
            // System.out.println("Group with name '" + oldGroupName + "' does not exist.");
            return; // Exit if the group does not exist
        }

        // Step 3: Check if the new group name is already taken
        Document newGroupQuery = new Document("groupname", newGroupName);
        if (groupCollection.find(newGroupQuery).first() != null) {
            System.out.println("Group with name '" + newGroupName + "' already exists.");
            return; // Exit if the new group name is already taken
        }

        // Step 4: Retrieve and modify the group document
        List<Document> userDocs = (List<Document>) groupDoc.get("users");
        List<Document> messageDocs = (List<Document>) groupDoc.get("messages");
        Document calendarDoc = (Document) groupDoc.get("calendar");

        // Update the group name in the database
        Document update = new Document("$set", new Document("groupname", newGroupName));
        groupCollection.updateOne(query, update);

        // Step 5: Update group name in all associated user documents
        for (Document userDoc : userDocs) {
            String username = userDoc.getString("username");
            Document userQuery = new Document("username", username);
            Document userUpdate = userCollection.find(userQuery).first();

            if (userUpdate != null) {
                List<Document> userGroups = (List<Document>) userUpdate.get("groups");
                if (userGroups != null) {
                    for (Document group : userGroups) {
                        if (group.getString("groupName").equals(oldGroupName)) {
                            group.put("groupName", newGroupName); // Update group name
                        }
                    }
                    // Push updated groups list back to the database
                    Document groupUpdate = new Document("$set", new Document("groups", userGroups));
                    userCollection.updateOne(userQuery, groupUpdate);
                }
            }
        }

        // Step 6: Log the update for auditing purposes
        System.out.println("Successfully updated group name from '" + oldGroupName + "' to '" + newGroupName + "'.");
        System.out.println("Updated group document details:");
        System.out.println(groupDoc.toJson());

        // Step 7: Handle potential inconsistencies or further updates
        if (messageDocs != null) {
            for (Document messageDoc : messageDocs) {
                System.out.println("Message: " + messageDoc.toJson());
            }
        }

        if (calendarDoc != null) {
            System.out.println("Calendar events:");
            List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
            if (eventDocs != null) {
                for (Document eventDoc : eventDocs) {
                    System.out.println("Event: " + eventDoc.toJson());
                }
            }
        }

        // Final confirmation
        System.out.println("Group renaming process completed for group: " + oldGroupName);
    }


    @Override
    public boolean groupExist(String groupName) {
        Document query = new Document("groupname", groupName);
        return groupCollection.find(query).first() != null;
    }

    @Override
    public void saveUser(User user) {
        Document userDoc = new Document("username", user.getName())
                .append("password", user.getPassword())
                .append("language", user.getLanguage())
                .append("friends", serializeFriends(user.getFriends()))
                .append("calendar", serializeCalendar(user.getUserCalendar()))
                .append("groups", serializeGroups(user.getGroups()));
        userCollection.insertOne(userDoc);
    }


    public void saveGroup(Group group) {
        Document groupDoc = new Document("groupname", group.getName())
                .append("messages", serializeMessages(group.getMessages()))
                .append("users", serializeUsers(group.getUsers()))
                .append("calendar", serializeCalendar(group.getGroupCalendar()));

        groupCollection.insertOne(groupDoc);

    }

    @Override
    public List<User> groupMembersToUsers(List<String> groupMembers) {
        List<User> users = new ArrayList<>();
        for (String groupMember : groupMembers) {
            User groupMemberUser = getUser(groupMember);
            users.add(groupMemberUser);
        }
        return users;
    }

    public void deleteGroup(String groupName) {
        Document query = new Document("groupname", groupName);
        groupCollection.deleteOne(query);
    }


    public void deleteUser(String username) {
        Document query = new Document("username", username);
        userCollection.deleteOne(query);
    }

    private List<Document> serializeMessages(List<Message> messages) {
        List<Document> messageDocs = new ArrayList<>();
        for (Message message : messages) {
            Document messageDoc = new Document("message", message.getMessage())
                    .append("sender", message.getSender().getName())
                    .append("time", message.getTime().toString())
                    .append("language", message.getLanguage());
            messageDocs.add(messageDoc);
        }
        return messageDocs;
    }

    private List<Document> serializeGroups(List<Group> groups) {
        List<Document> groupDocs = new ArrayList<>();
        for (Group group : groups) {
            groupDocs.add(new Document("groupName", group.getName()));
        }
        return groupDocs;
    }

    private List<Document> serializeFriends(List<User> friends) {
        List<Document> friendDocs = new ArrayList<>();
        for (User friend : friends) {
            friendDocs.add(new Document("username", friend.getName())
                    .append("language", friend.getLanguage()));
        }
        return friendDocs;
    }


    private List<Document> serializeUsers(List<User> users) {
        List<Document> userDocs = new ArrayList<>();
        for (User user : users) {
            Document userDoc = new Document("username", user.getName())
                    .append("language", user.getLanguage());
            userDocs.add(userDoc);
        }
        return userDocs;
    }

    private Document serializeCalendar(Calendar calendar) {
        List<Document> eventDocs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Event event : calendar.getEvents()) {
            eventDocs.add(new Document("eventName", event.getEventName())
                    .append("startTime", event.getStartTime().format(formatter))
                    .append("endTime", event.getEndTime().format(formatter)));
        }
        return new Document("name", calendar.getName())
                .append("events", eventDocs);
    }

    private List<Message> deserializeMessages(List<Document> messageDocs) {
        List<Message> messages = new ArrayList<>();
        for (Document messageDoc : messageDocs) {
            String textmessage = messageDoc.getString("message");
            User sender = userFactory.create(messageDoc.getString("sender"), "defaultpassword", "defaultlanguage");
            LocalDateTime time = LocalDateTime.parse(messageDoc.getString("time"));
            String language = messageDoc.getString("language");

            Message message = messageFactory.create(sender, textmessage, language);
            message.setTime(time);
            messages.add(message);
        }
        return messages;
    }

    private List<User> deserializeUsers(List<Document> userDocs) {
        List<User> users = new ArrayList<>();
        for (Document userDoc : userDocs) {
            String username = userDoc.getString("username");
            String language = userDoc.getString("language");
            User user = userFactory.create(username, "defaultpassword", language);
            users.add(user);
        }
        return users;
    }

    private Calendar deserializeCalendar(Document calendarDoc) {
        if (calendarDoc == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String name = calendarDoc.getString("name");
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        List<Event> events = new ArrayList<>();
        for (Document eventDoc : eventDocs) {
            Event event = eventFactory.create(
                    eventDoc.getString("eventName"),
                    LocalDateTime.parse(eventDoc.getString("startTime"), formatter),
                    LocalDateTime.parse(eventDoc.getString("endTime"), formatter),
                    true
            );
            events.add(event);
        }
        Calendar calendar = calendarFactory.create(name);
        calendar.setEvents(events);
        return calendar;
    }


    private List<Group> deserializeGroups(List<Document> groupDocs) {
        List<Group> groups = new ArrayList<>();
        if (groupDocs == null || groupDocs.isEmpty()) {
            return groups;
        }
        for (Document groupDoc : groupDocs) {
            String groupName = groupDoc.getString("groupName");
            if (groupName != null) {
                Group group = getGroup(groupName); // Fetch the full group details using getGroup()
                if (group != null) {
                    groups.add(group); // Add the group to the list if it exists
                }
            }
        }
        return groups;
    }

    private List<User> deserializeFriends(List<Document> friendDocs) {
        List<User> friends = new ArrayList<>();
        for (Document friendDoc : friendDocs) {
            // Mock class for the friend
            User friend = userFactory.create(friendDoc.getString("username"), "defaultpassword", friendDoc.getString("language"));
            friends.add(friend);
        }
        return friends;
    }

    @Override
    public void addEvent(User user, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Document query = new Document("username", user.getName());
        Document userDoc = userCollection.find(query).first();

        if (userDoc == null) return;

        Document calendarDoc = (Document) userDoc.get("calendar");
        if (calendarDoc == null) return;

        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        if (eventDocs == null) {
            eventDocs = new ArrayList<>();
        }

        Document newEventDoc = new Document("eventName", event.getEventName())
                .append("startTime", event.getStartTime().format(formatter))
                .append("endTime", event.getEndTime().format(formatter));
        eventDocs.add(newEventDoc);

        calendarDoc.put("events", eventDocs);

        Document update = new Document("$set", new Document("calendar", calendarDoc));
        userCollection.updateOne(query, update);
    }


    @Override
    public void addGroupToUser(String username, String groupname) {
        // Step 1: Query the database for the user document
        Document query = new Document("username", username);
        Document userDoc = userCollection.find(query).first();

        // Step 2: Check if the user exists in the database
        if (userDoc == null) {
            return; // User not found, exit
        }

        // Step 3: Get the current list of groups for the user, or initialize it if missing
        List<Document> groupDocs = (List<Document>) userDoc.get("groups");
        if (groupDocs == null) {
            groupDocs = new ArrayList<>();
        }

        // Step 4: Check if the group is already associated with the user
        for (Document groupDoc : groupDocs) {
            if (groupDoc.getString("groupName").equals(groupname)) {
                return; // Group already exists, no need to proceed
            }
        }

        // Step 5: Add the new group to the user's group list
        Document newGroupDoc = new Document("groupName", groupname);
        groupDocs.add(newGroupDoc);

        // Step 6: Update the user document in the database
        Document update = new Document("$set", new Document("groups", groupDocs));
        userCollection.updateOne(query, update);
    }

    @Override
    public void addGroupToUser(String username, Group group) {
        // Step 1: Query the database for the user document
        Document query = new Document("username", username);
        Document userDoc = userCollection.find(query).first();

        // Step 2: Check if the user exists in the database
        if (userDoc == null) {
            return; // User not found, exit
        }

        // Step 3: Get the current list of groups for the user, or initialize it if missing
        List<Document> groupDocs = (List<Document>) userDoc.get("groups");
        if (groupDocs == null) {
            groupDocs = new ArrayList<>();
        }

        // Step 4: Check if the group is already associated with the user
        for (Document groupDoc : groupDocs) {
            if (groupDoc.getString("groupName").equals(group.getName())) {
                return; // Group already exists, no need to proceed
            }
        }

        // Step 5: Add the new group to the user's group list
        Document newGroupDoc = new Document("groupName", group.getName());
        groupDocs.add(newGroupDoc);

        // Step 6: Update the user document in the database
        Document update = new Document("$set", new Document("groups", groupDocs));
        userCollection.updateOne(query, update);
    }

    @Override
    public void addUserToGroup(String groupname, String username) {
        // Fetch the group document
        Document groupQuery = new Document("groupname", groupname);
        Document groupDoc = groupCollection.find(groupQuery).first();

        // gets the user document
        Document userQuery = new Document("username", username);
        Document userDoc = userCollection.find(userQuery).first();

        // add the user to the group's user list
        Document newUserDoc = new Document("username", username)
                .append("language", userDoc.getString("language")); // Include additional fields if needed
        List<Document> userDocs = (List<Document>) groupDoc.get("users");
        userDocs.add(newUserDoc);

        // get usesr events to add them to the group events.
        Document userCalendarDoc = (Document) userDoc.get("calendar");
        List<Document> userEventDocs = userCalendarDoc != null ? (List<Document>) userCalendarDoc.get("events") : new ArrayList<>();

        Document groupCalendarDoc = (Document) groupDoc.get("calendar");
        List<Document> groupEventDocs = groupCalendarDoc != null ? (List<Document>) groupCalendarDoc.get("events") : new ArrayList<>();

        if (userEventDocs != null) {
            groupEventDocs.addAll(userEventDocs); // Add all user events to the group events
        }

        // to update the groups calendar.
        if (groupCalendarDoc == null) {
            groupCalendarDoc = new Document("name", groupname + "'s Calendar")
                    .append("events", groupEventDocs);
        } else {
            groupCalendarDoc.put("events", groupEventDocs);
        }

        // adds the newly added users events to the already existing group events .
        Document update = new Document("$set", new Document("users", userDocs)
                .append("calendar", groupCalendarDoc));
        groupCollection.updateOne(groupQuery, update);
    }



    @Override
    public void updateGroupMessages(Message message, String groupName) {
        Document query = new Document("groupname", groupName);
        Document groupDoc = groupCollection.find(query).first();

        if (groupDoc == null) {
            return;
        }

        List<Document> messageDocs = (List<Document>) groupDoc.get("messages");
        if (messageDocs == null) {
            messageDocs = new ArrayList<>();
        }

        Document newMessageDoc = new Document("message", message.getMessage())
                .append("sender", message.getSender().getName())
                .append("time", message.getTime().toString())
                .append("language", message.getLanguage());

        messageDocs.add(newMessageDoc);

        Document update = new Document("$set", new Document("messages", messageDocs));
        groupCollection.updateOne(query, update);
    }

    @Override
    public List<Message> getMessagesByGroup(String groupName) {
        Document query = new Document("groupname", groupName);
        Document groupDoc = groupCollection.find(query).first();
        List<Message> messages = new ArrayList<>();
        if (groupDoc != null) {
            List<Document> messageDocs = (List<Document>) groupDoc.get("messages");
            messageDocs.sort(Comparator.comparing(d -> d.getString("time"))); // Sort by time ascending
            for (Document doc : messageDocs) {
                User sender = getUser(doc.getString("sender"));
                messages.add(messageFactory.create(
                        sender,
                        doc.getString("message"),
                        doc.getString("language")
                ));
            }
        }
        return messages;
    }

    // Translation DAO methods
    @Override
    public String getTranslatedMessage(String message, String targetLanguage, String groupName) {
        Document groupQuery = new Document("groupname", groupName);
        Document projection = new Document("messages", new Document("$elemMatch", new Document("message", message)));
        Document groupDoc = groupCollection.find(groupQuery).projection(projection).first();
        if (groupDoc == null) {
            return null;
        }
        List<Document> messages = (List<Document>) groupDoc.get("messages");
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        Document messageDoc = messages.get(0); // Should only contain the matched message due to $elemMatch
        Document translations = (Document) messageDoc.get("translations");
        if (translations == null) {
            return null;
        }
        String translatedMessage = translations.getString(targetLanguage);
        if (translatedMessage == null) {
        }
        return translatedMessage;
    }

    @Override
    public void saveTranslation(String message, String targetLanguage, String translatedMessage, String groupName) {
        Document groupFilter = new Document("groupname", groupName);
        Document update = new Document("$set", new Document("messages.$[msg].translations." + targetLanguage, translatedMessage));
        List<Document> arrayFilters = List.of(
                new Document("msg.message", message) // Match the message
        );
        UpdateOptions options = new UpdateOptions().arrayFilters(arrayFilters);
        groupCollection.updateOne(groupFilter, update, options);
    }

    @Override
    public String translateMessage(String message, String targetLanguage) {
        String authkey = null;

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/config.properties")) {
            properties.load(input);
            authkey = properties.getProperty("deepl.apikey");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        Translator translator = new Translator(authkey);
        try {
            TextResult result = translator.translateText(message, null, targetLanguage);
            return result.getText();
        } catch (DeepLException e) {
            System.err.println("DeepLException: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("InterruptedException" + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e.getMessage());
        }
        return "";
    }

    @Override
    public User addFriend(String username, String friendUsername) {
        // Query the user document for the given username
        Document userQuery = new Document("username", username);
        Document userDoc = userCollection.find(userQuery).first();

        // Query the user document for the given friendUsername
        Document friendQuery = new Document("username", friendUsername);
        Document friendDoc = userCollection.find(friendQuery).first();

        // Retrieve and update the friends list for the user
        List<Document> userFriends = (List<Document>) userDoc.get("friends");
        if (userFriends == null) {
            userFriends = new ArrayList<>();
        }
        userFriends.add(new Document("username", friendUsername).append("language", friendDoc.getString("language")));
        userCollection.updateOne(userQuery, new Document("$set", new Document("friends", userFriends)));

        // Retrieve and update the friends list for the friend
        List<Document> friendFriends = (List<Document>) friendDoc.get("friends");
        if (friendFriends == null) {
            friendFriends = new ArrayList<>();
        }
        friendFriends.add(new Document("username", username).append("language", userDoc.getString("language")));
        userCollection.updateOne(friendQuery, new Document("$set", new Document("friends", friendFriends)));

        // Recreate the friend User object from friendDoc
        String name = friendDoc.getString("username");
        String password = friendDoc.getString("password");
        String language = friendDoc.getString("language");
        User friendUser = userFactory.create(name, password, language);

        // Add the friend’s groups, calendar, and friends to the object
        List<Document> friendGroupDocs = (List<Document>) friendDoc.get("groups");
        List<Group> friendGroups = deserializeGroups(friendGroupDocs);
        for (Group group : friendGroups) {
            friendUser.addGroup(group);
        }

        Document friendCalendarDoc = (Document) friendDoc.get("calendar");
        Calendar friendCalendar = deserializeCalendar(friendCalendarDoc);
        friendUser.setUserCalendar(friendCalendar);

        List<Document> friendFriendDocs = (List<Document>) friendDoc.get("friends");
        List<User> friendFriendsList = deserializeFriends(friendFriendDocs);
        friendUser.setFriends(friendFriendsList);

        return friendUser;
    }

    @Override
    public boolean isFriend(String username, String friendUsername) {
        Document query = new Document("username", username);
        Document userDoc = userCollection.find(query).first();

        if (userDoc == null) {
            return false;
        }

        List<Document> friendDocs = (List<Document>) userDoc.get("friends");
        if (friendDocs == null || friendDocs.isEmpty()) {
            return false;
        }

        for (Document friendDoc : friendDocs) {
            if (friendUsername.equals(friendDoc.getString("username"))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void changeUserLanguage(String username, String language) {
        Document query = new Document("username", username);
        Document update = new Document("$set", new Document("language", language));
        userCollection.updateOne(query, update);

    }

    @Override
    public void removeUserEvent(String username, String eventName, String startTime, String endTime) {
        Document query = new Document("username", username);
        Document userDoc = userCollection.find(query).first();
        Document calendarDoc = (Document) userDoc.get("calendar");
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        Document eventToRemove = null;
        for (Document eventDoc : eventDocs) {
            if (eventDoc.getString("eventName").equals(eventName)
                    && eventDoc.getString("startTime").equals(startTime)
                    && eventDoc.getString("endTime").equals(endTime)) {
                eventToRemove = eventDoc;
                break;
            }
        }

        eventDocs.remove(eventToRemove);
        calendarDoc.put("events", eventDocs);
        Document update = new Document("$set", new Document("calendar", calendarDoc));
        userCollection.updateOne(query, update);
    }

    @Override
    public void removeFriend(String userId, String friendId) {
        // Remove friend from user's friend list
        Document userQuery = new Document("username", userId);
        Document pullFriendFromUser = new Document("$pull", new Document("friends", new Document("username", friendId)));
        userCollection.updateOne(userQuery, pullFriendFromUser);

        // Remove user from friend's friend list
        Document friendQuery = new Document("username", friendId);
        Document pullUserFromFriend = new Document("$pull", new Document("friends", new Document("username", userId)));
        userCollection.updateOne(friendQuery, pullUserFromFriend);
    }

    @Override
    public void addEventToGroup(Event event, String groupName) {
        // Step 1: Query the group document using the groupName
        Document query = new Document("groupname", groupName);
        Document groupDoc = groupCollection.find(query).first();

        // Step 2: Access the calendar document in the group
        Document calendarDoc = (Document) groupDoc.get("calendar");


        // Step 3: Get the existing list of events or initialize a new one
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        if (eventDocs == null) {
            eventDocs = new ArrayList<>();
        }

        // Step 4: Create a new event document
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Document newEventDoc = new Document("eventName", event.getEventName())
                .append("startTime", event.getStartTime().format(formatter))
                .append("endTime", event.getEndTime().format(formatter));
        eventDocs.add(newEventDoc);

        // Step 5: Update the calendar document with the new event
        calendarDoc.put("events", eventDocs);

        // Step 6: Update the group document in the database
        Document update = new Document("$set", new Document("calendar", calendarDoc));
        groupCollection.updateOne(query, update);
    }

    public void removeGroupMember(String groupname, String username) {
        // Remove group from user groups list.
        Document query = new Document("username", username);
        Document pullFriendFromUser = new Document("$pull", new Document("groups", new Document("groupName", groupname)));
        userCollection.updateOne(query, pullFriendFromUser);

        // Remove user from group members list.
        Document groupQuery = new Document("groupname", groupname);
        Document pullfromGroup = new Document("$pull", new Document("users", new Document("username", username)));
        groupCollection.updateOne(groupQuery, pullfromGroup);
    }

    @Override
    public void addGroupEvent(String groupname, Event event) {
        // Step 1: Query the group document using the groupname
        Document groupQuery = new Document("groupname", groupname);
        Document groupDoc = groupCollection.find(groupQuery).first();

        if (groupDoc == null) {
            return; // Group not found; nothing to do
        }

        // Step 2: Access the calendar document within the group
        Document calendarDoc = (Document) groupDoc.get("calendar");

        // Step 3: Access or initialize the events list
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        if (eventDocs == null) {
            eventDocs = new ArrayList<>();
        }

        // Step 4: Create a new event document and add it to the events list
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Document newEventDoc = new Document("eventName", event.getEventName())
                .append("startTime", event.getStartTime().format(formatter))
                .append("endTime", event.getEndTime().format(formatter));
        eventDocs.add(newEventDoc);

        // Step 5: Update the calendar document with the new events list
        calendarDoc.put("events", eventDocs);

        // Step 6: Update the group document in the database
        Document update = new Document("$set", new Document("calendar", calendarDoc));
        groupCollection.updateOne(groupQuery, update);
    }

    @Override
    public void removeGroupEvent(String groupname, String eventname, String startTime, String endTime) {
        // Step 1: Query the group document using the groupname
        Document groupQuery = new Document("groupname", groupname);
        Document groupDoc = groupCollection.find(groupQuery).first();

        // Step 2: Access the calendar document in the group
        Document calendarDoc = (Document) groupDoc.get("calendar");

        // Step 3: Retrieve the list of events from the calendar
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        if (eventDocs == null) {
            return; // No events found; nothing to do
        }

        // Step 4: Find the event to remove based on the eventname, startTime, and endTime
        Document eventToRemove = null;
        for (Document eventDoc : eventDocs) {
            if (eventDoc.getString("eventName").equals(eventname) &&
                    eventDoc.getString("startTime").equals(startTime) &&
                    eventDoc.getString("endTime").equals(endTime)) {
                eventToRemove = eventDoc;
                break;
            }
        }

        // Step 5: Remove the event from the list if it was found
        if (eventToRemove != null) {
            eventDocs.remove(eventToRemove);
        } else {
            return; // Event not found; nothing to do
        }

        // Step 6: Update the calendar document with the modified list of events
        calendarDoc.put("events", eventDocs);

        // Step 7: Update the group document in the database
        Document update = new Document("$set", new Document("calendar", calendarDoc));
        groupCollection.updateOne(groupQuery, update);
    }

}

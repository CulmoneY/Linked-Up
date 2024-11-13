package daos;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import entity.*;
import usecases.add_personal_event.addPersonalEventDataAccessInterface;
import org.bson.Document;
import usecases.account_creation.AccountCreationUserDataAccessInterface;
import database.MongoDBConnection;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO implements addPersonalEventDataAccessInterface{
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> groupCollection;
    private final MessageFactory messageFactory;
    private final GroupFactory groupFactory;
    private final CalendarFactory calendarFactory;
    private final UserFactory userFactory;
    private final EventFactory eventFactory;

    public GroupDAO(GroupFactory groupFactory, MessageFactory messageFactory, CalendarFactory calendarFactory, UserFactory userFactory, EventFactory eventFactory) {
        this.mongoClient = MongoDBConnection.getMongoClient();
        this.database = mongoClient.getDatabase("LinkUp");
        this.groupCollection = database.getCollection("groups");
        this.groupFactory = groupFactory;
        this.messageFactory = messageFactory;
        this.calendarFactory = calendarFactory;
        this.userFactory = userFactory;
        this.eventFactory = eventFactory;
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



    public boolean groupExist(String groupName) {
        Document query = new Document("groupname", groupName);
        return groupCollection.find(query).first() != null;
    }

    public void saveGroup(Group group)  {
        Document groupDoc = new Document("groupname", group.getName())
                .append("messages", serializeMessages(group.getMessages()))
                .append("users", serializeUsers(group.getUsers()))
                .append("calendar", serializeCalendar(group.getGroupCalendar()));

        groupCollection.insertOne(groupDoc);
    }

    public void deleteGroup(String groupName) {
        Document query = new Document("groupname", groupName);
        groupCollection.deleteOne(query);
    }

    private List<Document> serializeMessages (List<Message> messages) {
        List<Document> messageDocs = new ArrayList<>();
        for (Message message : messages) {
            Document messageDoc = new Document("message", message.getMessage())
                    .append("sender", message.getSender().getName())
                    .append("time", message.getTime().toString());
            messageDocs.add(messageDoc);
        }
        return messageDocs;
    }

    private List<Document> serializeUsers (List<User> users) {
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
        for (Event event : calendar.getEvents()) {
            eventDocs.add(new Document("eventName", event.getEventName())
                    .append("startTime", event.getStartTime().toString())
                    .append("endTime", event.getEndTime().toString()));
        }
        return new Document("name", calendar.getName())
                .append("events", eventDocs);
    }

    private List<Message> deserializeMessages (List<Document> messageDocs) {
        List<Message> messages = new ArrayList<>();
        for (Document messageDoc : messageDocs) {
            String textmessage = messageDoc.getString("message");
            User sender = userFactory.create(messageDoc.getString("sender"), "defaultpassword", "defaultlanguage");
            LocalDateTime time = LocalDateTime.parse(messageDoc.getString("time"));
            Message message = messageFactory.create(sender, textmessage);
            message.setTime(time);
            messages.add(message);
        }
        return messages;
    }

    private List<User> deserializeUsers (List<Document> userDocs) {
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

        String name = calendarDoc.getString("name");
        List<Document> eventDocs = (List<Document>) calendarDoc.get("events");
        List<Event> events = new ArrayList<>();
        for (Document eventDoc : eventDocs) {
            Event event = eventFactory.create(
                    eventDoc.getString("eventName"),
                    LocalDateTime.parse(eventDoc.getString("startTime")),
                    LocalDateTime.parse(eventDoc.getString("endTime")),
                    true
            );
            events.add(event);
        }
        Calendar calendar = calendarFactory.create(name);
        calendar.setEvents(events);
        return calendar;
    }

    @Override
    public void addEvent(User user, Event event) {

    }
}

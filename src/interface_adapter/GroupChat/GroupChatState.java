package interface_adapter.GroupChat;

import entity.Message;

public class GroupChatState {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}

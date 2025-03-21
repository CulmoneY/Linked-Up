package app;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        final AppBuilder appBuilder = new AppBuilder();
        final JFrame application = appBuilder
                .addMessageUseCase()
                .addAccountCreationUseCase()
                .addLoginUseCase()
                .addPersonalEventUseCase()
                .addFriendUseCase()
                .addGroupMemberUseCase()
                .addRemoveGroupMemberUseCase()
                .addChangeLanguageUseCase()
                .addDeletePersonalEventUserCase()
                .addCreateGroupUseCase()
                .addTimeslotSelectionUseCase()
                .addExportCalendarUserUseCase()
                .addExportCalendarGroupUseCase()
                .addRemoveFriendUseCase()
                .addAddRecommendedEventUseCase()
                .addGroupEventUseCase()
                .addDeleteGroupEventUseCase()
                .addModifyGroupNameUseCase()
                .build();

        application.pack();
        application.setVisible(true);
    }
}

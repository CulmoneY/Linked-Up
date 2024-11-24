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
                .addChangeLanguageUseCase()
                .build();

        application.pack();
        application.setVisible(true);
    }
}

package app;

import daos.GroupDAO;
import daos.UserDAO;
import entity.*;
import interface_adapter.Login.LoginViewModel;
import interface_adapter.ViewManagerModel;
import interface_adapter.GroupChat.GroupChatViewModel;
import views.*;
//import views.LoginView;
//import views.ViewManager;
import usecases.account_creation.*;
import usecases.login.*;
import interface_adapter.Login.*;

import javax.swing.*;
import java.awt.*;

import interface_adapter.AccountCreation.*;

public class AppBuilder {
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final UserFactory userFactory = new CommonUserFactory();
    private final CalendarFactory calendarFactory = new CommonCalendarFactory();
    private final EventFactory eventFactory = new CommonEventFactory();
    private final MessageFactory messageFactory = new CommonMessageFactory();
    private final GroupFactory groupFactory = new CommonGroupFactory();
    private final ViewManagerModel viewManagerModel = new ViewManagerModel();
    private final ViewManager viewManager = new ViewManager(cardPanel, cardLayout, viewManagerModel);
    private final UserDAO userDAO = new UserDAO(userFactory, eventFactory, calendarFactory);
    private final GroupDAO groupDAO = new GroupDAO(groupFactory, messageFactory, calendarFactory, userFactory, eventFactory);
    private AccountCreationView accountCreationView;
    private AccountCreationViewModel accountCreationViewModel;
    private LoginView loginView;
    private LoginViewModel loginViewModel;
    private GroupChatView groupChatView;
    private GroupChatViewModel groupChatViewModel;

    public AppBuilder() {
        cardPanel.setLayout(cardLayout);
    }

    public AppBuilder addAccountCreationView() {
        accountCreationViewModel = new AccountCreationViewModel();
        accountCreationView = new AccountCreationView(accountCreationViewModel, viewManager); // Pass ViewManager
        viewManager.addView(accountCreationView.getViewName(), accountCreationView);
        return this;
    }

    public AppBuilder addAccountCreationUseCase() {
        final AccountCreationOutputBoundary accountCreationOutputBoundary = new AccountCreationPresenter(accountCreationViewModel, viewManagerModel);
        final AccountCreationInputBoundary userAccountCreationInteractor = new AccountCreationInteractor(userDAO, accountCreationOutputBoundary, userFactory);
        final AccountCreationController controller = new AccountCreationController(userAccountCreationInteractor);
        accountCreationView.setAccountCreationController(controller);
        return this;
    }

    public AppBuilder addLoginView() {
        loginViewModel = new LoginViewModel();
        loginView = new LoginView(loginViewModel, viewManager); // Pass ViewManager
        viewManager.addView(loginView.getViewName(), loginView);
        return this;
    }

    public AppBuilder addGroupChatView() {
        groupChatViewModel = new GroupChatViewModel();
        groupChatView = new GroupChatView(groupChatViewModel, viewManager);
        viewManager.addView(groupChatView.getViewName(), groupChatView);
        return this;
    }

//    public AppBuilder addLoginUseCase() {
//        final LoginOutputBoundary loginOutputBoundary = new LoginPresenter(viewManagerModel);
//        final LoginInputBoundary loginInteractor = new LoginInteractor(userDAO, loginOutputBoundary);
//        final LoginController loginController = new LoginController(loginInteractor);
//        loginView.setLoginController(loginController);
//        return this;
//    }

    public JFrame build() {
        final JFrame application = new JFrame("Linkup");
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        application.setSize(1280, 720); // Fixed window size
        application.setLocationRelativeTo(null); // Center the window
        application.add(cardPanel);

        // Set initial view to AccountCreationView
        viewManagerModel.setState("AccountCreationView");
        viewManagerModel.firePropertyChanged();

        return application;
    }
}

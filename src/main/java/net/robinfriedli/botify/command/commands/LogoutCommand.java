package net.robinfriedli.botify.command.commands;

import net.dv8tion.jda.core.entities.User;
import net.robinfriedli.botify.Botify;
import net.robinfriedli.botify.command.AbstractCommand;
import net.robinfriedli.botify.command.CommandContext;
import net.robinfriedli.botify.command.CommandManager;
import net.robinfriedli.botify.entities.xml.CommandContribution;
import net.robinfriedli.botify.login.Login;
import net.robinfriedli.botify.login.LoginManager;

public class LogoutCommand extends AbstractCommand {

    public LogoutCommand(CommandContribution commandContribution, CommandContext context, CommandManager commandManager, String commandString, String identifier, String description) {
        super(commandContribution, context, commandManager, commandString, false, identifier, description, Category.SPOTIFY);
    }

    @Override
    public void doRun() {
        LoginManager loginManager = Botify.get().getLoginManager();
        User user = getContext().getUser();
        Login login = loginManager.requireLoginForUser(user);
        login.cancel();
        loginManager.removeLogin(user);
    }

    @Override
    public void onSuccess() {
        sendSuccess("User " + getContext().getUser().getName() + " logged out from Spotify.");
    }
}

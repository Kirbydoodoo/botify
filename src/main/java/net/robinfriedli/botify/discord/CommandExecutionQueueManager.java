package net.robinfriedli.botify.discord;

import net.dv8tion.jda.core.entities.Guild;
import net.robinfriedli.botify.concurrent.ThreadExecutionQueue;
import net.robinfriedli.botify.util.ISnowflakeMap;

public class CommandExecutionQueueManager {

    private final ISnowflakeMap<ThreadExecutionQueue> guildExecutionQueues;

    public CommandExecutionQueueManager() {
        this.guildExecutionQueues = new ISnowflakeMap<>();
    }

    public void addGuild(Guild guild) {
        guildExecutionQueues.put(guild, new ThreadExecutionQueue(3));
    }

    public ThreadExecutionQueue getForGuild(Guild guild) {
        ThreadExecutionQueue threadExecutionQueue = guildExecutionQueues.get(guild);

        if (threadExecutionQueue == null) {
            ThreadExecutionQueue newQueue = new ThreadExecutionQueue(3);
            guildExecutionQueues.put(guild, newQueue);
            return newQueue;
        }

        return threadExecutionQueue;
    }

    public ISnowflakeMap<ThreadExecutionQueue> getGuildExecutionQueues() {
        return guildExecutionQueues;
    }

    public void joinAll() throws InterruptedException {
        for (ThreadExecutionQueue queue : guildExecutionQueues.values()) {
            queue.join();
        }
    }

    public void closeAll() {
        guildExecutionQueues.values().forEach(ThreadExecutionQueue::close);
    }

}

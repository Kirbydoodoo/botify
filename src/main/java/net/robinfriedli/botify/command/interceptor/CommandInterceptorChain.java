package net.robinfriedli.botify.command.interceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import net.robinfriedli.botify.command.AbstractCommand;
import net.robinfriedli.botify.command.interceptor.interceptors.CommandExecutionInterceptor;
import net.robinfriedli.botify.entities.xml.CommandInterceptorContribution;
import net.robinfriedli.botify.util.Cache;

public class CommandInterceptorChain implements CommandInterceptor {

    private final CommandInterceptor first;

    public CommandInterceptorChain(List<CommandInterceptorContribution> contributions) {
        Iterator<CommandInterceptorContribution> iterator = contributions.iterator();
        first = instantiate(iterator.next(), iterator);
    }

    @SuppressWarnings("unchecked")
    private static AbstractChainableCommandInterceptor instantiate(CommandInterceptorContribution interceptorContribution,
                                                                   Iterator<CommandInterceptorContribution> next) {
        Class<? extends AbstractChainableCommandInterceptor> interceptorClass = interceptorContribution.getImplementationClass();
        Constructor<?>[] constructors = interceptorClass.getConstructors();
        if (constructors.length == 0) {
            throw new IllegalStateException(interceptorClass.getSimpleName() + " does not have any public constructors");
        }

        Constructor<AbstractChainableCommandInterceptor> constructor = (Constructor<AbstractChainableCommandInterceptor>) constructors[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        int parameterCount = constructor.getParameterCount();
        Object[] parameters = new Object[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.isAssignableFrom(AbstractChainableCommandInterceptor.class)) {
                if (next.hasNext()) {
                    parameters[i] = instantiate(next.next(), next);
                } else {
                    parameters[i] = new CommandExecutionInterceptor();
                }
            } else if (parameterType.equals(CommandInterceptorContribution.class)) {
                parameters[i] = interceptorContribution;
            } else {
                parameters[i] = Cache.get(parameterType);
            }
        }

        try {
            return constructor.newInstance(parameters);
        } catch (InstantiationException e) {
            throw new RuntimeException("Constructor " + constructor.toString() + " cannot be instantiated", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access " + constructor.toString(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception while invoking constructor " + constructor.toString(), e);
        }
    }

    @Override
    public void intercept(AbstractCommand command) {
        first.intercept(command);
    }
}

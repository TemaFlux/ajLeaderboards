package us.ajg0702.leaderboards.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SchedulerUtil {
    private static List<Task> tasks;
    private static Boolean IS_FOLIA;

    public static boolean isFolia() {
        return IS_FOLIA == null ? IS_FOLIA = Bukkit.getVersion().toLowerCase().contains("folia") : IS_FOLIA;
    }

    public static Task runTask(Plugin plugin, Object handle, Runnable action) {
        if (plugin == null || action == null) return null;

        Object handleTask = null;

        if (isFolia()) {
            if (handle == null) {
                handle = getAnyEntity();
                if (handle == null) return null;
            }

            ScheduledTask task = null;
            if (handle instanceof Entity) task = ((Entity) handle).getScheduler().run(plugin, ignored -> action.run(), null);
            else if (handle instanceof Location) task = Bukkit.getRegionScheduler().run(plugin, (Location) handle, ignored -> action.run());

            if (task != null) handleTask = task;
        } else {
            handleTask = Bukkit.getScheduler().runTask(plugin, action);
        }

        Task task = handleTask == null ? null : new Task(handleTask);
        addTask(task);
        return task;
    }

    public static Task runTaskLater(Plugin plugin, Object handle, Runnable action, long delayTicks) {
        if (plugin == null || action == null) return null;

        Object handleTask = null;

        if (isFolia()) {
            if (handle == null) {
                handle = getAnyEntity();
                if (handle == null) return null;
            }

            ScheduledTask task = null;
            if (handle instanceof Entity) task = ((Entity) handle).getScheduler().runDelayed(plugin, ignored -> action.run(), null, delayTicks);
            else if (handle instanceof Location) task = Bukkit.getRegionScheduler().runDelayed(plugin, (Location) handle, ignored -> action.run(), delayTicks);

            if (task != null) handleTask = task;
        } else {
            handleTask = Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
        }

        Task task = handleTask == null ? null : new Task(handleTask);
        addTask(task);
        return task;
    }

    public static Task runTaskAsynchronously(Plugin plugin, Runnable action) {
        if (plugin == null || action == null) return null;

        Object handleTask;

        if (isFolia()) {
            handleTask = Bukkit.getAsyncScheduler().runNow(plugin, ignored -> action.run());
        } else {
            handleTask = SchedulerUtil.runTaskAsynchronously(plugin, action);
        }

        Task task = handleTask == null ? null : new Task(handleTask);
        addTask(task);
        return task;
    }

    public static Task runTaskTimerAsynchronously(Plugin plugin, Runnable action, long initialDelayTicks, long periodTicks) {
        if (plugin == null || action == null) return null;

        Object handleTask;

        if (isFolia()) {
            initialDelayTicks = (initialDelayTicks * 1000L) / 20;
            periodTicks = (periodTicks * 1000L) / 20;

            handleTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, ignored -> action.run(), initialDelayTicks, periodTicks, TimeUnit.MILLISECONDS);
        } else {
            handleTask = SchedulerUtil.runTaskTimerAsynchronously(plugin, action, initialDelayTicks, periodTicks);
        }

        Task task = handleTask == null ? null : new Task(handleTask);
        addTask(task);
        return task;
    }

    public static Task runTaskLaterAsynchronously(Plugin plugin, Runnable action, long delayTicks) {
        if (plugin == null || action == null) return null;

        Object handleTask;

        if (isFolia()) {
            handleTask = Bukkit.getAsyncScheduler().runDelayed(plugin, ignored -> action.run(), (long) ((delayTicks * 1000D) / 20D), TimeUnit.MILLISECONDS);
        } else {
            handleTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, action, delayTicks);
        }

        Task task = new Task(handleTask);
        addTask(task);
        return task;
    }

    public static void cancelTasks(Plugin plugin) {
        if (plugin == null) return;

        if (isFolia()) {
            try {
                Bukkit.getAsyncScheduler().cancelTasks(plugin);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            try {
                if (tasks == null || tasks.isEmpty()) return;

                tasks.removeIf(task -> {
                    if (Objects.equals(task.getOwner(), plugin)) {
                        if (!task.isCancelled()) task.cancel();
                        return true;
                    }

                    return false;
                });
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } else {
            SchedulerUtil.cancelTasks(plugin);
        }
    }

    public static List<Task> getActiveWorkers() {
        return tasks == null ? Collections.emptyList() : Collections.unmodifiableList(tasks);
    }

    private static void addTask(Task task) {
        if (task == null) return;

        if (tasks == null) tasks = new ArrayList<>();
        tasks.add(task);
    }

    private static Entity getAnyEntity() {
        List<World> worldList = Bukkit.getWorlds();
        World world = worldList.isEmpty() ? null : worldList.get(0);

        List<Entity> entities = world == null ? null : world.getEntities();
        return entities == null || entities.isEmpty() ? null : entities.get(0);
    }

    public static class Task {
        private final Object handle;

        public Task(Object handle) {
            this.handle = handle;
        }

        public void cancel() {
            if (handle == null) return;

            if (handle instanceof ScheduledTask) {
                ((ScheduledTask) handle).cancel();
            } else if (handle instanceof BukkitTask) {
                ((BukkitTask) handle).cancel();
            } else if (handle instanceof CompletableFuture) {
                ((CompletableFuture<?>) handle).cancel(true);
            }
        }

        public boolean isCancelled() {
            if (handle == null) return false;

            if (handle instanceof ScheduledTask) {
                return ((ScheduledTask) handle).isCancelled();
            } else if (handle instanceof BukkitTask) {
                return ((BukkitTask) handle).isCancelled();
            } else if (handle instanceof CompletableFuture) {
                return ((CompletableFuture<?>) handle).isCancelled();
            }

            return false;
        }

        public Plugin getOwner() {
            if (handle == null) return null;

            if (handle instanceof ScheduledTask) {
                return ((ScheduledTask) handle).getOwningPlugin();
            } else if (handle instanceof BukkitTask) {
                return ((BukkitTask) handle).getOwner();
            }

            return null;
        }

        public int getTaskId() {
            if (handle == null) return -1;

            if (handle instanceof BukkitTask) {
                return ((BukkitTask) handle).getTaskId();
            }

            return -1;
        }

        public Object getHandle() {
            return handle;
        }
    }
}

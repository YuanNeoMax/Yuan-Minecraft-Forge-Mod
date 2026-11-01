package com.yuan.timerewind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class RewindHistory {
    private static final class Entry {
        final long tick;
        final Object event;
        Entry(long tick, Object event) { this.tick = tick; this.event = event; }
    }

    /**
     * MOVABLE, so a recorder can be widened in place instead of replaced.
     *
     * <p>When the player equips a sword with a larger window, the recorder used to be rebuilt from
     * scratch -- throwing away every recorded tick AND the registries the restorer depends on,
     * which turned the newly announced window into an empty one and made every standing-still
     * entity look newly created on the next rewind. Growing the existing history keeps the recording
     * continuous: what is already there stays, and the new capacity fills as time passes.
     */
    private long windowTicks;
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public RewindHistory(long windowTicks) {
        this.windowTicks = Math.max(1L, windowTicks);
    }

    public synchronized void add(long tick, Object event) {
        entries.addLast(new Entry(tick, event));
        long cutoff = tick - windowTicks;
        while (!entries.isEmpty() && entries.peekFirst().tick < cutoff) {
            entries.removeFirst();
        }
    }

    public synchronized long earliestTick() {
        return entries.isEmpty() ? Long.MAX_VALUE : entries.peekFirst().tick;
    }

    public synchronized long windowTicks() {
        return windowTicks;
    }

    /**
     * Widens the retention window WITHOUT discarding anything already recorded.
     *
     * <p>Shrinking is refused rather than applied: a rewind that reaches further back than the data
     * still present would silently restore an older state than it promised, so the window only
     * ever grows and a smaller configured value simply leaves the recording as it is. Entries that
     * fall outside the OLD window are already gone; the ones inside it are exactly the recording
     * that continues.
     */
    public synchronized void widenWindow(long newWindowTicks) {
        if (newWindowTicks > windowTicks) {
            windowTicks = newWindowTicks;
        }
    }

    public synchronized List<Object> since(long targetTick) {
        List<Object> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.tick >= targetTick) out.add(e.event);
        }
        return out;
    }

    public synchronized List<Object> atOrBefore(long targetTick) {
        List<Object> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.tick <= targetTick) out.add(e.event);
        }
        return out;
    }

    public synchronized void clear() {
        entries.clear();
    }
}

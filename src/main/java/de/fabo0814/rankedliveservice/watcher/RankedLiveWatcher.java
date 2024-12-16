package de.fabo0814.rankedliveservice.watcher;

import de.fabo0814.rankedliveservice.socket.RankedLiveSocketHandler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

@Component
public class RankedLiveWatcher implements CommandLineRunner {

    private static final Path MCSR_RANKED_FOLDER = Paths.get(System.getProperty("user.home") + "/mcsrranked/");

    private static JSONObject lastSent;

    @Override
    public void run(String... args) throws Exception {
        WatchService watchService = FileSystems.getDefault().newWatchService();

        RankedLiveWatcher.MCSR_RANKED_FOLDER.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                System.out.println(event.kind() + ": " + event.context());
                if (event.context() instanceof Path filePath && filePath.toString().equals("spectate_match.json")) {
                    processEvent(filePath);
                }
            }
            key.reset();
        }
    }

    private void processEvent(@NotNull Path relativePath) {
        JSONTokener tokener;
        JSONObject json;

        File file = new File(RankedLiveWatcher.MCSR_RANKED_FOLDER.toFile(), relativePath.toString());

        try {
            tokener = new JSONTokener(new FileReader(file));
            json = new JSONObject(tokener);
        } catch (IOException e) {
            return;
        }

        JSONObject data;

        if (timelineIsConsideredNew(json)) {
            JSONArray timelines = json.getJSONArray("timelines");
            if (timelines.isEmpty()) {
                return;
            }

            data = new JSONObject()
                    .put("type", "timeline")
                    .put("timeline", timelines.get(timelines.length() - 1));
        } else if (completionIsConsideredNew(json)) {
            JSONArray completions = json.getJSONArray("completes");
            if (completions.isEmpty()) {
                return;
            }

            data = new JSONObject()
                    .put("type", "complete")
                    .put("completes", completions.get(completions.length() - 1));
        } else {
            return;
        }

        RankedLiveWatcher.lastSent = json;
        RankedLiveSocketHandler.broadcast(data.toString());
    }

    private boolean timelineIsConsideredNew(@NotNull JSONObject json) {
        if (RankedLiveWatcher.lastSent == null) {
            return true;
        }

        JSONArray timelines1 = RankedLiveWatcher.lastSent.getJSONArray("timelines");
        JSONArray timelines2 = json.getJSONArray("timelines");

        return !timelines2.toString().equals(timelines1.toString());
    }

    private boolean completionIsConsideredNew(@NotNull JSONObject json) {
        if (RankedLiveWatcher.lastSent == null) {
            return true;
        }

        JSONArray completes1 = RankedLiveWatcher.lastSent.getJSONArray("completes");
        JSONArray completes2 = json.getJSONArray("completes");

        return !completes2.toString().equals(completes1.toString());
    }

}

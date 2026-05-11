package awa.uhd;

import arc.Events;
import arc.files.Fi;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.Timer.Task;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.ServerLoadEvent;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import arc.util.CommandHandler;

public class UhdServerStatusPlugin extends Plugin{
    private static final String PLUGIN_NAME = "uhd-server-status-plugin";

    private UhdConfig config;
    private Task statusTask;

    @Override
    public void init(){
        reloadConfig();

        Events.on(ServerLoadEvent.class, event -> startStatusTask());
        Events.on(PlayerJoin.class, event -> showStatusTo(event.player));
        Events.on(ResetEvent.class, event -> hideStatusFromAll());

        if(Vars.net != null && Vars.net.server()){
            startStatusTask();
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("uhdreload", "从磁盘重新加载 UHD 状态插件配置。", args -> {
            reloadConfig();
            Log.info("已重新加载 UHD 状态插件配置。");
        });
    }

    private void reloadConfig(){
        config = UhdConfig.load(resolveConfigFile());
        startStatusTask();
    }

    private Fi resolveConfigFile(){
        Fi folder = new Fi("config").child("mods").child("config").child(PLUGIN_NAME);
        folder.mkdirs();
        return folder.child("config.json");
    }

    private void startStatusTask(){
        if(statusTask != null){
            statusTask.cancel();
            statusTask = null;
        }

        if(config == null || !config.enabled){
            return;
        }

        float refresh = Math.max(config.refreshIntervalSec, 1f);
        statusTask = Timer.schedule(this::broadcastStatus, 1f, refresh);
    }

    private void broadcastStatus(){
        for(Player player : Groups.player){
            showStatusTo(player);
        }
    }

    private void showStatusTo(Player player){
        if(player == null || player.con == null || config == null || !config.enabled){
            return;
        }

        Call.infoPopup(
            player.con,
            renderStatus(player),
            config.popupId,
            Math.max(config.popupDurationSec, 0.8f),
            config.alignValue(),
            config.top,
            config.left,
            config.bottom,
            config.right
        );
    }

    private void hideStatusFromAll(){
        for(Player player : Groups.player){
            if(player != null && player.con != null){
                String popupId = config == null ? "uhd-server-status" : config.popupId;
                int align = config == null ? Align.topLeft : config.alignValue();
                int top = config == null ? 60 : config.top;
                int left = config == null ? 16 : config.left;
                int bottom = config == null ? 0 : config.bottom;
                int right = config == null ? 0 : config.right;
                Call.infoPopup(player.con, "", popupId, 0.1f, align, top, left, bottom, right);
            }
        }
    }

    private String renderStatus(Player player){
        double memoryMb = currentMemoryMb();
        int tps = Vars.state != null && Vars.state.serverTps > 0 ? Vars.state.serverTps : 60;

        StringBuilder builder = new StringBuilder();
        appendLine(builder, config.headerText);
        appendLine(builder, applyPlaceholders(config.serverNameFormat, player, tps, memoryMb, config.serverName, config.serverAnnouncement));
        appendLine(builder, applyPlaceholders(config.announcementFormat, player, tps, memoryMb, config.serverName, config.serverAnnouncement));
        appendLine(builder, applyPlaceholders(config.tpsFormat, player, tps, memoryMb, config.serverName, config.serverAnnouncement));
        appendLine(builder, applyPlaceholders(config.memoryFormat, player, tps, memoryMb, config.serverName, config.serverAnnouncement));
        return builder.toString().trim();
    }

    private String applyPlaceholders(String template, Player player, int tps, double memoryMb, String serverName, String announcement){
        String value = template == null ? "" : template;
        return value
            .replace("{server_name}", safe(serverName))
            .replace("{announcement}", safe(announcement))
            .replace("{tps}", String.valueOf(tps))
            .replace("{memory_mb}", Strings.fixed((float)memoryMb, 1))
            .replace("{player_name}", player == null ? "" : Strings.stripColors(player.name));
    }

    private void appendLine(StringBuilder builder, String line){
        if(line == null){
            return;
        }

        String text = line.trim();
        if(text.isEmpty()){
            return;
        }

        if(builder.length() > 0){
            builder.append('\n');
        }
        builder.append(text);
    }

    private double currentMemoryMb(){
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        return usedBytes / 1024d / 1024d;
    }

    private String safe(String value){
        return value == null ? "" : value;
    }
}

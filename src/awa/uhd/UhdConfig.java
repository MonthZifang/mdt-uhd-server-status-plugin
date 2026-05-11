package awa.uhd;

import arc.files.Fi;
import arc.util.Align;
import arc.util.Log;
import arc.util.serialization.Json;

public class UhdConfig{
    public boolean enabled = true;
    public float refreshIntervalSec = 1.5f;
    public float popupDurationSec = 2.2f;
    public String align = "top_left";
    public int top = 120;
    public int left = 95;
    public int bottom = 0;
    public int right = 0;
    public String popupId = "uhd-server-status";
    public String headerText = "";
    public String serverName = "我的服务器";
    public String serverAnnouncement = "欢迎来到服务器";
    public String serverNameFormat = "[white]服务器名称:[] [accent]{server_name}[]";
    public String announcementFormat = "[white]服务器公告:[] [lightgray]{announcement}[]";
    public String tpsFormat = "[white]服务器TPS:[] [green]{tps}[]";
    public String memoryFormat = "[white]服务器内存:[] [green]{memory_mb} MB[]";

    public static UhdConfig load(Fi file){
        UhdConfig defaults = new UhdConfig();
        defaults.sanitize();

        if(file == null){
            return defaults;
        }

        Json json = new Json();
        json.setIgnoreUnknownFields(true);

        try{
            if(file.parent() != null){
                file.parent().mkdirs();
            }

            if(!file.exists()){
                file.writeString(render(defaults), false, "UTF-8");
                return defaults;
            }

            UhdConfig loaded = json.fromJson(UhdConfig.class, file.readString("UTF-8"));
            if(loaded == null){
                file.writeString(render(defaults), false, "UTF-8");
                return defaults;
            }

            loaded.sanitize();
            file.writeString(render(loaded), false, "UTF-8");
            return loaded;
        }catch(Throwable t){
            Log.err("加载 UHD 配置失败，正在重写默认配置。");
            Log.err(t);
            try{
                file.writeString(render(defaults), false, "UTF-8");
            }catch(Throwable writeError){
                Log.err(writeError);
            }
            return defaults;
        }
    }

    public int alignValue(){
        String value = align == null ? "" : align.trim().toLowerCase();
        switch(value){
            case "top":
            case "top_center":
                return Align.top;
            case "top_right":
                return Align.topRight;
            case "left":
            case "center_left":
                return Align.left;
            case "center":
                return Align.center;
            case "top_left":
            default:
                return Align.topLeft;
        }
    }

    public void sanitize(){
        refreshIntervalSec = Math.max(refreshIntervalSec, 1f);
        popupDurationSec = Math.max(popupDurationSec, 0.8f);
        align = safe(align, "top_left");
        popupId = safe(popupId, "uhd-server-status");
        headerText = safe(headerText, "");
        serverName = safe(serverName, "我的服务器");
        serverAnnouncement = safe(serverAnnouncement, "欢迎来到服务器");
        serverNameFormat = safe(serverNameFormat, "[white]服务器名称:[] [accent]{server_name}[]");
        announcementFormat = safe(announcementFormat, "[white]服务器公告:[] [lightgray]{announcement}[]");
        tpsFormat = safe(tpsFormat, "[white]服务器TPS:[] [green]{tps}[]");
        memoryFormat = safe(memoryFormat, "[white]服务器内存:[] [green]{memory_mb} MB[]");
    }

    private static String render(UhdConfig cfg){
        StringBuilder out = new StringBuilder(1024);
        out.append("{\n");
        appendBoolean(out, "enabled", cfg.enabled, true);
        appendFloat(out, "refreshIntervalSec", cfg.refreshIntervalSec, true);
        appendFloat(out, "popupDurationSec", cfg.popupDurationSec, true);
        appendString(out, "align", cfg.align, true);
        appendInt(out, "top", cfg.top, true);
        appendInt(out, "left", cfg.left, true);
        appendInt(out, "bottom", cfg.bottom, true);
        appendInt(out, "right", cfg.right, true);
        appendString(out, "popupId", cfg.popupId, true);
        appendString(out, "headerText", cfg.headerText, true);
        appendString(out, "serverName", cfg.serverName, true);
        appendString(out, "serverAnnouncement", cfg.serverAnnouncement, true);
        appendString(out, "serverNameFormat", cfg.serverNameFormat, true);
        appendString(out, "announcementFormat", cfg.announcementFormat, true);
        appendString(out, "tpsFormat", cfg.tpsFormat, true);
        appendString(out, "memoryFormat", cfg.memoryFormat, false);
        out.append("}\n");
        return out.toString();
    }

    private static void appendBoolean(StringBuilder out, String key, boolean value, boolean comma){
        out.append("  \"").append(key).append("\": ").append(value).append(comma ? ",\n" : "\n");
    }

    private static void appendFloat(StringBuilder out, String key, float value, boolean comma){
        out.append("  \"").append(key).append("\": ").append(value).append(comma ? ",\n" : "\n");
    }

    private static void appendInt(StringBuilder out, String key, int value, boolean comma){
        out.append("  \"").append(key).append("\": ").append(value).append(comma ? ",\n" : "\n");
    }

    private static void appendString(StringBuilder out, String key, String value, boolean comma){
        out.append("  \"").append(key).append("\": \"").append(escape(value)).append("\"").append(comma ? ",\n" : "\n");
    }

    private static String safe(String value, String fallback){
        return value == null ? fallback : value;
    }

    private static String escape(String value){
        String text = value == null ? "" : value;
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }
}

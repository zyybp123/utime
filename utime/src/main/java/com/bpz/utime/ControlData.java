package com.bpz.utime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author bpzzr
 * @date 2023/5/31
 * <p>
 * 控制信令数据
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ControlData {
    //连接指令
    public static final String COMMAND_CONNECT = "connect_command";
    //心跳检测指令
    public static final String COMMAND_HEARTBEAT = "heart_beat_command";
    private String command;
    private String code;
    private String extra;

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

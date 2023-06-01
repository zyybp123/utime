package com.bpz.utime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bpzzr
 * @date 2023/3/10
 * <p>
 * 用于倒计时控制中心的 websocket 服务端
 */
@Slf4j
@Component
@ServerEndpoint("/ws/countdown/{tag}")
public class WebSocketServer {
    public static final String TAG_WEB = "web";
    public static final String TAG_PHONE = "phone";

    private static final Map<Session, String> sServerCodeMap = new ConcurrentHashMap<>();
    private static final Map<String, Session> sClientSessionMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(@PathParam("tag") String tag, Session session) throws IOException {
        if (TAG_WEB.equals(tag)) {
            //来自被控端
            UUID uuid = UUID.randomUUID();
            String code = uuid.toString().split("-")[0];
            sServerCodeMap.put(session, code);
            log.info("连接码：" + code);
            sendMessageTo(code, session);
        }
        //if (TAG_PHONE.equals(tag)) {
            //来自控制端
            // TODO: 2023/5/31
            //log.info("...");
        //}
        log.info(tag + "端已连接！");
    }

    @OnMessage
    public void onMessage(@PathParam("tag") String tag, String message, Session session) throws IOException {
        log.info(tag + "端发送的信息为：" + message);
        // 解析消息数据
        ObjectMapper om = new ObjectMapper();

        ControlData controlData = om.readValue(message, ControlData.class);
        String code = controlData.getCode();
        String command = controlData.getCommand();
        Session sessionByCode = getSessionByCode(code);

        if (ControlData.COMMAND_HEARTBEAT.equals(command)) {
            //心跳检测，直接忽略
            //log.info("heart_beat");
            return;
        }
        if (TAG_PHONE.equals(tag)) {
            // 控制端发来消息
            if (ControlData.COMMAND_CONNECT.equals(command)) {
                if (sessionByCode != null) {
                    //如果是连接命令,记录客户端的session
                    sClientSessionMap.put(code, session);
                }
            }
            if (sessionByCode == null) {
                //告知客户端，连接码错误
                String errorMsg = "{\"command\":0,\"msg\":\"错误的连接码！\",\"time\":\"\"}";
                log.info(errorMsg);
                sendMessageTo(errorMsg, session);
            }
            //直接把命令转给被控端
            sendMessageTo(command, sessionByCode);
        }
        if (TAG_WEB.equals(tag)) {
            // 网页端的消息，转发给已连接的客户端
            String serverCode = sServerCodeMap.get(session);
            sendMessageTo(message, sClientSessionMap.get(serverCode));
        }
    }

    private Session getSessionByCode(String code) {
        for (Map.Entry<Session, String> entry : sServerCodeMap.entrySet()) {
            Session s = entry.getKey();
            String tempCode = sServerCodeMap.get(s);
            if (Objects.equals(tempCode, code)) {
                return s;
            }
        }
        return null;
    }

    @OnMessage
    public void onMessage(@PathParam("tag") String tag, byte[] messages, Session session) throws IOException {
        log.info(tag + "端(B)发送的信息为：" + Arrays.toString(messages));
    }


    @OnClose
    public void onClose(@PathParam("tag") String tag, Session session) throws IOException {
        log.info(tag + "端已断开！");
        if (TAG_WEB.equals(tag)) {
            sServerCodeMap.remove(session);
        }
        if (TAG_PHONE.equals(tag)) {
            removeClient(session);
        }
    }

    private void removeClient(Session session) {
        for (Map.Entry<String, Session> entry : sClientSessionMap.entrySet()) {
            Session s = entry.getValue();
            if (Objects.equals(s, session)) {
                sClientSessionMap.remove(entry.getKey());
            }
        }
    }

    @OnError
    public void onError(@PathParam("tag") String tag, Session session, Throwable error) {
        if (error != null) {
            error.printStackTrace();
        }
    }

    public void sendMessageTo(String message, Session session) throws IOException {
        if (session != null) {
            session.getBasicRemote().sendText(message);
        }
    }

    public void sendMessageTo(byte[] messages, Session session) throws IOException {
        if (session != null) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(messages.length);
            session.getBasicRemote().sendBinary(buffer);
        }
    }
}

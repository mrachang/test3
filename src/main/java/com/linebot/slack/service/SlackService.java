package com.linebot.slack.service;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SlackService {

    private final MethodsClient methodsClient;

    @Value("${slack.channel}")
    String channelId;

    @Value("${slack.template}")
    String slackTemplate;

    public SlackService(MethodsClient methodsClient) {
        this.methodsClient = methodsClient;
    }

    public boolean send(String content) {
        ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                .channel(channelId) // Use a channel ID `C1234567` is preferable
                .text(content)
                .build();
        ChatPostMessageResponse response = null;
        try {
            response = methodsClient.chatPostMessage(request);
        } catch (Exception e) {
            log.warn("something went wrong");
        }
        return response != null && response.isOk();
    }

    public boolean send(String type,MessageEvent<TextMessageContent> event) {

        String from = getSource(event);
        String text = getText(event);

        String postContent = String.format(slackTemplate,getNow(),type,from,text);

        return  send(postContent);
    }

    private String getSource(Event event){
        var from = "";
        Source source = event.getSource();
        if (source instanceof GroupSource) {
            from = ((GroupSource) source).getGroupId();
        } else if (source instanceof RoomSource) {
            from = ((RoomSource) source).getRoomId();
        } else {
            from = event.getSource().getUserId();
        }
        return from;
    }

    private String getNow(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return now.format(formatter);
    }

    private String getText(MessageEvent<TextMessageContent> event){
        return event.getMessage().getText();
    }

}

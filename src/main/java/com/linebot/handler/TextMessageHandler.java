package com.linebot.handler;


import com.linebot.slack.service.SlackService;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.group.GroupMemberCountResponse;
import com.linecorp.bot.model.group.GroupSummaryResponse;
import com.linecorp.bot.model.message.ImageMessage;

import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.sender.Sender;
import com.linecorp.bot.model.message.template.*;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.model.room.RoomMemberCountResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;

@LineMessageHandler
@Slf4j
public class TextMessageHandler {

    private final LineMessagingClient lineMessagingClient;
    private final SlackService slackService;

    public TextMessageHandler(LineMessagingClient lineMessagingClient,SlackService slackService) {
        this.lineMessagingClient = lineMessagingClient;
        this.slackService = slackService;
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        log.info("event: {}", event);
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
        slackService.send("text",event);
    }

    private void reply(@NonNull String replyToken,
                       @NonNull List<Message> messages,
                       boolean notificationDisabled) {
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
                    .get();
            log.info("Sent messages: {}", apiResponse);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        reply(replyToken, messages, false);
    }

    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private static URI createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .scheme("https")
                .path(path).build()
                .toUri();
    }


    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        final String text = content.getText();


        switch (text) {
            case "我是誰": {
                final String userId = event.getSource().getUserId();
                if (userId != null) {
                    if (event.getSource() instanceof GroupSource) {
                        lineMessagingClient
                                .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }
                                    this.reply(replyToken, Arrays.asList(new TextMessage("(from group)"),
                                            new TextMessage("你是：" + profile.getDisplayName()),
                                            new ImageMessage(profile.getPictureUrl(), profile.getPictureUrl()))
                                    );
                                });
                    } else {
                        lineMessagingClient
                                .getProfile(userId)
                                .whenComplete((profile, throwable) -> {
                                    if (throwable != null) {
                                        this.replyText(replyToken, throwable.getMessage());
                                        return;
                                    }

                                    this.reply(
                                            replyToken,
                                            Arrays.asList(new TextMessage("你是: " + profile.getDisplayName()),
                                                    new TextMessage("您好"))
                                    );
                                });
                    }
                } else {
                    this.replyText(replyToken, "看不懂您的名子");
                }
                break;
            }
            case "掰機器人": {
                Source source = event.getSource();
                if (source instanceof GroupSource) {
                    this.replyText(replyToken, "離開群組");
                    lineMessagingClient.leaveGroup(((GroupSource) source).getGroupId()).get();
                } else if (source instanceof RoomSource) {
                    this.replyText(replyToken, "離開房間");
                    lineMessagingClient.leaveRoom(((RoomSource) source).getRoomId()).get();
                } else {
                    this.replyText(replyToken, "我不能離開個人聊天室");
                }
                break;
            }
            case "回饋": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "喜歡這個機器人嗎?",
                        new MessageAction("喜歡", "喜歡"),
                        new MessageAction("不喜歡", "不喜歡"));
                TemplateMessage templateMessage = new TemplateMessage("Confirm alt text", confirmTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "喜歡":{
                this.replyText(replyToken, "謝謝你");
            }
            case "不喜歡":{
                this.replyText(replyToken, "好吧!");
            }

        case "天氣": {
                URI uri = new URI("https://ssl.gstatic.com/onebox/weather/64/partly_cloudy.png");
                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                        new CarouselColumn(uri, "台中市", "", Arrays.asList(
                                new MessageAction("氣溫",
                                        "今天氣溫是30度"),
                                new MessageAction("降雨機率",
                                        "今天的降雨機率是0%"),
                                new MessageAction("降雨機率",
                                        "今天的降雨機率是0%")

                        ))
                        ));
                TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "你好":{
                this.replyText(replyToken, "你好啊!");
                break;
            }
            case "你叫什麼名子":{
                this.replyText(replyToken, "我叫測試2");
                break;
            }
            default:
                this.replyText(replyToken, "很抱歉這句話我還聽不懂");
                break;
        }
    }

}

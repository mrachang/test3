package com.linebot;

import com.linebot.slack.service.SlackService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class SlackTest {

    @Autowired
    private SlackService slackService;

    @Test
    public void contextLoads() {
    }

    @Test
    public void slackNotification(){
        Assertions.assertTrue(slackService.send("wahaha"));
    }
}

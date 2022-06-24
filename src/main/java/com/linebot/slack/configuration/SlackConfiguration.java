package com.linebot.slack.configuration;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfiguration {

    @Value("${slack.token}")
    String slackToken;

    @Bean
    public MethodsClient slack(){
        return Slack.getInstance().methods(slackToken);
    }
}

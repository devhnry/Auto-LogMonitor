package org.remita.autologmonitor.config;

import dev.ditsche.mailo.config.MailoConfig;
import dev.ditsche.mailo.config.SmtpConfig;
import dev.ditsche.mailo.provider.MailProvider;
import dev.ditsche.mailo.provider.SmtpMailProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    public MailConfig() {
        MailoConfig config = MailoConfig.get();
        config.setTemplateDirectory("mails/");
        config.setMjmlAppId("ccd40de4-bb5d-42b0-a942-7630b5028571");
        config.setMjmlAppSecret("9d28eb3c-578f-4d30-9bb6-03c93932d2f8");
    }

    @Bean
    public MailProvider mailProvider() {
        SmtpConfig config = new SmtpConfig();
        config.setHost("smtp.gmail.com");
        config.setUsername("devwhenry@gmail.com");
        config.setPassword("dmkj mopg cmgs rjoj");
        config.setPort(465);
        config.setSsl(true);
        return new SmtpMailProvider(config);
    }
}

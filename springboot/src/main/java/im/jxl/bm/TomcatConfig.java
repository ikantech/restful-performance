package im.jxl.bm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * TomcatConfig
 */
@Configuration
@EnableConfigurationProperties(ConfigProperties.class)
public class TomcatConfig {

    @Bean
    public ServletWebServerFactory servletWebServerFactory(ConfigProperties configProperties) {
        ConfigProperties.Tomcat tomcat = configProperties.getTomcat();

        TomcatServletWebServerFactory tomcatServletWebServerFactory = new TomcatServletWebServerFactory();
        tomcatServletWebServerFactory.setProtocol(tomcat.getProtocol());

        tomcatServletWebServerFactory.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
            Field[] fields = tomcat.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    connector.setAttribute(field.getName(), field.get(tomcat));
                } catch (IllegalAccessException e) {
                    System.out.println("Tomcat connector 配置异常:"+e.getMessage());
                    continue;
                }
            }
        });

        return tomcatServletWebServerFactory;
    }
}
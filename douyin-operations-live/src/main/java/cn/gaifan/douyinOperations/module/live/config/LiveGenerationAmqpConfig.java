package cn.gaifan.douyinOperations.module.live.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 直播一键话术异步生成（G-2）：主队列 + DLX/DLQ
 */
@Configuration
public class LiveGenerationAmqpConfig {

    public static final String QUEUE_LIVE_SCRIPT_GENERATION = "live.script.generation";
    public static final String DLX_LIVE_SCRIPT_GENERATION = "live.script.generation.dlx";
    public static final String ROUTING_KEY_DLQ = "live.script.generation.dead";
    public static final String QUEUE_LIVE_SCRIPT_GENERATION_DLQ = "live.script.generation.dlq";

    @Bean
    public DirectExchange liveScriptGenerationDeadLetterExchange() {
        return new DirectExchange(DLX_LIVE_SCRIPT_GENERATION, true, false);
    }

    @Bean
    public Queue liveScriptGenerationDlq() {
        return new Queue(QUEUE_LIVE_SCRIPT_GENERATION_DLQ, true);
    }

    @Bean
    public Binding liveScriptGenerationDlqBinding(Queue liveScriptGenerationDlq,
            DirectExchange liveScriptGenerationDeadLetterExchange) {
        return BindingBuilder.bind(liveScriptGenerationDlq).to(liveScriptGenerationDeadLetterExchange).with(ROUTING_KEY_DLQ);
    }

    @Bean
    public Queue liveScriptGenerationQueue(
            @Value("${app.live.generation.async.amqp.max-priority:10}") int maxPriority) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_LIVE_SCRIPT_GENERATION);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DLQ);
        if (maxPriority > 0) {
            args.put("x-max-priority", maxPriority);
        }
        return new Queue(QUEUE_LIVE_SCRIPT_GENERATION, true, false, false, args);
    }
}

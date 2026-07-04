package com.github.cadecode.xboot.admin.state;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/**
 * 状态机 Demo 配置
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@TestConfiguration
@EnableStateMachineFactory
public class StateTestConfig extends StateMachineConfigurerAdapter<StateTestState, StateTestEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<StateTestState, StateTestEvent> states) throws Exception {
        states.withStates()
                .initial(StateTestState.S1)
                .states(EnumSet.allOf(StateTestState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<StateTestState, StateTestEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(StateTestState.S1).target(StateTestState.S2)
                .event(StateTestEvent.GO_S2)
                .and()
                .withExternal()
                .source(StateTestState.S2).target(StateTestState.S3)
                .event(StateTestEvent.GO_S3)
                .and()
                .withExternal()
                .source(StateTestState.S3).target(StateTestState.S4)
                .event(StateTestEvent.GO_S4)
                .and()
                .withExternal()
                .source(StateTestState.S1).target(StateTestState.S5)
                .event(StateTestEvent.CANCEL);
    }
}

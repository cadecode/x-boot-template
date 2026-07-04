package com.github.cadecode.xboot.admin.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机 Demo 测试
 * <p>
 * 转换规则：S1 →(GO_S2)→ S2 →(GO_S3)→ S3 →(GO_S4)→ S4
 *           S1 →(CANCEL)→ S5
 *
 * @author Cade Li
 * @since 2026/7/4
 */
@SpringBootTest
@Import(StateTestConfig.class)
public class StateTests {

    @Autowired
    private StateMachineFactory<StateTestState, StateTestEvent> stateMachineFactory;

    private record Transition(StateTestState source, StateTestState target, StateTestEvent event) {
    }

    private StateMachine<StateTestState, StateTestEvent> createMachine(String id, List<Transition> recorder) {
        StateMachine<StateTestState, StateTestEvent> sm = stateMachineFactory.getStateMachine(id);

        sm.getStateMachineAccessor().doWithAllRegions(access -> {
            access.addStateMachineInterceptor(new org.springframework.statemachine.support.StateMachineInterceptorAdapter<>() {
                @Override
                public void postStateChange(
                        org.springframework.statemachine.state.State<StateTestState, StateTestEvent> state,
                        org.springframework.messaging.Message<StateTestEvent> message,
                        org.springframework.statemachine.transition.Transition<StateTestState, StateTestEvent> transition,
                        StateMachine<StateTestState, StateTestEvent> stateMachine,
                        StateMachine<StateTestState, StateTestEvent> rootStateMachine) {
                    if (transition == null || transition.getSource() == null) return;
                    StateTestEvent evt = message != null ? message.getPayload() : null;
                    recorder.add(new Transition(
                            transition.getSource().getId(),
                            state.getId(),
                            evt));
                }
            });
        });
        return sm;
    }

    private static void send(StateMachine<StateTestState, StateTestEvent> sm, StateTestEvent event) {
        sm.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
    }

    @Test
    @DisplayName("正常流程：S1 → S2 → S3 → S4")
    void testNormalFlow() {
        List<Transition> recorder = new ArrayList<>();
        StateMachine<StateTestState, StateTestEvent> sm = createMachine("demo-001", recorder);
        sm.startReactively().block();

        send(sm, StateTestEvent.GO_S2);
        send(sm, StateTestEvent.GO_S3);
        send(sm, StateTestEvent.GO_S4);
        sm.stopReactively().block();

        assertEquals(StateTestState.S4, sm.getState().getId());
        assertEquals(3, recorder.size());
        assertEquals(new Transition(StateTestState.S1, StateTestState.S2, StateTestEvent.GO_S2), recorder.get(0));
        assertEquals(new Transition(StateTestState.S2, StateTestState.S3, StateTestEvent.GO_S3), recorder.get(1));
        assertEquals(new Transition(StateTestState.S3, StateTestState.S4, StateTestEvent.GO_S4), recorder.get(2));
    }

    @Test
    @DisplayName("非法事件：S1 下发 GO_S3 → 拒绝，状态不变")
    void testInvalidTransition() {
        List<Transition> recorder = new ArrayList<>();
        StateMachine<StateTestState, StateTestEvent> sm = createMachine("demo-002", recorder);
        sm.startReactively().block();

        int before = recorder.size();
        send(sm, StateTestEvent.GO_S3); // S1 不能直接跳到 S3
        sm.stopReactively().block();

        assertEquals(StateTestState.S1, sm.getState().getId());
        assertEquals(before, recorder.size());
    }

    @Test
    @DisplayName("取消流程：S1 → CANCEL → S5")
    void testCancelFlow() {
        List<Transition> recorder = new ArrayList<>();
        StateMachine<StateTestState, StateTestEvent> sm = createMachine("demo-003", recorder);
        sm.startReactively().block();

        send(sm, StateTestEvent.CANCEL);
        sm.stopReactively().block();

        assertEquals(1, recorder.size());
        assertEquals(new Transition(StateTestState.S1, StateTestState.S5, StateTestEvent.CANCEL), recorder.get(0));
    }

    @Test
    @DisplayName("S2 后取消 → 拒绝")
    void testCancelAfterStep() {
        List<Transition> recorder = new ArrayList<>();
        StateMachine<StateTestState, StateTestEvent> sm = createMachine("demo-004", recorder);
        sm.startReactively().block();

        send(sm, StateTestEvent.GO_S2);
        assertEquals(1, recorder.size());

        send(sm, StateTestEvent.CANCEL); // S2 不能取消
        assertEquals(1, recorder.size());
        assertEquals(StateTestState.S2, sm.getState().getId());

        sm.stopReactively().block();
    }
}

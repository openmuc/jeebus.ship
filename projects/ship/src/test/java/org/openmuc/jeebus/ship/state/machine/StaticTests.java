/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.state.machine;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaticTests {
    /**
     * Check that the assertions in State's static{} block succeed
     */
    @Test
    void staticAssertsSucceed() {
        // dummy data dependency to make sure the class is loaded,
        // and therefore its static { assert ... } has run
        assert State.CMI_INIT_START.toString().length() > 2;
    }

    /**
     * Check that state handlers don't have any non-final fields
     */
    @Test
    void stateHandlersAreStateless() {
        for (State value : State.values()) {
            assertClassIsStateless(value.getHandler().getClass());
        }
    }

    void assertClassIsStateless(Class<?> cls) {
        for (Field field : cls.getFields()) {
            assertTrue(Modifier.isFinal(field.getModifiers()));
        }
    }

    /**
     * Check that every state handler which overrides {@link StateHandler#processMessage(byte[], StateHandlerContext)}
     * also overrides {@link StateHandler#onEntered(State, StateHandlerContext)}, which
     * can cause {@code processMessage()} to be called.
     * <p>
     * This reduces the probability of errors where there is a {@code processMessage()}
     * implementation that is simply never called.
     */
    @Test
    void stateHandlersOverrideSomethingToProcessMessage() throws
        NoSuchMethodException {
        for (State value : State.values()) {
            assertStateHandlerOverridesSomethingToProcessMessage(value.getHandler().getClass());
        }
    }

    private void assertStateHandlerOverridesSomethingToProcessMessage(Class<? extends StateHandler> aClass) throws
        NoSuchMethodException {
        Method processMessage = aClass.getMethod(
            "processMessage",
            byte[].class,
            StateHandlerContext.class
        );
        if (!processMessage.isDefault()) {
            boolean foundPotentialCaller = false;
            for (Method method : aClass.getMethods()) {
                if (method.isDefault() || method.equals(processMessage)) continue;
                foundPotentialCaller = true;
            }
            assertTrue(foundPotentialCaller, "class "+aClass.getSimpleName()+ " overrides processMessage() but does not override any method that could call it!");
        }
    }
}

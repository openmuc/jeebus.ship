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

public enum SpecifiedTimeout {
    CMI_TIMEOUT,
    SME_WAIT_FOR_READY,
    SME_SEND_PROLONGATION_REQUEST,
    SME_PROLONGATION_REQUEST_REPLY,
    SME_PROTH_WAIT,
    CLOSE_WAIT
}

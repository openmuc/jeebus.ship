/********************************************************************************
 * Copyright (c) 2026 Fraunhofer ISE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openmuc.jeebus.ship.message.smepin;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import static org.openmuc.jeebus.ship.message.MessageUtility.*;

public class PinTypeAdapter extends TypeAdapter<PinMsg> {
    @Override
    public void write(JsonWriter out, PinMsg value) throws IOException {
        out.beginObject();

        if (value.getPinValue() instanceof ConnectionPinStateType) {
            ConnectionPinStateType cpsType
                = (ConnectionPinStateType) value.getPinValue();
            if (cpsType.getPinState() == null) {
                throw new IllegalArgumentException("pinState cannot be null");
            }

            out.name("connectionPinState");

            writeConnectionPinState(out, cpsType);
        }
        else if (value.getPinValue() instanceof ConnectionPinInputType) {
            ConnectionPinInputType cpiType
                = (ConnectionPinInputType) value.getPinValue();

            if (cpiType.getPin() == null) {
                throw new IllegalArgumentException("pin cannot be null");
            }
            if (!isValidPin(cpiType.getPin())) {
                throw new IllegalArgumentException("pin is invalid");
            }

            out.name("connectionPinInput");

            out.beginArray();
            writeStringToObject(out, "pin", cpiType.getPin());
            out.endArray();
        }
        else {
            ConnectionPinErrorType cpeType
                = (ConnectionPinErrorType) value.getPinValue();
            if (cpeType.getPinError() == null) {
                throw new IllegalArgumentException("pin error type cannot be null");
            }

            out.name("connectionPinError");

            out.beginArray();
            writeIntToObject(out, "error", cpeType.getPinError().getError());
            out.endArray();
        }

        out.endObject();
    }

    private void writeConnectionPinState(
        JsonWriter out,
        ConnectionPinStateType cps
    ) throws IOException {
        out.beginArray();
        writeStringToObject(out, "pinState", cps.getPinState().toString());
        if (cps.getInputPermission() != null) {
            writeStringToObject(
                out,
                "inputPermission",
                cps.getInputPermission().toString()
            );
        }
        out.endArray();
    }

    @Override
    public PinMsg read(JsonReader in) throws IOException {
        PinMsg pinMsg = new PinMsg();

        in.beginObject();

        while (in.hasNext()) {
            String field = in.nextName();
            switch (field) {
                // either a PinState msg or a PinInput msg
                case "connectionPinState":
                    pinMsg.setPinValue(readPinState(in));
                    break;
                case "connectionPinInput":
                    pinMsg.setPinValue(readPinInput(in));
                    break;
                case "connectionPinError":
                    pinMsg.setPinValue(readPinError(in));
                    break;
                default:
                    throw new IllegalArgumentException("field '"
                        + field
                        + "' not recognized");
            }
        }

        in.endObject();
        return pinMsg;
    }

    private ConnectionPinStateType readPinState(JsonReader in) throws IOException {
        ConnectionPinStateType cpsType = new ConnectionPinStateType();
        in.beginArray();
        in.beginObject();
        while (in.hasNext()) {
            String field = in.nextName();
            if (field.equals("pinState")) {
                cpsType.setPinState(PinStateType.getByValue(in.nextString()));
                in.endObject();
                beginObjectIfHasNext(in);
            }
            else if (field.equals("inputPermission")) {
                cpsType.setInputPermission(PinInputPermissionType.getByValue(in.nextString()));
                in.endObject();
            }
            else {
                throw new IllegalArgumentException("field "
                    + field
                    + " not recognized");
            }
        }
        in.endArray();
        return cpsType;
    }

    private ConnectionPinInputType readPinInput(JsonReader in) throws IOException {
        ConnectionPinInputType cpiType = new ConnectionPinInputType();
        in.beginArray();
        in.beginObject();
        if (in.nextName().equals("pin")) {
            cpiType.setPin(readWrappedString(in));
        }
        else {
            throw new IllegalArgumentException("invalid PIN input message");
        }
        in.endArray();
        return cpiType;
    }

    private ConnectionPinErrorType readPinError(JsonReader in) throws IOException {
        ConnectionPinErrorType cpeType = new ConnectionPinErrorType();
        in.beginArray();
        in.beginObject();
        if (in.nextName().equals("error")) {
            cpeType.setPinError(PinError.getByValue(readWrappedInt(in)));
        }
        else {
            throw new IllegalArgumentException("invalid PIN error message");
        }
        in.endArray();
        return cpeType;
    }
}

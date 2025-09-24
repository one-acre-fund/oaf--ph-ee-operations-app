package org.apache.fineract.data;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

public class ErrorResponse extends HashMap<String, String> {

    public static final String EMPTY_RESPONSE = "Empty response";

    private enum Key {
        ERRORCODE("errorCode"),
        ERRORDESCRIPTION("errorDescription"),
        DEVELOPERMESSAGE("developerMessage");

        private final String text;

        private Key(final String text) {
            this.text = text;
        }
    }

    public static class Builder {
        ErrorResponse res = new ErrorResponse();

        public Builder setErrorCode(String errorCode) {
            res.put(Key.ERRORCODE.text, errorCode);
            return this;
        }

        public Builder setErrorDescription(String errorDescription) {
            res.put(Key.ERRORDESCRIPTION.text, errorDescription);
            return this;
        }

        public Builder setDeveloperMessage(String developerMessage) {
            res.put(Key.DEVELOPERMESSAGE.text, developerMessage);
            return this;
        }

        public ErrorResponse build() {
            if(!res.containsKey(Key.ERRORCODE.text)) {
                setErrorCode("");
            }
            if(!res.containsKey(Key.ERRORDESCRIPTION.text)) {
                setErrorDescription("");
            }
            if (!res.containsKey(Key.DEVELOPERMESSAGE.text)) {
                setDeveloperMessage(res.get(Key.ERRORDESCRIPTION.text));
            }
            return res;
        }
    }

    /**
     * Creates an ErrorResponse object with 404 error code.
     * @return {@link ErrorResponse}
     */
    public static ErrorResponse forEmptyResult() {
        return new ErrorResponse.Builder()
            .setErrorCode("" + HttpServletResponse.SC_NOT_FOUND)
            .setErrorDescription(EMPTY_RESPONSE)
            .setDeveloperMessage(EMPTY_RESPONSE).build();
    }
}

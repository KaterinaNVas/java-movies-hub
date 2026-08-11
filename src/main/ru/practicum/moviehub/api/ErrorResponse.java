package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;
    private final String message;
    private final int statusCode;
    private final long timestamp;

    public ErrorResponse(String error, String message, int statusCode, long timestamp) {
        this.error = error;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
    }
    public ErrorResponse(String error, String message, int statusCode) {
        this.error = error;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = System.currentTimeMillis();
    }



    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String toJson() {
        return String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"statusCode\":%d,\"timestamp\":%d}",
                error, message, statusCode, timestamp
        );
    }

    @Override
    public String toString() {
        return toJson();
    }


}
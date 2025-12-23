class ValueHolder {
    String value;
    Long expiryTime; // Store the absolute millisecond when this should die

    ValueHolder(String value, Long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    boolean isExpired() {
        // If expiryTime is null, it lives forever. 
        // Otherwise, check if current time is past the death time.
        return expiryTime != null && System.currentTimeMillis() > expiryTime;
    }
}
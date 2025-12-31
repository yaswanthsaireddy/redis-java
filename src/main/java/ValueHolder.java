class ValueHolder {
    Object value; // Changed to Object to hold String OR List
    Long expiryTime;

    ValueHolder(Object value, Long expiryTime) { // Changed parameter to Object
        this.value = value;
        this.expiryTime = expiryTime;
    }

    boolean isExpired() {
        return expiryTime != null && System.currentTimeMillis() > expiryTime;
    }
}
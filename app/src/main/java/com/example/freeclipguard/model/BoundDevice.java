package com.example.freeclipguard.model;

/**
 * 使用Mac地址作为唯一标识，避免因设备重命名导致的识别问题
 * 
 * @author xiaowu
 */
public final class BoundDevice {

    private final String name;
    private final String address;

    public BoundDevice(String name, String address) {
        this.name = name == null ? "未命名设备" : name;
        this.address = address == null ? "" : address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public boolean isConfigured() {
        return !address.isBlank();
    }

    public boolean matchesAddress(String otherAddress) {
        return otherAddress != null && address.equalsIgnoreCase(otherAddress);
    }
}

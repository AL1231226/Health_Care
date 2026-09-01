package com.example.Elderly_care_Platfrom.utils;

public class UserContext {
    private static final ThreadLocal<UserInfo> holder = new ThreadLocal<>();

    public static void set(UserInfo user) {
        holder.set(user);
    }

    public static UserInfo get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }

    public record UserInfo(String userId,String role, String name) {}
}

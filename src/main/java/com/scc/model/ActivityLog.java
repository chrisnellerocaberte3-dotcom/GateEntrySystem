package com.scc.model;

public record ActivityLog(int logId, int userId, String action, String description, String timestamp) { }
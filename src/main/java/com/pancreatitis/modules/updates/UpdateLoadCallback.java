package com.pancreatitis.modules.updates;

public interface UpdateLoadCallback {
    void onStart(int totalCount);
    void onProgress(int loaded, int total);
    void onUpdateLoaded(UpdateLoadResult result);
    void onComplete(int successCount, int failCount);
    void onError(String error);
}
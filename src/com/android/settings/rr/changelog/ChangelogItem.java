package com.android.settings.rr.changelog;

class ChangelogItem {
    private String commit_id;
    private String commit_message;

    ChangelogItem(String id, String message) {
        commit_id = id;
        commit_message = message;
    }

    ChangelogItem(String id) {
        commit_id = id;
    }

    String getCommit_id() {
        return commit_id;
    }

    String getCommit_message() {
        return commit_message;
    }
}

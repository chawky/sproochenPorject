package com.nailic.sproochencoach.service;

record SupportEmailIngestionResult(
        boolean importCreated,
        boolean alreadyExisting,
        boolean ignoredEmail,
        int attachmentsAdded
) {
    static SupportEmailIngestionResult imported(int attachmentsAdded) {
        return new SupportEmailIngestionResult(true, false, false, attachmentsAdded);
    }

    static SupportEmailIngestionResult existing(int attachmentsAdded) {
        return new SupportEmailIngestionResult(false, true, false, attachmentsAdded);
    }

    static SupportEmailIngestionResult ignored() {
        return new SupportEmailIngestionResult(false, false, true, 0);
    }
}

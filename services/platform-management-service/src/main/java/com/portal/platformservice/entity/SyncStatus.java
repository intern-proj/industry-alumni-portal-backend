package com.portal.platformservice.entity;

/**
 * Tracks whether a local approval decision has been successfully propagated
 * to the owning service (User Service or Vacancy Service) via Feign.
 *
 * A decision is always committed locally first; the outbound callback is
 * fired after commit. If that callback fails, the record stays at
 * PENDING_CALLBACK and CallbackRetryScheduler replays it against an
 * idempotent receiving endpoint until it reaches SYNCED.
 */
public enum SyncStatus {
    SYNCED,
    PENDING_CALLBACK,
    FAILED
}

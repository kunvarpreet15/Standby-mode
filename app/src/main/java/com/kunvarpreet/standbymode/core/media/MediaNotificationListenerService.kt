package com.kunvarpreet.standbymode.core.media

import android.service.notification.NotificationListenerService

/**
 * Android NotificationListenerService required by MediaSessionManager to discover
 * and interact with third-party media sessions on the device.
 *
 * Privacy & Security:
 * StandBy Mode does NOT inspect, collect, or store notification bodies, messages,
 * or unrelated notification content. This service exists strictly to fulfill the
 * platform security requirement for MediaSessionManager.getActiveSessions(ComponentName).
 */
class MediaNotificationListenerService : NotificationListenerService()

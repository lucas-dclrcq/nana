package org.nana.webhook;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WebhookPayload(String event, WebhookDownload download) {
}
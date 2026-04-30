"""
Kafka consumer for real-time ad event ingestion.
Consumes from 'ad-events' topic and buffers events for feature aggregation.
"""

import json
import logging
import threading
from collections import defaultdict
from datetime import datetime
from typing import Dict, List, Optional

from confluent_kafka import Consumer, KafkaError, KafkaException

from app.config import settings

logger = logging.getLogger(__name__)


class EventBuffer:
    """Thread-safe buffer for accumulating ad events before aggregation."""

    def __init__(self):
        self._lock = threading.Lock()
        self._events: Dict[int, List[dict]] = defaultdict(list)
        self._ad_metadata: Dict[int, str] = {}  # adId -> createdBy

    def add_event(self, event: dict):
        ad_id = event.get("adId")
        event_type = event.get("type", "")
        if ad_id is None or event_type == "CREATION":
            # Store createdBy from CREATION events
            if event_type == "CREATION" and ad_id is not None:
                self._ad_metadata[ad_id] = event.get("createdBy", "UNKNOWN")
            return

        with self._lock:
            self._events[ad_id].append(event)
            if ad_id not in self._ad_metadata:
                self._ad_metadata[ad_id] = event.get("createdBy", "UNKNOWN")

    def get_ad_stats(self, ad_id: int) -> Optional[dict]:
        """Aggregate buffered events for a single ad into feature vector."""
        with self._lock:
            events = self._events.get(ad_id)
            if not events:
                return None

            n_views = sum(1 for e in events if e.get("type") == "VIEW")
            n_clicks = sum(1 for e in events if e.get("type") == "CLICK")
            n_hovers = sum(1 for e in events if e.get("type") == "HOVER")
            n_events = len(events)
            unique_users = len(set(e.get("userId", 0) for e in events if e.get("userId")))

            timestamps = []
            for e in events:
                ts = e.get("timestamp") or e.get("event_time")
                if ts:
                    try:
                        if isinstance(ts, str):
                            timestamps.append(datetime.fromisoformat(ts.replace("Z", "+00:00")))
                        elif isinstance(ts, list) and len(ts) >= 6:
                            timestamps.append(datetime(*ts[:6]))
                    except Exception:
                        pass

            lifespan_hours = 0.0
            if len(timestamps) >= 2:
                lifespan_hours = (max(timestamps) - min(timestamps)).total_seconds() / 3600

            ctr = n_clicks / n_views if n_views > 0 else 0.0
            hover_rate = n_hovers / n_views if n_views > 0 else 0.0
            click_per_event = n_clicks / n_events if n_events > 0 else 0.0

            return {
                "adId": ad_id,
                "createdBy": self._ad_metadata.get(ad_id, "UNKNOWN"),
                "n_views": n_views,
                "n_clicks": n_clicks,
                "n_hovers": n_hovers,
                "n_events": n_events,
                "n_unique_users": unique_users,
                "ctr": ctr,
                "hover_rate": hover_rate,
                "click_per_event": click_per_event,
                "lifespan_hours": lifespan_hours,
            }

    def get_all_ad_stats(self) -> List[dict]:
        """Aggregate all buffered ads."""
        with self._lock:
            ad_ids = list(self._events.keys())
        return [s for ad_id in ad_ids if (s := self.get_ad_stats(ad_id)) is not None]

    @property
    def n_ads(self) -> int:
        with self._lock:
            return len(self._events)

    @property
    def n_events_total(self) -> int:
        with self._lock:
            return sum(len(v) for v in self._events.values())


# Singleton buffer
event_buffer = EventBuffer()


class AdEventsConsumer:
    """Kafka consumer that runs in a background thread."""

    def __init__(self):
        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._consumer: Optional[Consumer] = None
        self._connected = False

    def start(self):
        """Start consuming in a background thread."""
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._consume_loop, daemon=True)
        self._thread.start()
        logger.info("[Kafka] Consumer thread started")

    def stop(self):
        """Signal the consumer to stop."""
        self._running = False
        if self._thread:
            self._thread.join(timeout=10)
        logger.info("[Kafka] Consumer thread stopped")

    def _consume_loop(self):
        """Main consumption loop."""
        conf = {
            "bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS,
            "group.id": settings.KAFKA_CONSUMER_GROUP,
            "auto.offset.reset": "earliest",
            "enable.auto.commit": True,
            "session.timeout.ms": 30000,
        }

        try:
            self._consumer = Consumer(conf)
            self._consumer.subscribe([settings.KAFKA_TOPIC_AD_EVENTS])
            self._connected = True
            logger.info(
                f"[Kafka] Subscribed to '{settings.KAFKA_TOPIC_AD_EVENTS}' "
                f"on {settings.KAFKA_BOOTSTRAP_SERVERS}"
            )
        except KafkaException as e:
            logger.error(f"[Kafka] Failed to connect: {e}")
            self._connected = False
            self._running = False
            return

        while self._running:
            try:
                msg = self._consumer.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    logger.error(f"[Kafka] Consumer error: {msg.error()}")
                    continue

                value = msg.value()
                if isinstance(value, bytes):
                    value = value.decode("utf-8")
                event = json.loads(value)
                event_buffer.add_event(event)

            except json.JSONDecodeError as e:
                logger.warning(f"[Kafka] Invalid JSON: {e}")
            except Exception as e:
                logger.error(f"[Kafka] Unexpected error: {e}")

        if self._consumer:
            self._consumer.close()
            self._connected = False

    @property
    def is_connected(self) -> bool:
        return self._connected


# Singleton consumer
kafka_consumer = AdEventsConsumer()

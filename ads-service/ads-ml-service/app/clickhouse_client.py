"""
ClickHouse client for reading ad events and aggregated features.
Mirrors the notebook's feature engineering pipeline.
"""

import logging
from typing import Optional

import clickhouse_connect
import numpy as np
import pandas as pd

from app.config import settings

logger = logging.getLogger(__name__)


def get_client():
    """Create a ClickHouse HTTP client."""
    return clickhouse_connect.get_client(
        host=settings.CLICKHOUSE_HOST,
        port=settings.CLICKHOUSE_PORT,
        database=settings.CLICKHOUSE_DB,
        username=settings.CLICKHOUSE_USER,
        password=settings.CLICKHOUSE_PASSWORD,
    )


def fetch_raw_events() -> pd.DataFrame:
    """Fetch all ad interaction events (VIEW, CLICK, HOVER) from ClickHouse."""
    client = get_client()
    query = """
        SELECT
            adId,
            type,
            createdBy,
            userId,
            event_time
        FROM ad_events
        WHERE type IN ('VIEW', 'CLICK', 'HOVER')
        ORDER BY event_time
    """
    result = client.query(query)
    df = pd.DataFrame(result.result_rows, columns=result.column_names)
    logger.info(f"[ClickHouse] Fetched {len(df)} raw interaction events")
    return df


def fetch_ad_features() -> pd.DataFrame:
    """
    Aggregate raw events into per-ad feature vectors.
    Mirrors the notebook's STEP 4 feature engineering exactly.
    """
    client = get_client()
    query = """
        SELECT
            adId,
            createdBy,
            countIf(type = 'VIEW')  AS n_views,
            countIf(type = 'CLICK') AS n_clicks,
            countIf(type = 'HOVER') AS n_hovers,
            count()                 AS n_events,
            uniq(userId)            AS n_unique_users,
            min(event_time)         AS first_interaction,
            max(event_time)         AS last_interaction
        FROM ad_events
        WHERE type IN ('VIEW', 'CLICK', 'HOVER')
        GROUP BY adId, createdBy
        HAVING n_views > 0
        ORDER BY adId
    """
    result = client.query(query)
    df = pd.DataFrame(result.result_rows, columns=result.column_names)

    if df.empty:
        logger.warning("[ClickHouse] No ad features found — table may be empty")
        return df

    # Derived metrics (same as notebook)
    df["ctr"] = df["n_clicks"] / df["n_views"].replace(0, np.nan)
    df["hover_rate"] = df["n_hovers"] / df["n_views"].replace(0, np.nan)
    df["click_per_event"] = df["n_clicks"] / df["n_events"].replace(0, np.nan)
    df["lifespan_hours"] = (
        (df["last_interaction"] - df["first_interaction"]).dt.total_seconds() / 3600
    )
    df[["ctr", "hover_rate", "click_per_event"]] = df[
        ["ctr", "hover_rate", "click_per_event"]
    ].fillna(0)

    logger.info(f"[ClickHouse] Aggregated features for {len(df)} ads")
    return df


def fetch_single_ad_features(ad_id: int) -> Optional[pd.DataFrame]:
    """Fetch aggregated features for a single ad."""
    client = get_client()
    query = f"""
        SELECT
            adId,
            createdBy,
            countIf(type = 'VIEW')  AS n_views,
            countIf(type = 'CLICK') AS n_clicks,
            countIf(type = 'HOVER') AS n_hovers,
            count()                 AS n_events,
            uniq(userId)            AS n_unique_users,
            min(event_time)         AS first_interaction,
            max(event_time)         AS last_interaction
        FROM ad_events
        WHERE type IN ('VIEW', 'CLICK', 'HOVER') AND adId = {ad_id}
        GROUP BY adId, createdBy
        HAVING n_views > 0
    """
    result = client.query(query)
    df = pd.DataFrame(result.result_rows, columns=result.column_names)

    if df.empty:
        return None

    df["ctr"] = df["n_clicks"] / df["n_views"].replace(0, np.nan)
    df["hover_rate"] = df["n_hovers"] / df["n_views"].replace(0, np.nan)
    df["click_per_event"] = df["n_clicks"] / df["n_events"].replace(0, np.nan)
    df["lifespan_hours"] = (
        (df["last_interaction"] - df["first_interaction"]).dt.total_seconds() / 3600
    )
    df[["ctr", "hover_rate", "click_per_event"]] = df[
        ["ctr", "hover_rate", "click_per_event"]
    ].fillna(0)

    return df

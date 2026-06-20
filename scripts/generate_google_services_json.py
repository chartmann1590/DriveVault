#!/usr/bin/env python3
"""Generate app/google-services.json from local.properties values.

Run this before a release build if the Google Services Gradle plugin is applied
and you do not want to commit google-services.json to Git.

Usage:
    python scripts/generate_google_services_json.py
"""

import json
import os
import re
from pathlib import Path


def load_local_properties(path: Path) -> dict:
    props = {}
    if not path.exists():
        raise FileNotFoundError(f"{path} not found. Copy local.properties.example to local.properties and fill it in.")
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            match = re.match(r"^([^=]+)=(.*)$", line)
            if match:
                key = match.group(1).strip()
                value = match.group(2).strip()
                # Unescape Android SDK path if needed
                value = value.replace("\\:", ":")
                props[key] = value
    return props


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    local_props = load_local_properties(root / "local.properties")

    required = [
        "FIREBASE_PROJECT_ID",
        "FIREBASE_PROJECT_NUMBER",
        "FIREBASE_STORAGE_BUCKET",
        "FIREBASE_DEBUG_APP_ID",
        "FIREBASE_DEBUG_API_KEY",
        "FIREBASE_RELEASE_APP_ID",
        "FIREBASE_RELEASE_API_KEY",
    ]
    missing = [k for k in required if not local_props.get(k)]
    if missing:
        raise SystemExit(f"Missing values in local.properties: {', '.join(missing)}")

    application_id = "com.drivevault.dashcam"

    data = {
        "project_info": {
            "project_number": local_props["FIREBASE_PROJECT_NUMBER"],
            "firebase_url": "",
            "project_id": local_props["FIREBASE_PROJECT_ID"],
            "storage_bucket": local_props["FIREBASE_STORAGE_BUCKET"],
        },
        "client": [
            {
                "client_info": {
                    "mobilesdk_app_id": local_props["FIREBASE_DEBUG_APP_ID"],
                    "android_client_info": {"package_name": f"{application_id}.debug"},
                },
                "api_key": [{"current_key": local_props["FIREBASE_DEBUG_API_KEY"]}],
            },
            {
                "client_info": {
                    "mobilesdk_app_id": local_props["FIREBASE_RELEASE_APP_ID"],
                    "android_client_info": {"package_name": application_id},
                },
                "api_key": [{"current_key": local_props["FIREBASE_RELEASE_API_KEY"]}],
            },
        ],
    }

    output = root / "app" / "google-services.json"
    with output.open("w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Generated {output}")


if __name__ == "__main__":
    main()

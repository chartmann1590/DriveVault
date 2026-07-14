#!/usr/bin/env python3
"""Push the DriveVault Play Store listing (text + graphics) via the Android Publisher API.

This only updates the *store listing* (title, descriptions, icon, feature graphic,
screenshots) for the en-US locale. It does NOT touch release tracks, does NOT upload
an app bundle, and does NOT submit anything for review — publishing a release still
requires the manual first-time Play Console declarations (content rating, data
safety, ads, target audience, app access) since those have no public API.

Requires:
    pip install google-api-python-client google-auth

Auth: reads the service account JSON from the GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env
var (as used by the publish-google-play.yml workflow), or from a file path passed
as the first argument.

Usage:
    python play-store/scripts/update_store_listing.py [path/to/service-account.json]
"""

import json
import os
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE_NAME = "com.drivevault.dashcam"
LOCALE = "en-US"
LISTING_DIR = Path(__file__).resolve().parent.parent / "listing"
GRAPHICS_DIR = Path(__file__).resolve().parent.parent / "graphics"
SCREENSHOTS_DIR = Path(__file__).resolve().parent.parent / "screenshots"

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip()


def load_credentials():
    if len(sys.argv) > 1:
        return service_account.Credentials.from_service_account_file(sys.argv[1], scopes=SCOPES)

    sa_json = os.environ.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON")
    if not sa_json:
        raise SystemExit(
            "No service account provided. Set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON or pass a file path."
        )
    info = json.loads(sa_json)
    return service_account.Credentials.from_service_account_info(info, scopes=SCOPES)


def upload_images(service, edit_id, image_type, paths):
    if not paths:
        return
    existing = service.edits().images().list(
        editId=edit_id, packageName=PACKAGE_NAME, language=LOCALE, imageType=image_type
    ).execute()
    for image in existing.get("images", []):
        service.edits().images().delete(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            language=LOCALE,
            imageType=image_type,
            imageId=image["id"],
        ).execute()

    for path in paths:
        print(f"  uploading {image_type}: {path.name}")
        service.edits().images().upload(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            language=LOCALE,
            imageType=image_type,
            media_body=str(path),
        ).execute()


def validate_lengths(title, short_description, full_description):
    limits = {
        "title": (title, 30),
        "short-description": (short_description, 80),
        "full-description": (full_description, 4000),
    }
    errors = [
        f"{name}.txt is {len(value)} chars, exceeds the {limit}-char Play Store limit"
        for name, (value, limit) in limits.items()
        if len(value) > limit
    ]
    if errors:
        raise SystemExit("Fix these before running:\n  " + "\n  ".join(errors))


def main():
    credentials = load_credentials()
    service = build("androidpublisher", "v3", credentials=credentials)

    title = read_text(LISTING_DIR / "title.txt")
    short_description = read_text(LISTING_DIR / "short-description.txt")
    full_description = read_text(LISTING_DIR / "full-description.txt")
    website_url = read_text(LISTING_DIR / "website-url.txt")
    support_email = read_text(LISTING_DIR / "support-email.txt")

    validate_lengths(title, short_description, full_description)

    print(f"Package: {PACKAGE_NAME}")
    print(f"Title: {title}")

    edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
    edit_id = edit["id"]
    print(f"Created edit {edit_id}")

    try:
        service.edits().details().update(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            body={
                "defaultLanguage": LOCALE,
                "contactEmail": support_email,
                "contactWebsite": website_url,
            },
        ).execute()
        print("Updated app details (contact email/website)")

        service.edits().listings().update(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            language=LOCALE,
            body={
                "language": LOCALE,
                "title": title,
                "shortDescription": short_description,
                "fullDescription": full_description,
            },
        ).execute()
        print(f"Updated {LOCALE} store listing text")

        upload_images(
            service, edit_id, "icon",
            [GRAPHICS_DIR / "app-icon" / "drivevault_app-icon.png"],
        )
        upload_images(
            service, edit_id, "featureGraphic",
            [GRAPHICS_DIR / "feature-graphic" / "drivevault_feature-graphic.png"],
        )
        upload_images(
            service, edit_id, "phoneScreenshots",
            sorted((SCREENSHOTS_DIR / "phone").glob("*.png")),
        )
        upload_images(
            service, edit_id, "sevenInchScreenshots",
            sorted((SCREENSHOTS_DIR / "tablet-7in").glob("*.png")),
        )
        upload_images(
            service, edit_id, "tenInchScreenshots",
            sorted((SCREENSHOTS_DIR / "tablet-10in").glob("*.png")),
        )

        service.edits().commit(editId=edit_id, packageName=PACKAGE_NAME).execute()
        print("Committed edit successfully. Store listing text and graphics are updated.")
        print(
            "NOTE: content rating, data safety, ads declaration, and target audience "
            "still require manual completion in Play Console before any release can "
            "be rolled out — the Android Publisher API has no endpoint for these."
        )
    except Exception:
        print("Error occurred, deleting draft edit to avoid leaving it dangling.")
        try:
            service.edits().delete(editId=edit_id, packageName=PACKAGE_NAME).execute()
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
